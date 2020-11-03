package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatService
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.nare.Resultat
import no.nav.familie.ba.sak.task.dto.FerdigstillBehandlingDTO
import no.nav.familie.ba.sak.vedtak.producer.MockKafkaProducer.Companion.meldingSendtFor
import no.nav.familie.ba.sak.økonomi.ØkonomiService
import no.nav.familie.eksterne.kontrakter.saksstatistikk.BehandlingDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.SakDVH
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres", "mock-dokgen", "mock-pdl", "mock-økonomi", "mock-infotrygd-feed", "mock-arbeidsfordeling")
@Tag("integration")
class FerdigstillBehandlingTaskTest {

    @Autowired
    private lateinit var ferdigstillBehandlingTask: FerdigstillBehandlingTask

    @Autowired
    private lateinit var vedtakService: VedtakService

    @Autowired
    private lateinit var fagsakService: FagsakService

    @Autowired
    lateinit var behandlingService: BehandlingService

    @Autowired
    lateinit var stegService: StegService

    @Autowired
    lateinit var behandlingResultatService: BehandlingResultatService

    @Autowired
    lateinit var økonomiService: ØkonomiService

    @Autowired
    lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    private var vedtak: Vedtak? = null

    private fun lagTestTask(resultat: Resultat): Task {
        val fnr = randomFnr()
        val fnrBarn = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
                lagBehandling(fagsak, årsak = BehandlingÅrsak.FØDSELSHENDELSE, automatiskOpprettelse = true))

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(fnrBarn))
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        val behandlingResultat = lagBehandlingResultat(fnr, behandling, resultat)

        behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat = behandlingResultat)
        val behandlingSomSkalKjøreVilkårsvurdering =
                behandlingService.oppdaterStegPåBehandling(behandling.id, StegType.VILKÅRSVURDERING)
        stegService.håndterVilkårsvurdering(behandlingSomSkalKjøreVilkårsvurdering)

        vedtak = vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag
        )

        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.IVERKSETTER_VEDTAK)
        behandlingService.oppdaterStegPåBehandling(behandlingId = behandling.id, steg = StegType.FERDIGSTILLE_BEHANDLING)

        return FerdigstillBehandlingTask.opprettTask(personIdent = fnr, behandlingsId = behandling.id)
    }

    @Test
    fun `Skal ferdigstille behandling og fagsak blir til løpende`() {
        val testTask = lagTestTask(Resultat.JA)
        iverksettMotOppdrag(vedtak = vedtak!!)

        val ferdigstillBehandlingDTO = objectMapper.readValue(testTask.payload, FerdigstillBehandlingDTO::class.java)

        ferdigstillBehandlingTask.doTask(testTask)
        ferdigstillBehandlingTask.onCompletion(testTask)

        val ferdigstiltBehandling = behandlingService.hent(behandlingId = ferdigstillBehandlingDTO.behandlingsId)
        assertEquals(BehandlingStatus.AVSLUTTET, ferdigstiltBehandling.status)
        //assertEquals(BehandlingStatus.AVSLUTTET.name, (meldingSendtFor(ferdigstiltBehandling) as BehandlingDVH).behandlingStatus) TODO: Stig fikser

        val ferdigstiltFagsak = ferdigstiltBehandling.fagsak
        assertEquals(FagsakStatus.LØPENDE, ferdigstiltFagsak.status)
        //assertEquals(FagsakStatus.LØPENDE.name, (meldingSendtFor(ferdigstiltFagsak) as SakDVH).sakStatus) TODO: Stig fikser
    }

    @Test
    fun `Skal ferdigstille behandling og sette fagsak til stanset`() {
        val testTask = lagTestTask(Resultat.NEI)

        val ferdigstillBehandlingDTO = objectMapper.readValue(testTask.payload, FerdigstillBehandlingDTO::class.java)

        ferdigstillBehandlingTask.doTask(testTask)
        ferdigstillBehandlingTask.onCompletion(testTask)

        val ferdigstiltBehandling = behandlingService.hent(behandlingId = ferdigstillBehandlingDTO.behandlingsId)
        assertEquals(BehandlingStatus.AVSLUTTET, ferdigstiltBehandling.status)

        val ferdigstiltFagsak = ferdigstiltBehandling.fagsak
        assertEquals(FagsakStatus.AVSLUTTET, ferdigstiltFagsak.status)
        //assertEquals(FagsakStatus.AVSLUTTET.name, (meldingSendtFor(ferdigstiltFagsak) as SakDVH).sakStatus) TODO: Stig fikser
    }

    private fun iverksettMotOppdrag(vedtak: Vedtak) {
        økonomiService.oppdaterTilkjentYtelseOgIverksettVedtak(vedtak, "ansvarligSaksbehandler")
    }
}