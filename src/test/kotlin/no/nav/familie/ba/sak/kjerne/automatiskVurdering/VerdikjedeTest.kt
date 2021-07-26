package no.nav.familie.ba.sak.kjerne.automatiskVurdering

import io.mockk.every
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.config.ClientMocks.Companion.initEuKodeverk
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.config.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.infotrygd.domene.InfotrygdFødselhendelsesFeedTaskDto
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.VergeResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.DødsfallData
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.FiltreringsreglerResultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.dokument.BrevService
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.Vedtaksbrevtype
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.task.BehandleFødselshendelseTask
import no.nav.familie.ba.sak.task.dto.OpprettOppgaveTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.prosessering.domene.TaskRepository
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate

@SpringBootTest(properties = ["FAMILIE_FAMILIE_TILBAKE_API_URL=http://localhost:28085/api"])
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles(
        "dev",
        "postgres",
        "mock-pdl",
        "mock-familie-tilbake",
        "mock-infotrygd-feed",
        "mock-infotrygd-barnetrygd",
        "mock-brev-klient"
)
@Tag("integration")
class VerdikjedeTest(
        @Autowired val stegService: StegService,
        @Autowired val personopplysningerService: PersonopplysningerService,
        @Autowired val persongrunnlagService: PersongrunnlagService,
        @Autowired val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        @Autowired val fagSakService: FagsakService,
        @Autowired val taskRepository: TaskRepository,
        @Autowired val behandleFødselshendelseTask: BehandleFødselshendelseTask,
        @Autowired val behandlingService: BehandlingService,
        @Autowired val databaseCleanupService: DatabaseCleanupService,
        @Autowired val featureToggleService: FeatureToggleService,
        @Autowired val integrasjonClient: IntegrasjonClient,
        @Autowired val vedtakService: VedtakService,
        @Autowired val brevService: BrevService,
        @Autowired val vedtaksperiodeService: VedtaksperiodeService,
) {

    val morsIdent = "04086226621"
    val barnasIdenter = listOf("21111777001")

    val clientMocks = ClientMocks()

    @BeforeEach
    fun init() {
        taskRepository.deleteAll()
        initEuKodeverk(integrasjonClient)
        mockPersonopplysning(morsIdent, mockSøkerAutomatiskBehandling, personopplysningerService)
        mockIntegrasjonsClient(morsIdent, integrasjonClient)

        every { featureToggleService.isEnabled(FeatureToggleConfig.AUTOMATISK_FØDSELSHENDELSE) } returns true
        every { featureToggleService.isEnabled(FeatureToggleConfig.BRUK_VEDTAKSTYPE_MED_BEGRUNNELSER) } returns true

    }

    @AfterEach
    fun tearDown() {
        databaseCleanupService.truncate();
    }


    @Test
    fun `Søker uten løpende fagsak i BA blir sendt til infotrygd`() {
        mockPersonopplysning(barnasIdenter.first(), mockBarnAutomatiskBehandling, personopplysningerService)
        lagOgkjørfødselshendelseTask(morsIdent, barnasIdenter, behandleFødselshendelseTask)
        val fagsak = fagSakService.hent(PersonIdent(morsIdent))
        assertEquals(null, fagsak?.status)

        val taskFraInfotrygd = taskRepository.findAll()[0]
        val InfotrygdFødselhendelsesFeedTaskDto =
                objectMapper.readValue(taskFraInfotrygd.payload, InfotrygdFødselhendelsesFeedTaskDto::class.java)
        assertEquals(barnasIdenter, InfotrygdFødselhendelsesFeedTaskDto.fnrBarn)
    }

    @Test
    fun `Søker med løpende fagsak og betaling i BA blir sendt til manuell behandling`() {
        val barneIdentForFørsteHendelse = "20010777101"
        val barneForFørsteHendelse = mockBarnAutomatiskBehandling.copy(fødselsdato = LocalDate.now().minusYears(2))
        val tobarnsmorsIdent = "04086226688"
        val tobarnsmor = mockSøkerMedToBarnAutomatiskBehandling
        mockPersonopplysning(barnasIdenter.first(), mockBarnAutomatiskBehandling, personopplysningerService)
        mockPersonopplysning(barneIdentForFørsteHendelse, barneForFørsteHendelse, personopplysningerService)
        mockPersonopplysning(tobarnsmorsIdent, tobarnsmor, personopplysningerService)
        every { personopplysningerService.harVerge(tobarnsmorsIdent) } returns VergeResponse(false)

        val fagsak = løpendeFagsakForÅUnngåInfotrygd(tobarnsmorsIdent, fagSakService)

        lagOgkjørfødselshendelseTask(tobarnsmorsIdent, listOf(barneIdentForFørsteHendelse), behandleFødselshendelseTask)
        val behanding = behandlingService.hentBehandlinger(fagsak.id).first()
        behandlingOgFagsakErÅpen(behanding, fagsak)

        //begynner neste behandling
        lagOgkjørfødselshendelseTask(tobarnsmorsIdent, barnasIdenter, behandleFødselshendelseTask)

        val data = hentDataForNyTask(taskRepository, 1);

        assertEquals(behanding.id, data.behandlingId)
        assertEquals("Fødselshendelse: Bruker har åpen behandling", data.beskrivelse)
    }

    @Test
    fun `søker med løpende fagsak og verge blir sendt til manuell behandling `() {
        mockPersonopplysning(barnasIdenter.first(), mockBarnAutomatiskBehandling, personopplysningerService)
        every { personopplysningerService.harVerge(morsIdent) } returns VergeResponse(true)

        val fagsak = løpendeFagsakForÅUnngåInfotrygd(morsIdent, fagSakService)

        lagOgkjørfødselshendelseTask(morsIdent, barnasIdenter, behandleFødselshendelseTask)

        val behanding = behandlingService.hentBehandlinger(fagsak.id).first()
        behandlingOgFagsakErÅpen(behanding, fagsak)

        val data = hentDataForNyTask(taskRepository);
        assertEquals(behanding.id, data.behandlingId)
        assertEquals(FiltreringsreglerResultat.MOR_HAR_VERGE.beskrivelse, data.beskrivelse)
    }

    @Test
    fun `søker med ugyldig Fnr blir sendt til manuell behandling`() {
        mockPersonopplysning(barnasIdenter.first(), mockBarnAutomatiskBehandling, personopplysningerService)
        val morsUgyldigeFnr = "04886226622"
        mockIntegrasjonsClient(morsUgyldigeFnr, integrasjonClient)
        mockPersonopplysning(morsUgyldigeFnr, mockSøkerAutomatiskBehandling, personopplysningerService)
        every { personopplysningerService.harVerge(morsUgyldigeFnr) } returns VergeResponse(false)

        val fagsak = løpendeFagsakForÅUnngåInfotrygd(morsUgyldigeFnr, fagSakService)

        lagOgkjørfødselshendelseTask(morsUgyldigeFnr, barnasIdenter, behandleFødselshendelseTask)

        val behanding = behandlingService.hentBehandlinger(fagsak.id).first()
        behandlingOgFagsakErÅpen(behanding, fagsak)

        val data = hentDataForNyTask(taskRepository);
        assertEquals(behanding.id, data.behandlingId)
        assertEquals(FiltreringsreglerResultat.MOR_IKKE_GYLDIG_FNR.beskrivelse, data.beskrivelse)
    }

    @Test
    fun `død søker blir sendt til manuell behandling`() {
        mockPersonopplysning(barnasIdenter.first(), mockBarnAutomatiskBehandling, personopplysningerService)
        every { personopplysningerService.harVerge(morsIdent) } returns VergeResponse(false)
        every { personopplysningerService.hentDødsfall(Ident(morsIdent)) } returns DødsfallData(true, null)

        val fagsak = løpendeFagsakForÅUnngåInfotrygd(morsIdent, fagSakService)

        lagOgkjørfødselshendelseTask(morsIdent, barnasIdenter, behandleFødselshendelseTask)

        val behanding = behandlingService.hentBehandlinger(fagsak.id).first()
        behandlingOgFagsakErÅpen(behanding, fagsak)

        val data = hentDataForNyTask(taskRepository);
        assertEquals(behanding.id, data.behandlingId)
        assertEquals(FiltreringsreglerResultat.MOR_ER_DØD.beskrivelse, data.beskrivelse)
    }

    @Test
    fun `søker med dødt barn blir sendt til manuell behandling`() {
        mockPersonopplysning(barnasIdenter.first(), mockBarnAutomatiskBehandling, personopplysningerService)
        every { personopplysningerService.harVerge(morsIdent) } returns VergeResponse(false)
        every { personopplysningerService.hentDødsfall(Ident(barnasIdenter.first())) } returns DødsfallData(true, null)

        val fagsak = løpendeFagsakForÅUnngåInfotrygd(morsIdent, fagSakService)

        lagOgkjørfødselshendelseTask(morsIdent, barnasIdenter, behandleFødselshendelseTask)

        val behanding = behandlingService.hentBehandlinger(fagsak.id).first()
        behandlingOgFagsakErÅpen(behanding, fagsak)

        val data = hentDataForNyTask(taskRepository);
        assertEquals(behanding.id, data.behandlingId)
        assertEquals(FiltreringsreglerResultat.DØDT_BARN.beskrivelse, data.beskrivelse)
    }

    @Test
    fun `søker under 18 blir sendt til manuell behandling`() {
        val morUnder18 = mockSøkerAutomatiskBehandling.copy(fødselsdato = LocalDate.now().minusYears(17))
        mockPersonopplysning(morsIdent, morUnder18, personopplysningerService)
        mockPersonopplysning(barnasIdenter.first(), mockBarnAutomatiskBehandling, personopplysningerService)
        every { personopplysningerService.harVerge(morsIdent) } returns VergeResponse(false)

        val fagsak = løpendeFagsakForÅUnngåInfotrygd(morsIdent, fagSakService)

        lagOgkjørfødselshendelseTask(morsIdent, barnasIdenter, behandleFødselshendelseTask)

        val behanding = behandlingService.hentBehandlinger(fagsak.id).first()
        behandlingOgFagsakErÅpen(behanding, fagsak)

        val data = hentDataForNyTask(taskRepository);
        assertEquals(behanding.id, data.behandlingId)
        assertEquals(FiltreringsreglerResultat.MOR_ER_IKKE_OVER_18.beskrivelse, data.beskrivelse)
    }

    @Test
    fun `Barn bor ikke i Norge, består filtrering, stopper i vilkår, opprettet manuell oppgave`() {
        val barn = mockBarnAutomatiskBehandling.copy(bostedsadresser = emptyList())
        mockPersonopplysning(barnasIdenter.first(), barn, personopplysningerService)
        every { personopplysningerService.harVerge(morsIdent) } returns VergeResponse(false)

        val fagsak = løpendeFagsakForÅUnngåInfotrygd(morsIdent, fagSakService)
        lagOgkjørfødselshendelseTask(morsIdent, barnasIdenter, behandleFødselshendelseTask)

        val behanding = behandlingService.hentBehandlinger(fagsak.id).first()
        assertEquals(BehandlingResultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE, behanding.resultat)
    }

    @Test
    fun `Barn er gift, stopper i vilkår`() {
        val barn = mockBarnAutomatiskBehandling.copy(sivilstander = listOf(Sivilstand(SIVILSTAND.GIFT)))
        mockPersonopplysning(barnasIdenter.first(), barn, personopplysningerService)
        every { personopplysningerService.harVerge(morsIdent) } returns VergeResponse(false)

        val fagsak = løpendeFagsakForÅUnngåInfotrygd(morsIdent, fagSakService)
        lagOgkjørfødselshendelseTask(morsIdent, barnasIdenter, behandleFødselshendelseTask)

        val behanding = behandlingService.hentBehandlinger(fagsak.id).first()
        assertEquals(BehandlingResultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE, behanding.resultat)
    }

    @Test
    fun `Barn bor ikke med søker`() {
        val barn = mockBarnAutomatiskBehandling.copy(
                bostedsadresser = alternaltivAdresse
        )
        mockPersonopplysning(barnasIdenter.first(), barn, personopplysningerService)
        every { personopplysningerService.harVerge(morsIdent) } returns VergeResponse(false)

        val fagsak = løpendeFagsakForÅUnngåInfotrygd(morsIdent, fagSakService)
        lagOgkjørfødselshendelseTask(morsIdent, barnasIdenter, behandleFødselshendelseTask)

        val behanding = behandlingService.hentBehandlinger(fagsak.id).first()
        assertEquals(BehandlingResultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE, behanding.resultat)
        val taskForOpprettelseAvManuellBehandling = taskRepository.findAll().first()
        val opprettOppgaveTaskDTO =
            objectMapper.readValue(taskForOpprettelseAvManuellBehandling.payload, OpprettOppgaveTaskDTO::class.java)
        assertEquals(behanding.id, opprettOppgaveTaskDTO.behandlingId)
        assertEquals("Fødselshendelse: Barnet ikke bosatt med mor\n", opprettOppgaveTaskDTO.beskrivelse)
    }

    @Test
    fun `Setter riktig begrunnelse i automatisk løp når mor har barn fra før`() {
        val mor = mockSøkerAutomatiskBehandling
        mockPersonopplysning(morsIdent, mor, personopplysningerService)
        mockPersonopplysning(barnasIdenter.first(), mockBarnAutomatiskBehandling, personopplysningerService)
        every { personopplysningerService.harVerge(morsIdent) } returns VergeResponse(false)
        val fagsak = løpendeFagsakForÅUnngåInfotrygd(morsIdent, fagSakService)

        lagOgkjørfødselshendelseTask(morsIdent, barnasIdenter, behandleFødselshendelseTask)
        val behanding = behandlingService.hentBehandlinger(fagsak.id).first()
        val vedtak = vedtakService.hentAktivForBehandling(behanding.id)

        val vedtaksbrev = brevService.hentVedtaksbrevData(vedtak!!)

        assertEquals(Vedtaksbrevtype.AUTOVEDTAK_NYFØDT_BARN_FRA_FØR, vedtaksbrev.type)
    }

    @Test
    fun `Setter riktig begrunnelse i automatisk løp når mor er førstegangsfødende`() {
        val mor = mockSøkerAutomatiskBehandling
        mockPersonopplysning(morsIdent, mor, personopplysningerService)
        mockPersonopplysning(barnasIdenter.first(), mockBarnAutomatiskBehandling, personopplysningerService)
        every { personopplysningerService.harVerge(morsIdent) } returns VergeResponse(false)


        val fagsak = fagSakService.hentEllerOpprettFagsak(PersonIdent(morsIdent), true)
        fagSakService.oppdaterStatus(fagsak, FagsakStatus.AVSLUTTET)

        lagOgkjørfødselshendelseTask(morsIdent, barnasIdenter, behandleFødselshendelseTask)
        val behanding = behandlingService.hentBehandlinger(fagsak.id).first()
        val vedtak = vedtakService.hentAktivForBehandling(behanding.id)

        val vedtaksbrev = brevService.hentVedtaksbrevData(vedtak!!)

        assertEquals(Vedtaksbrevtype.AUTOVEDTAK_NYFØDT_FØRSTE_BARN, vedtaksbrev.type)
    }
}
