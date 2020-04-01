package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatService
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.PeriodeResultat
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.behandling.vilkår.VilkårResultat
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.task.dto.FerdigstillBehandlingDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import no.nav.nare.core.evaluations.Resultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres", "mock-dokgen")
@Tag("integration")
class FerdigstillBehandlingTest {

    @Autowired
    private lateinit var ferdigstillBehandling: FerdigstillBehandling

    @Autowired
    private lateinit var vedtakService: VedtakService

    @Autowired
    private lateinit var fagsakService: FagsakService

    @Autowired
    lateinit var behandlingService: BehandlingService

    @Autowired
    lateinit var behandlingResultatService: BehandlingResultatService

    @Autowired
    lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    private fun lagTestTask(resultat: Resultat): Task {
        val fnr = randomFnr()
        val fnrBarn = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        behandlingService.settBegrunnelseForVilkårsvurdering(behandling, "")

        val behandlingResultat = lagBehandlingResultat(fnr, behandling, resultat)

        behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat)
        Assertions.assertNotNull(behandling.fagsak.id)

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(fnrBarn))
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag,
                ansvarligSaksbehandler = "ansvarligSaksbehandler"
        )

        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.IVERKSATT)
        behandlingService.oppdaterStegPåBehandling(behandlingId = behandling.id, steg = StegType.FERDIGSTILLE_BEHANDLING)

        return FerdigstillBehandling.opprettTask(personIdent = fnr, behandlingsId = behandling.id)
    }

    @Test
    fun `Skal ferdigstille behandling og sette fagsak til løpende`() {
        val testTask = lagTestTask(Resultat.JA)
        val ferdigstillBehandlingDTO = objectMapper.readValue(testTask.payload, FerdigstillBehandlingDTO::class.java)

        ferdigstillBehandling.doTask(testTask)
        ferdigstillBehandling.onCompletion(testTask)

        val ferdigstiltBehandling = behandlingService.hent(behandlingId = ferdigstillBehandlingDTO.behandlingsId)
        Assertions.assertEquals(BehandlingStatus.FERDIGSTILT, ferdigstiltBehandling.status)

        val ferdigstiltFagsak = ferdigstiltBehandling.fagsak
        Assertions.assertEquals(FagsakStatus.LØPENDE, ferdigstiltFagsak.status)
    }

    @Test
    fun `Skal ferdigstille behandling og sette fagsak til stanset`() {
        val testTask = lagTestTask(Resultat.NEI)
        val ferdigstillBehandlingDTO = objectMapper.readValue(testTask.payload, FerdigstillBehandlingDTO::class.java)

        ferdigstillBehandling.doTask(testTask)
        ferdigstillBehandling.onCompletion(testTask)

        val ferdigstiltBehandling = behandlingService.hent(behandlingId = ferdigstillBehandlingDTO.behandlingsId)
        Assertions.assertEquals(BehandlingStatus.FERDIGSTILT, ferdigstiltBehandling.status)

        val ferdigstiltFagsak = ferdigstiltBehandling.fagsak
        Assertions.assertEquals(FagsakStatus.STANSET, ferdigstiltFagsak.status)
    }
}