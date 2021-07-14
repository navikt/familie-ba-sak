package no.nav.familie.ba.sak.kjerne.automatiskVurdering

import io.mockk.clearAllMocks
import io.mockk.every
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.config.ClientMocks.Companion.initEuKodeverk
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.config.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.integrasjoner.infotrygd.domene.InfotrygdFødselhendelsesFeedTaskDto
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.VergeResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.DødsfallData
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.IdentInformasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingIntegrationTest
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.FiltreringsreglerResultat
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.VelgFagSystemService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.GrBostedsadresseperiode
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.task.BehandleFødselshendelseTask
import no.nav.familie.ba.sak.task.dto.BehandleFødselshendelseTaskDTO
import no.nav.familie.ba.sak.task.dto.OpprettOppgaveTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import no.nav.familie.prosessering.domene.TaskRepository
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
)
@Tag("integration")
class VerdikjedeTest(
        @Autowired val stegService: StegService,
        @Autowired val personopplysningerService: PersonopplysningerService,
        @Autowired val persongrunnlagService: PersongrunnlagService,
        @Autowired val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        @Autowired val pdlRestClient: PdlRestClient,
        @Autowired val velgFagSystemService: VelgFagSystemService,
        @Autowired val fagSakService: FagsakService,
        @Autowired val taskRepository: TaskRepository,
        @Autowired val behandleFødselshendelseTask: BehandleFødselshendelseTask,
        @Autowired val behandlingService: BehandlingService,
        @Autowired val databaseCleanupService: DatabaseCleanupService,
        @Autowired val featureToggleService: FeatureToggleService,
        @Autowired val integrasjonClient: IntegrasjonClient,
        @Autowired val vedtakService: VedtakService,
        @Autowired val vedtakRepository: VedtakRepository
) {

    val morsIdent = "04086226621"
    val barnasIdenter = listOf("21111777001")
    val nyfødteBarnsIdenter = listOf("21001777001")

    val clientMocks = ClientMocks()

    @BeforeEach
    fun init() {
        databaseCleanupService.truncate()
        clearAllMocks()
        taskRepository.deleteAll()
        initEuKodeverk(integrasjonClient)
        mockPersonopplysning(morsIdent, mockSøkerAutomatiskBehandling)
        mockPersonopplysning(barnasIdenter.first(), mockBarnAutomatiskBehandling)

        every { featureToggleService.isEnabled(FeatureToggleConfig.AUTOMATISK_FØDSELSHENDELSE) } returns true
        every { integrasjonClient.hentBehandlendeEnhet(morsIdent) } returns listOf(
                Arbeidsfordelingsenhet(enhetId = ArbeidsfordelingIntegrationTest.IKKE_FORTROLIG_ENHET,
                                       enhetNavn = "vanlig enhet"))
        every { integrasjonClient.hentLand(any()) } returns "NOK"

    }

    @Test
    fun `Søker uten løpende fagsak i BA blir sendt til infotrygd`() {
        val nyBehandlingHendelse = NyBehandlingHendelse(morsIdent, barnasIdenter)
        val task = BehandleFødselshendelseTask.opprettTask(BehandleFødselshendelseTaskDTO(nyBehandlingHendelse))
        behandleFødselshendelseTask.doTask(task)
        val fagsak = fagSakService.hent(PersonIdent(morsIdent))
        assertEquals(null, fagsak?.status)

        val taskFraInfotrygd = taskRepository.findAll()[0]
        val InfotrygdFødselhendelsesFeedTaskDto =
                objectMapper.readValue(taskFraInfotrygd.payload, InfotrygdFødselhendelsesFeedTaskDto::class.java)
        assertEquals(barnasIdenter, InfotrygdFødselhendelsesFeedTaskDto.fnrBarn)
    }

    @Test
    fun `Søker med løpende fagsak og betaling i BA blir sendt til manuell behandling`() {
        every { personopplysningerService.harVerge(morsIdent) } returns VergeResponse(false)

        val åpenFagsak = fagSakService.hentEllerOpprettFagsak(PersonIdent(morsIdent), true)
        fagSakService.oppdaterStatus(åpenFagsak, FagsakStatus.LØPENDE)
        assertEquals(FagsakStatus.LØPENDE, åpenFagsak?.status)

        val nyBehandlingHendelse = NyBehandlingHendelse(morsIdent, barnasIdenter)
        val taskForÅpenBehandling =
                BehandleFødselshendelseTask.opprettTask(BehandleFødselshendelseTaskDTO(nyBehandlingHendelse))

        behandleFødselshendelseTask.doTask(taskForÅpenBehandling)
        val fagsak = fagSakService.hent(PersonIdent(morsIdent))
        assertEquals(åpenFagsak.id, fagsak?.id)
        assertEquals(FagsakStatus.LØPENDE, fagsak?.status)

        val behanding = behandlingService.hentBehandlinger(fagsak!!.id).first()
        assertEquals(BehandlingStatus.UTREDES, behanding.status)
        assertEquals(BehandlingÅrsak.FØDSELSHENDELSE, behanding.opprettetÅrsak)
        assertEquals(StegType.VILKÅRSVURDERING, behanding.steg)

        //begynner neste behandling
        val taskForNyBehandling =
                BehandleFødselshendelseTask.opprettTask(BehandleFødselshendelseTaskDTO(nyBehandlingHendelse))

        behandleFødselshendelseTask.doTask(taskForNyBehandling)
        val fagsakEtterToBehandlinger = fagSakService.hent(PersonIdent(morsIdent))

        val tasker = taskRepository.findAll()
        val taskForOpprettelseAvManuellBehandling = tasker[1]
        val opprettOppgaveTaskDTO =
                objectMapper.readValue(taskForOpprettelseAvManuellBehandling.payload, OpprettOppgaveTaskDTO::class.java)
        assertEquals(behanding.id, opprettOppgaveTaskDTO.behandlingId)
        assertEquals("Fødselshendelse: Bruker har åpen behandling", opprettOppgaveTaskDTO.beskrivelse)
    }

    @Test
    fun `søker med løpende fagsak og verge blir sendt til manuell behandling `() {
        val nyfødtBarn = mockBarnAutomatiskBehandling.copy(fødselsdato = LocalDate.now())
        mockPersonopplysning(nyfødteBarnsIdenter.first(), nyfødtBarn)
        every { personopplysningerService.harVerge(morsIdent) } returns VergeResponse(true)

        val åpenFagsak = fagSakService.hentEllerOpprettFagsak(PersonIdent(morsIdent), true)
        fagSakService.oppdaterStatus(åpenFagsak, FagsakStatus.LØPENDE)

        val nyBehandlingHendelse = NyBehandlingHendelse(morsIdent, nyfødteBarnsIdenter)
        val task = BehandleFødselshendelseTask.opprettTask(BehandleFødselshendelseTaskDTO(nyBehandlingHendelse))

        behandleFødselshendelseTask.doTask(task)
        val fagsak = fagSakService.hent(PersonIdent(morsIdent))
        val behanding = behandlingService.hentBehandlinger(fagsak!!.id).first()
        assertEquals(BehandlingStatus.UTREDES, behanding.status)
        assertEquals(BehandlingÅrsak.FØDSELSHENDELSE, behanding.opprettetÅrsak)
        assertEquals(StegType.VILKÅRSVURDERING, behanding.steg)
        assertEquals(åpenFagsak.id, fagsak.id)
        assertEquals(FagsakStatus.LØPENDE, fagsak?.status)

        val taskForOpprettelseAvManuellBehandling = taskRepository.findAll().first()
        val opprettOppgaveTaskDTO =
                objectMapper.readValue(taskForOpprettelseAvManuellBehandling.payload, OpprettOppgaveTaskDTO::class.java)
        assertEquals(behanding.id, opprettOppgaveTaskDTO.behandlingId)
        assertEquals(FiltreringsreglerResultat.MOR_HAR_VERGE.beskrivelse, opprettOppgaveTaskDTO.beskrivelse)
    }

    @Test
    @Disabled
    fun `søker med _ går gjennom filtrering videre til vedtak`() {
        val nyfødtBarn = mockBarnAutomatiskBehandling.copy(fødselsdato = LocalDate.now())
        mockPersonopplysning(nyfødteBarnsIdenter.first(), nyfødtBarn)
        every { personopplysningerService.harVerge(morsIdent) } returns VergeResponse(false)

        val åpenFagsak = fagSakService.hentEllerOpprettFagsak(PersonIdent(morsIdent), true)
        fagSakService.oppdaterStatus(åpenFagsak, FagsakStatus.LØPENDE)

        val nyBehandlingHendelse = NyBehandlingHendelse(morsIdent, nyfødteBarnsIdenter)
        val task = BehandleFødselshendelseTask.opprettTask(BehandleFødselshendelseTaskDTO(nyBehandlingHendelse))
        assertThrows<FunksjonellFeil> {
            behandleFødselshendelseTask.doTask(task)
        }
    }

    fun mockPersonopplysning(personnr: String, personInfo: PersonInfo) {
        every { personopplysningerService.hentPersoninfo(personnr) } returns personInfo
        every { personopplysningerService.hentIdenter(Ident(personnr)) } returns listOf(IdentInformasjon(personnr,
                                                                                                         false,
                                                                                                         gruppe = ""))
        every { personopplysningerService.hentPersoninfoMedRelasjoner(personnr) } returns personInfo
        every { personopplysningerService.hentAktivAktørId(Ident(personnr)) } returns AktørId(personnr)
        every { personopplysningerService.hentStatsborgerskap(Ident(personnr)) } returns
                listOf(Statsborgerskap("NOK",
                                       personInfo.fødselsdato,
                                       LocalDate.now()
                                               .plusYears(
                                                       30)))
        every {
            personopplysningerService.hentBostedsadresseperioder(personnr)
        } answers {
            listOf(GrBostedsadresseperiode(
                    periode = DatoIntervallEntitet(
                            fom = LocalDate.of(2002, 1, 4),
                            tom = LocalDate.of(2022, 1, 5)
                    )))
        }
        every {
            personopplysningerService.hentOpphold(personnr)
        } answers {

            personInfo.opphold!!
        }
        /*
        listOf(Opphold(type = OPPHOLDSTILLATELSE.PERMANENT,
                           oppholdFra = personInfo.fødselsdato,
                           oppholdTil = LocalDate.of(2499, 1, 1)))*/
        every { personopplysningerService.hentDødsfall(Ident(personnr)) } returns DødsfallData(false, null)


    }
}

