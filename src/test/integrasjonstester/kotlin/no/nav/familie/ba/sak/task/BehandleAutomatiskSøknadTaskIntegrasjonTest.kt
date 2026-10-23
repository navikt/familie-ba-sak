package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.common.ClockProvider
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagBarnetrygdSøknadV10
import no.nav.familie.ba.sak.datagenerator.lagBostedsadresse
import no.nav.familie.ba.sak.datagenerator.lagMatrikkeladresse
import no.nav.familie.ba.sak.fake.FakeBrevKlient
import no.nav.familie.ba.sak.fake.FakeIntegrasjonKlient
import no.nav.familie.ba.sak.fake.FakeOppdragBackendKlient
import no.nav.familie.ba.sak.fake.FakePersonopplysningerService
import no.nav.familie.ba.sak.internal.TestVerktøyService
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.FILTRERINGSREGLER_SØKNAD
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.domene.FiltreringResultatRepository
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.domene.erOppfylt
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs.SatsendringEøsKjøringService
import no.nav.familie.ba.sak.kjerne.autovedtak.søknad.AutovedtakSøknadMåBehandlesManueltFeil
import no.nav.familie.ba.sak.kjerne.autovedtak.søknad.AutovedtakSøknadMåBehandlesManueltÅrsak.SØKER_VILKÅR_BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.Søknadsinfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatStegValideringService
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brev
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseRepository
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpRepository
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.logg.LoggRepository
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.strengtfortrolig.StrengtFortroligService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeRepository
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.statistikk.automatiskbehandlingavSoknadStatistikk.AutomatiskBehandlingAvSøknadStatistikkRepository
import no.nav.familie.ba.sak.task.dto.BehandleAutomatiskSøknadTaskDTO
import no.nav.familie.ba.sak.task.dto.IverksettingTaskDTO
import no.nav.familie.ba.sak.task.dto.ManuellOppgaveType
import no.nav.familie.ba.sak.task.dto.OpprettOppgaveTaskDTO
import no.nav.familie.ba.sak.testfiler.Testfil.TEST_PDF
import no.nav.familie.kontrakter.ba.søknad.VersjonertBarnetrygdSøknadV10
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.prosessering.internal.TaskWorker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ActiveProfilesResolver
import org.springframework.transaction.UnexpectedRollbackException
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Import(BehandleAutomatiskSøknadTaskIntegrasjonTest.TestConfig::class)
@ActiveProfiles(
    resolver = BehandleAutomatiskSøknadTaskIntegrasjonTest.ProfilResolver::class,
    inheritProfiles = false,
)
class BehandleAutomatiskSøknadTaskIntegrasjonTest(
    @Autowired private val taskWorker: TaskWorker,
    @Autowired private val taskService: TaskService,
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val persongrunnlagRepository: PersonopplysningGrunnlagRepository,
    @Autowired private val filtreringResultatRepository: FiltreringResultatRepository,
    @Autowired private val vilkårsvurderingService: VilkårsvurderingService,
    @Autowired private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    @Autowired private val vedtakRepository: VedtakRepository,
    @Autowired private val vedtaksperiodeRepository: VedtaksperiodeRepository,
    @Autowired private val totrinnskontrollRepository: TotrinnskontrollRepository,
    @Autowired private val søknadsinfoRepository: BehandlingSøknadsinfoRepository,
    @Autowired private val loggRepository: LoggRepository,
    @Autowired private val statistikkRepository: AutomatiskBehandlingAvSøknadStatistikkRepository,
    @Autowired private val integrasjonKlient: FakeIntegrasjonKlient,
) : AbstractSpringIntegrationTest() {
    @Autowired
    private lateinit var behandlingsresultatStegValideringService: TestBehandlingsresultatStegValideringService

    @Autowired
    private lateinit var brevKlient: TestBrevKlient

    private val prevalidertBehandlingId get() = behandlingsresultatStegValideringService.prevalidertBehandlingId

    @BeforeEach
    fun settOpp() {
        behandlingsresultatStegValideringService.prevalidertBehandlingId = null
        behandlingsresultatStegValideringService.førPrevalidering = {}
        brevKlient.førGenerering = {}
        brevKlient.genererteBrev.clear()
    }

    @Test
    fun `skal rulle hele behandlingsforsøket tilbake når preValiderSteg i et steg feiler`() {
        // Arrange
        val behandleAutomatiskSøknadTask = opprettBehandleAutomatiskSøknadTask()
        val feil = IllegalStateException("Feil i prevalidering")
        behandlingsresultatStegValideringService.førPrevalidering = {
            throw feil
        }

        // Act
        val exception = assertThrows<IllegalStateException> { taskWorker.doActualWork(behandleAutomatiskSøknadTask.id) }

        // Assert
        assertThat(exception).isSameAs(feil)
        verifiserFullRollback(behandleAutomatiskSøknadTask)
    }

    @Test
    fun `skal rulle hele behandlingsforsøket tilbake når brev feiler`() {
        // Arrange
        val behandleAutomatiskSøknadTask = opprettBehandleAutomatiskSøknadTask()
        val feil = IllegalStateException("Brevproduksjon feilet")
        brevKlient.førGenerering = {
            throw feil
        }

        // Act
        val exception = assertThrows<Feil> { taskWorker.doActualWork(behandleAutomatiskSøknadTask.id) }

        // Assert
        assertThat(exception).hasCause(feil)
        verifiserFullRollback(behandleAutomatiskSøknadTask)
    }

    // Legger til denne testen for fremheve oppdaget problem med å gjøre validering for automatisk søknad i preValiderSteg().
    // Vi har flyttet valideringen som skal føre til henleggelse og opprettelse av ny behandling utenfor for å unngå problemet, men her bør vi vurdere å skrive om slik at vi kan fortsette å bruke preValiderSteg()
    // selv i tilfeller vi ikke ønsker å rulle tilbake alt.
    @Test
    fun `skal rulle hele behandlingsforsøket tilbake selv om AutovedtakSøknadService fanger den spesifikke feilen`() {
        // Arrange
        val behandleAutomatiskSøknadTask = opprettBehandleAutomatiskSøknadTask()
        behandlingsresultatStegValideringService.førPrevalidering = {
            throw AutovedtakSøknadMåBehandlesManueltFeil("Feil fra prevalidering", SØKER_VILKÅR_BOSATT_I_RIKET)
        }

        // Act & Assert
        assertThrows<UnexpectedRollbackException> { taskWorker.doActualWork(behandleAutomatiskSøknadTask.id) }
        verifiserFullRollback(behandleAutomatiskSøknadTask)
    }

    @Test
    fun `skal ikke rulle tilbake hele behandlingsforsøket men henlegge automatisk behandling og opprette ny manuell behandling når validering mellom steg feiler`() {
        // Arrange
        val behandleAutomatiskSøknadTask = opprettBehandleAutomatiskSøknadTask(medHistoriskOppholdUtenNorskBostedsadresse = true)

        // Act
        taskWorker.doActualWork(behandleAutomatiskSøknadTask.id)

        // Assert
        val behandlinger = behandlingRepository.finnBehandlinger(behandleAutomatiskSøknadTask.metadata.getProperty("fagsakId").toLong())
        assertThat(behandlinger).hasSize(2)
        val automatiskSøknadBehandling = behandlinger.single { it.opprettetÅrsak == BehandlingÅrsak.AUTOMATISK_BEHANDLING_AV_SØKNAD }
        val manuellSøknadBehandling = behandlinger.single { it.opprettetÅrsak == BehandlingÅrsak.SØKNAD }
        assertThat(automatiskSøknadBehandling.resultat).isEqualTo(Behandlingsresultat.HENLAGT_AUTOMATISK_SØKNAD)
        assertThat(automatiskSøknadBehandling.status).isEqualTo(BehandlingStatus.AVSLUTTET)
        assertThat(automatiskSøknadBehandling.aktiv).isFalse()
        val filtreringResultater = filtreringResultatRepository.finnFiltreringResultater(automatiskSøknadBehandling.id)
        assertThat(filtreringResultater).hasSize(FILTRERINGSREGLER_SØKNAD.size)
        assertThat(filtreringResultater.erOppfylt()).isTrue()
        assertThat(prevalidertBehandlingId).isNull()
        assertThat(
            vilkårsvurderingService
                .hentAktivForBehandlingThrows(automatiskSøknadBehandling.id)
                .personResultater
                .single { it.erSøkersResultater() }
                .vilkårResultater
                .filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }
                .sortedBy { it.periodeFom }
                .map { it.resultat },
        ).containsExactly(Resultat.OPPFYLT, Resultat.IKKE_OPPFYLT, Resultat.OPPFYLT)
        assertThat(manuellSøknadBehandling.skalBehandlesAutomatisk).isFalse()
        assertThat(manuellSøknadBehandling.aktiv).isTrue()
        assertThat(manuellSøknadBehandling.status).isEqualTo(BehandlingStatus.UTREDES)
        assertThat(persongrunnlagRepository.findByBehandlingAndAktiv(manuellSøknadBehandling.id)).isNotNull()
        assertThat(søknadsinfoRepository.findByBehandlingId(manuellSøknadBehandling.id)).hasSize(1)
        assertThat(statistikkRepository.findAll().filter { it.behandlingId == automatiskSøknadBehandling.id }.map { it.årsakTilManuellBehandling })
            .containsExactly(SØKER_VILKÅR_BOSATT_I_RIKET.name)
        val tasker = hentTaskerOpprettetEtterKjøringAvTask(behandleAutomatiskSøknadTask)
        assertThat(tasker.filter { it.type == IverksettMotOppdragTask.TASK_STEP_TYPE }).isEmpty()
        assertThat(
            tasker
                .filter { it.type == OpprettOppgaveTask.TASK_STEP_TYPE }
                .map { jsonMapper.readValue(it.payload, OpprettOppgaveTaskDTO::class.java) }
                .filter { it.behandlingId == manuellSøknadBehandling.id && it.manuellOppgaveType == ManuellOppgaveType.SØKNAD },
        ).hasSize(1)
        assertThat(taskService.findById(behandleAutomatiskSøknadTask.id).status).isEqualTo(Status.FERDIG)
    }

    @Test
    fun `skal ikke rulle tilbake hele behandlingsforsøket men fullføre automatisk behandling og opprette iverksettingstask når ingen valideringer feiler`() {
        // Arrange
        val behandleAutomatiskSøknadTask = opprettBehandleAutomatiskSøknadTask()

        // Act
        taskWorker.doActualWork(behandleAutomatiskSøknadTask.id)

        // Assert
        val behandlinger = behandlingRepository.finnBehandlinger(behandleAutomatiskSøknadTask.metadata.getProperty("fagsakId").toLong())
        assertThat(behandlinger).hasSize(1)
        val behandling = behandlinger.single()
        assertThat(behandling.opprettetÅrsak).isEqualTo(BehandlingÅrsak.AUTOMATISK_BEHANDLING_AV_SØKNAD)
        assertThat(behandling.skalBehandlesAutomatisk).isTrue()
        assertThat(behandling.resultat).isEqualTo(Behandlingsresultat.INNVILGET)
        assertThat(behandling.status).isEqualTo(BehandlingStatus.IVERKSETTER_VEDTAK)
        assertThat(behandling.steg).isEqualTo(StegType.IVERKSETT_MOT_OPPDRAG)
        assertThat(filtreringResultatRepository.finnFiltreringResultater(behandling.id)).hasSize(FILTRERINGSREGLER_SØKNAD.size)
        assertThat(tilkjentYtelseRepository.findByBehandling(behandling.id).andelerTilkjentYtelse).isNotEmpty()
        assertThat(persongrunnlagRepository.findByBehandlingAndAktiv(behandling.id)).isNotNull()
        assertThat(søknadsinfoRepository.findByBehandlingId(behandling.id)).hasSize(1)
        val vedtak = vedtakRepository.findByBehandlingAndAktiv(behandling.id)
        assertThat(vedtak.stønadBrevPdF).isEqualTo(TEST_PDF)
        assertThat(brevKlient.genererteBrev).hasSize(1)
        assertThat(
            vedtaksperiodeRepository
                .finnVedtaksperioderFor(vedtak.id)
                .flatMap { it.begrunnelser }
                .map { it.standardbegrunnelse },
        ).contains(Standardbegrunnelse.INNVILGET_AUTOMATISK_SØKNAD)
        assertThat(totrinnskontrollRepository.findByBehandlingAndAktiv(behandling.id)?.godkjent).isTrue()
        val tasker = hentTaskerOpprettetEtterKjøringAvTask(behandleAutomatiskSøknadTask)
        val iverksettingstask = tasker.single { it.type == IverksettMotOppdragTask.TASK_STEP_TYPE }
        val iverksettingTaskDTO = jsonMapper.readValue(iverksettingstask.payload, IverksettingTaskDTO::class.java)
        assertThat(iverksettingTaskDTO.behandlingsId).isEqualTo(behandling.id)
        assertThat(iverksettingTaskDTO.vedtaksId).isEqualTo(vedtak.id)
        assertThat(tasker.filter { it.type == OpprettOppgaveTask.TASK_STEP_TYPE }).isEmpty()
        assertThat(statistikkRepository.findAll().filter { it.behandlingId == behandling.id }).isEmpty()
        assertThat(taskService.findById(behandleAutomatiskSøknadTask.id).status).isEqualTo(Status.FERDIG)
    }

    private fun opprettBehandleAutomatiskSøknadTask(medHistoriskOppholdUtenNorskBostedsadresse: Boolean = false): Task {
        val søkerIdent = FakePersonopplysningerService.leggTilPersonInfo(LocalDate.now().minusYears(30))
        val barnIdent = FakePersonopplysningerService.leggTilPersonInfo(LocalDate.now().minusYears(2))
        val fellesBostedsadresse =
            lagBostedsadresse(
                gyldigFraOgMed = LocalDate.now().minusMonths(2),
                gyldigTilOgMed = null,
                matrikkeladresse = lagMatrikkeladresse(matrikkelId = 123L, bruksenhetsnummer = "H0301", postnummer = "0202", kommunenummer = "0301"),
            )
        FakePersonopplysningerService.leggTilBostedsadresserIPersonInfo(listOf(søkerIdent, barnIdent), listOf(fellesBostedsadresse))
        if (medHistoriskOppholdUtenNorskBostedsadresse) {
            // Filtreringen ser dagens adresse. Preutfyllingen beholder også søkers historiske hull,
            // mens barnets adressehistorikk starter ved innflyttingen og oppfyller bor-med-søker.
            FakePersonopplysningerService.leggTilBostedsadresserIPersonInfo(
                listOf(søkerIdent),
                listOf(
                    fellesBostedsadresse.copy(
                        gyldigFraOgMed = LocalDate.now().minusYears(1),
                        angittFlyttedato = LocalDate.now().minusYears(1),
                        gyldigTilOgMed = LocalDate.now().minusMonths(4).minusDays(1),
                    ),
                    fellesBostedsadresse,
                ),
            )
        }
        FakePersonopplysningerService.leggTilRelasjonIPersonInfo(søkerIdent, barnIdent, FORELDERBARNRELASJONROLLE.BARN)
        FakePersonopplysningerService.leggTilRelasjonIPersonInfo(barnIdent, søkerIdent, FORELDERBARNRELASJONROLLE.MOR)
        val journalpostId = UUID.randomUUID().toString()
        integrasjonKlient.leggTilVersjonertBarnetrygdSøknad(
            journalpostId,
            VersjonertBarnetrygdSøknadV10(lagBarnetrygdSøknadV10(søkerFnr = søkerIdent, barnFnr = listOf(barnIdent))),
        )
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerIdent)
        FakeOppdragBackendKlient.leggTilSimuleringResultat(
            fagsak.id.toString(),
            DetaljertSimuleringResultat(
                FakeOppdragBackendKlient.simuleringsMottakere.map { mottaker ->
                    mottaker.copy(
                        mottakerNummer = søkerIdent,
                        simulertPostering = mottaker.simulertPostering.filter { it.posteringType == PosteringType.YTELSE && it.beløp.signum() > 0 },
                    )
                },
            ),
        )
        return taskService
            .save(
                BehandleAutomatiskSøknadTask
                    .opprettTask(
                        BehandleAutomatiskSøknadTaskDTO(
                            NyBehandling(
                                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                                behandlingÅrsak = BehandlingÅrsak.SØKNAD,
                                kategori = BehandlingKategori.NASJONAL,
                                underkategori = BehandlingUnderkategori.ORDINÆR,
                                fagsakId = fagsak.id,
                                barnasIdenter = listOf(barnIdent),
                                søknadMottattDato = LocalDate.now(),
                                søknadsinfo = Søknadsinfo(journalpostId = journalpostId, brevkode = "NAV 33-00.07", erDigital = true),
                            ),
                        ),
                    ).medTriggerTid(LocalDateTime.now().minusSeconds(1)),
            ).also {
                requireNotNull(taskWorker.markerPlukket(it.id))
            }
    }

    private fun verifiserFullRollback(task: Task) {
        val behandlingId = requireNotNull(prevalidertBehandlingId)
        assertThat(behandlingRepository.finnBehandlinger(task.metadata.getProperty("fagsakId").toLong())).isEmpty()
        assertThat(persongrunnlagRepository.findByBehandlingAndAktiv(behandlingId)).isNull()
        assertThat(vilkårsvurderingService.hentAktivForBehandling(behandlingId)).isNull()
        assertThat(tilkjentYtelseRepository.findByBehandlingOptional(behandlingId)).isNull()
        assertThat(vedtakRepository.finnVedtakForBehandling(behandlingId)).isEmpty()
        assertThat(totrinnskontrollRepository.findByBehandlingAndAktiv(behandlingId)).isNull()
        assertThat(søknadsinfoRepository.findByBehandlingId(behandlingId)).isEmpty()
        assertThat(loggRepository.hentLoggForBehandling(behandlingId)).isEmpty()
        assertThat(filtreringResultatRepository.finnFiltreringResultater(behandlingId)).isEmpty()
        assertThat(statistikkRepository.findAll().filter { it.behandlingId == behandlingId }).isEmpty()
        assertThat(hentTaskerOpprettetEtterKjøringAvTask(task)).isEmpty()
        assertThat(taskService.findById(task.id).status).isEqualTo(Status.PLUKKET)
    }

    private fun hentTaskerOpprettetEtterKjøringAvTask(task: Task): List<Task> {
        val fagsakId = task.metadata.getProperty("fagsakId")
        val behandlingIder = behandlingRepository.finnBehandlinger(fagsakId.toLong()).map { it.id }.toSet() + listOfNotNull(prevalidertBehandlingId)
        return taskService.findAll().filter {
            it.id != task.id &&
                (
                    it.metadata.getProperty("fagsakId") == fagsakId ||
                        it.metadata.getProperty("behandlingId")?.toLong() in behandlingIder ||
                        (it.type == OpprettOppgaveTask.TASK_STEP_TYPE && jsonMapper.readValue(it.payload, OpprettOppgaveTaskDTO::class.java).behandlingId in behandlingIder)
                )
        }
    }

    class ProfilResolver : ActiveProfilesResolver {
        override fun resolve(testClass: Class<*>): Array<String> =
            AbstractSpringIntegrationTest::class.java
                .getAnnotation(ActiveProfiles::class.java)
                .value
                // Unngår å bruke fake-implementasjoner av taskRepository og BrevKlient da vi ønsker å lagre tasker i databasen og ønsker å kunne trigge feil fra BrevKlient.
                .filterNot { it in setOf("fake-task-repository", "mock-brev-klient") }
                .toTypedArray()
    }

    @TestConfiguration
    class TestConfig {
        @Bean
        @Primary
        fun testBehandlingsresultatStegValideringService(
            beregningService: BeregningService,
            behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
            andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
            vilkårService: VilkårService,
            kompetanseRepository: KompetanseRepository,
            utenlandskPeriodebeløpRepository: UtenlandskPeriodebeløpRepository,
            valutakursRepository: ValutakursRepository,
            andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
            strengtFortroligService: StrengtFortroligService,
            persongrunnlagService: PersongrunnlagService,
            clockProvider: ClockProvider,
            satsendringEøsKjøringService: SatsendringEøsKjøringService,
        ): TestBehandlingsresultatStegValideringService =
            TestBehandlingsresultatStegValideringService(
                beregningService,
                behandlingHentOgPersisterService,
                andelTilkjentYtelseRepository,
                vilkårService,
                kompetanseRepository,
                utenlandskPeriodebeløpRepository,
                valutakursRepository,
                andelerTilkjentYtelseOgEndreteUtbetalingerService,
                strengtFortroligService,
                persongrunnlagService,
                clockProvider,
                satsendringEøsKjøringService,
            )

        @Bean
        @Primary
        fun testBrevKlient(testVerktøyService: TestVerktøyService): TestBrevKlient = TestBrevKlient(testVerktøyService)
    }

    class TestBehandlingsresultatStegValideringService(
        beregningService: BeregningService,
        behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
        andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
        vilkårService: VilkårService,
        kompetanseRepository: KompetanseRepository,
        utenlandskPeriodebeløpRepository: UtenlandskPeriodebeløpRepository,
        valutakursRepository: ValutakursRepository,
        andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
        strengtFortroligService: StrengtFortroligService,
        persongrunnlagService: PersongrunnlagService,
        clockProvider: ClockProvider,
        satsendringEøsKjøringService: SatsendringEøsKjøringService,
    ) : BehandlingsresultatStegValideringService(
            beregningService,
            behandlingHentOgPersisterService,
            andelTilkjentYtelseRepository,
            vilkårService,
            kompetanseRepository,
            utenlandskPeriodebeløpRepository,
            valutakursRepository,
            andelerTilkjentYtelseOgEndreteUtbetalingerService,
            strengtFortroligService,
            persongrunnlagService,
            clockProvider,
            satsendringEøsKjøringService,
        ) {
        var prevalidertBehandlingId: Long? = null
        var førPrevalidering: (Behandling) -> Unit = {}

        override fun validerBehandlingFørBehandlingsresultat(behandling: Behandling) {
            // Automatiske søknader hopper ellers over denne valideringen.
            prevalidertBehandlingId = behandling.id
            førPrevalidering(behandling)
            super.validerBehandlingFørBehandlingsresultat(behandling)
        }
    }

    class TestBrevKlient(
        testVerktøyService: TestVerktøyService,
    ) : FakeBrevKlient(testVerktøyService) {
        var førGenerering: () -> Unit = {}

        override fun genererBrev(
            målform: String,
            brev: Brev,
        ): ByteArray {
            førGenerering()
            return super.genererBrev(målform, brev)
        }
    }
}
