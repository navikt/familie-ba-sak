package no.nav.familie.ba.sak.task

import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.domene.vedtak.NyttVedtak
import no.nav.familie.ba.sak.behandling.domene.vedtak.VedtakResultat
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonTjeneste
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.task.dto.FerdigstillBehandlingDTO
import no.nav.familie.ba.sak.util.DbContainerInitializer
import no.nav.familie.ba.sak.util.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.util.randomFnr
import no.nav.familie.ba.sak.vilkår.vilkårsvurderingKomplettForBarnOgSøker
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres", "mock-dokgen")
@Tag("integration")
class FerdigstillBehandlingTest {

    @Autowired
    private lateinit var ferdigstillBehandling: FerdigstillBehandling

    @Autowired
    private lateinit var taskRepositoryMock: TaskRepository

    @MockBean
    private lateinit var integrasjonTjeneste: IntegrasjonTjeneste

    @Autowired
    lateinit var behandlingService: BehandlingService

    @Autowired
    lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @BeforeEach
    fun setUp() {
        taskRepositoryMock = mockk()
        Mockito.`when`(integrasjonTjeneste.hentAktørId(ArgumentMatchers.anyString())).thenReturn(AktørId("1"))
    }

    fun lagTestTask(vedtakResultat: VedtakResultat): Task {
        val fnr = randomFnr()
        val fnrBarn = randomFnr()

        val fagsak = behandlingService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.opprettNyBehandlingPåFagsak(fagsak,
                                                                       "sdf",
                                                                       BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                       BehandlingKategori.NASJONAL,
                                                                       BehandlingUnderkategori.ORDINÆR)
        Assertions.assertNotNull(behandling.fagsak.id)

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, fnr, fnrBarn)
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        behandlingService.nyttVedtakForAktivBehandling(
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag,
                nyttVedtak = NyttVedtak(resultat = vedtakResultat,
                                        samletVilkårResultat = vilkårsvurderingKomplettForBarnOgSøker(fnr,
                                                                                                      listOf(fnrBarn)),
                                        begrunnelse = ""),
                ansvarligSaksbehandler = "ansvarligSaksbehandler"
        )

        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.IVERKSATT)

        return FerdigstillBehandling.opprettTask(personIdent = fnr, behandlingsId = behandling.id)
    }

    @Test
    fun `Skal ferdigstille behandling og sette fagsak til løpende`() {
        val testTask = lagTestTask(VedtakResultat.INNVILGET)
        val ferdigstillBehandlingDTO = objectMapper.readValue(testTask.payload, FerdigstillBehandlingDTO::class.java)

        ferdigstillBehandling.doTask(testTask)
        ferdigstillBehandling.onCompletion(testTask)

        val ferdigstiltBehandling = behandlingService.hentBehandling(behandlingId = ferdigstillBehandlingDTO.behandlingsId)
        Assertions.assertEquals(BehandlingStatus.FERDIGSTILT, ferdigstiltBehandling?.status)

        val ferdigstiltFagsak = ferdigstiltBehandling?.fagsak
        Assertions.assertEquals(FagsakStatus.LØPENDE, ferdigstiltFagsak?.status)
    }

    @Test
    fun `Skal ferdigstille behandling og sette fagsak til stanset`() {
        val testTask = lagTestTask(VedtakResultat.AVSLÅTT)
        val ferdigstillBehandlingDTO = objectMapper.readValue(testTask.payload, FerdigstillBehandlingDTO::class.java)

        ferdigstillBehandling.doTask(testTask)
        ferdigstillBehandling.onCompletion(testTask)

        val ferdigstiltBehandling = behandlingService.hentBehandling(behandlingId = ferdigstillBehandlingDTO.behandlingsId)
        Assertions.assertEquals(BehandlingStatus.FERDIGSTILT, ferdigstiltBehandling?.status)

        val ferdigstiltFagsak = ferdigstiltBehandling?.fagsak
        Assertions.assertEquals(FagsakStatus.STANSET, ferdigstiltFagsak?.status)
    }
}