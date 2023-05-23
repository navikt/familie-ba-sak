package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.ba.sak.common.LocalDateService
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.AutovedtakSatsendringService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.SatsendringSvar
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.Satskjøring
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.ReaktiverÅpenBehandlingTask
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentService
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.SatsTidspunkt
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.ba.sak.task.BehandleFødselshendelseTask
import no.nav.familie.ba.sak.task.SatsendringTaskDto
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit

class BehandlingSatsendringTest(
    @Autowired private val mockLocalDateService: LocalDateService,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val behandleFødselshendelseTask: BehandleFødselshendelseTask,
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired private val personidentService: PersonidentService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val stegService: StegService,
    @Autowired private val autovedtakSatsendringService: AutovedtakSatsendringService,
    @Autowired private val andelTilkjentYtelseMedEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    @Autowired private val satskjøringRepository: SatskjøringRepository,
    @Autowired private val brevmalService: BrevmalService,
    @Autowired private val settPåVentService: SettPåVentService,
    @Autowired private val jdbcTemplate: JdbcTemplate,
    @Autowired private val taskRepositoryWrapper: TaskRepositoryWrapper,
    @Autowired private val featureToggleService: FeatureToggleService,
) : AbstractVerdikjedetest() {

    private val opprettedeTasks = mutableListOf<Task>()

    @BeforeEach
    fun setUp() {
        opprettedeTasks.clear()
        databaseCleanupService.truncate()
        mockkObject(SatsTidspunkt)
        // Grunnen til at denne mockes er egentlig at den indirekte påvirker hva SatsService.hentGyldigSatsFor
        // returnerer. Det vi ønsker er at den sist tillagte satsendringen ikke kommer med slik at selve
        // satsendringen som skal kjøres senere faktisk utgjør en endring (slik at behandlingsresultatet blir ENDRET).
        every { SatsTidspunkt.senesteSatsTidspunkt } returns LocalDate.of(2023, 2, 1)

        every { mockLocalDateService.now() } returns LocalDate.now().minusYears(6) andThen LocalDate.now()
        every { taskRepositoryWrapper.save(capture(opprettedeTasks)) } answers { firstArg() }
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SatsTidspunkt)
    }

    @Test
    fun `Skal kjøre satsendring på løpende fagsak hvor brukeren har barnetrygd under 6 år`() {
        val scenario = mockServerKlient().lagScenario(restScenario)
        val behandling = opprettBehandling(scenario)
        satskjøringRepository.saveAndFlush(Satskjøring(fagsakId = behandling.fagsak.id))

        // Fjerner mocking slik at den siste satsendringen vi fjernet via mocking nå skal komme med.
        unmockkObject(SatsTidspunkt)

        val satsendringResultat =
            autovedtakSatsendringService.kjørBehandling(SatsendringTaskDto(behandling.fagsak.id, YearMonth.of(2023, 3)))

        assertEquals(SatsendringSvar.SATSENDRING_KJØRT_OK, satsendringResultat)

        val satsendringBehandling = behandlingHentOgPersisterService.finnAktivForFagsak(fagsakId = behandling.fagsak.id)
        assertEquals(Behandlingsresultat.ENDRET_UTBETALING, satsendringBehandling?.resultat)
        assertEquals(StegType.IVERKSETT_MOT_OPPDRAG, satsendringBehandling?.steg)

        val satsendingsvedtak = vedtakService.hentAktivForBehandling(behandlingId = satsendringBehandling!!.id)
        assertNull(satsendingsvedtak!!.stønadBrevPdF)

        val aty = andelTilkjentYtelseMedEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
            satsendringBehandling.id,
        )

        val atyMedSenesteTilleggOrbaSats =
            aty.first { it.type == YtelseType.ORDINÆR_BARNETRYGD && it.stønadFom == YearMonth.of(2023, 3) }
        val atyMedVanligOrbaSats =
            aty.first { it.type == YtelseType.ORDINÆR_BARNETRYGD && it.stønadFom == YearMonth.of(2029, 1) }
        assertThat(atyMedSenesteTilleggOrbaSats.sats).isEqualTo(SatsService.finnSisteSatsFor(SatsType.TILLEGG_ORBA).beløp)
        assertThat(atyMedVanligOrbaSats.sats).isEqualTo(SatsService.finnSisteSatsFor(SatsType.ORBA).beløp)

        val satskjøring = satskjøringRepository.findByFagsakId(behandling.fagsak.id)
        assertThat(satskjøring?.ferdigTidspunkt)
            .isCloseTo(LocalDateTime.now(), Assertions.within(30, ChronoUnit.SECONDS))

        assertThat(opprettedeTasks.filter { it.type == ReaktiverÅpenBehandlingTask.TASK_STEP_TYPE }).isEmpty()
    }

    @Test
    fun `Skal ignorere satsendring hvis siste sats alt er satt`() {
        // Fjerner mocking slik at den siste satsendringen vi fjernet via mocking nå skal komme med.
        unmockkObject(SatsTidspunkt)

        val scenario = mockServerKlient().lagScenario(restScenario)
        val behandling = opprettBehandling(scenario)
        satskjøringRepository.saveAndFlush(Satskjøring(fagsakId = behandling.fagsak.id))

        val satsendringResultat =
            autovedtakSatsendringService.kjørBehandling(SatsendringTaskDto(behandling.fagsak.id, YearMonth.of(2023, 3)))

        assertEquals(SatsendringSvar.SATSENDRING_ER_ALLEREDE_UTFØRT, satsendringResultat)

        val satskjøring = satskjøringRepository.findByFagsakId(behandling.fagsak.id)
        assertThat(satskjøring?.ferdigTidspunkt)
            .isCloseTo(LocalDateTime.now(), Assertions.within(30, ChronoUnit.SECONDS))
    }

    @Nested
    inner class ÅpenBehandling {
        // TODO hvor skal vi lage "ta av vent"-service som fjerner vedtak/tilkjent ytelse etc hvis behandlingen har vært på maskinell vent?

        @Test
        fun `Kan ikke sette åpen behandling på vent når behandlingen akkurat er opprettet`() {
            every { featureToggleService.isEnabled(FeatureToggleConfig.SATSENDRING_SNIKE_I_KØEN) } returns true
            val scenario = mockServerKlient().lagScenario(restScenario)
            val behandling = opprettBehandling(scenario)
            satskjøringRepository.saveAndFlush(Satskjøring(fagsakId = behandling.fagsak.id))

            // Opprett revurdering som blir liggende igjen som åpen og på behandlingsresultatsteget
            opprettRevurdering(scenario, behandling)

            // Fjerner mocking slik at den siste satsendringen vi fjernet via mocking nå skal komme med.
            unmockkObject(SatsTidspunkt)

            val satsendringTaskDto = SatsendringTaskDto(
                behandling.fagsak.id,
                YearMonth.of(2023, 3),
            )
            val satsendringResultat = autovedtakSatsendringService.kjørBehandling(satsendringTaskDto)

            assertThat(satsendringResultat).isEqualTo(SatsendringSvar.BEHANDLING_KAN_IKKE_SETTES_PÅ_VENT)
            assertThat(opprettedeTasks.filter { it.type == ReaktiverÅpenBehandlingTask.TASK_STEP_TYPE }).isEmpty()
        }

        @Test
        fun `Skal sette åpen behandling på maskinell vent hvis den er satt på vent av saksbehandler ved kjøring av satsendring`() {
            every { featureToggleService.isEnabled(FeatureToggleConfig.SATSENDRING_SNIKE_I_KØEN) } returns true
            val scenario = mockServerKlient().lagScenario(restScenario)
            val behandling = opprettBehandling(scenario)
            satskjøringRepository.saveAndFlush(Satskjøring(fagsakId = behandling.fagsak.id))

            // Opprett revurdering som blir liggende igjen som åpen og på behandlingsresultatsteget
            val revurdering = opprettRevurdering(scenario, behandling)
            justerLoggTidspunktForÅKunneSatsendreNårDetFinnesÅpenBehandling(revurdering)

            // Fjerner mocking slik at den siste satsendringen vi fjernet via mocking nå skal komme med.
            unmockkObject(SatsTidspunkt)

            val satsendringTaskDto = SatsendringTaskDto(
                behandling.fagsak.id,
                YearMonth.of(2023, 3),
            )
            val satsendringResultat = autovedtakSatsendringService.kjørBehandling(satsendringTaskDto)

            assertThat(satsendringResultat).isEqualTo(SatsendringSvar.SATSENDRING_KJØRT_OK)

            assertThat(opprettedeTasks.filter { it.type == ReaktiverÅpenBehandlingTask.TASK_STEP_TYPE }).hasSize(1)
        }

        // Kan fjernes når feature toggle er fjernet
        @Test
        fun `Skal ikke sette behandling på vent hvis feature toggle er slått av`() {
            every { featureToggleService.isEnabled(FeatureToggleConfig.SATSENDRING_SNIKE_I_KØEN) } returns false
            val scenario = mockServerKlient().lagScenario(restScenario)
            val behandling = opprettBehandling(scenario)
            satskjøringRepository.saveAndFlush(Satskjøring(fagsakId = behandling.fagsak.id))

            // Opprett revurdering som blir liggende igjen som åpen og på behandlingsresultatsteget
            val revurdering = opprettRevurdering(scenario, behandling)
            justerLoggTidspunktForÅKunneSatsendreNårDetFinnesÅpenBehandling(revurdering)

            // Fjerner mocking slik at den siste satsendringen vi fjernet via mocking nå skal komme med.
            unmockkObject(SatsTidspunkt)

            val satsendringTaskDto = SatsendringTaskDto(
                behandling.fagsak.id,
                YearMonth.of(2023, 3),
            )
            val satsendringResultat = autovedtakSatsendringService.kjørBehandling(satsendringTaskDto)

            assertThat(satsendringResultat).isEqualTo(SatsendringSvar.BEHANDLING_KAN_SETTES_PÅ_VENT_MEN_TOGGLE_ER_SLÅTT_AV)

            assertThat(opprettedeTasks.filter { it.type == ReaktiverÅpenBehandlingTask.TASK_STEP_TYPE }).isEmpty()
        }

        @Test
        fun `Skal sette behandling på vent på maskinell vent hvis den er satt på vent av saksbehandler ved kjøring av satsendring`() {
            every { featureToggleService.isEnabled(FeatureToggleConfig.SATSENDRING_SNIKE_I_KØEN) } returns true
            val scenario = mockServerKlient().lagScenario(restScenario)
            val behandling = opprettBehandling(scenario)
            satskjøringRepository.saveAndFlush(Satskjøring(fagsakId = behandling.fagsak.id))

            // Opprett revurdering som blir liggende igjen som åpen og på behandlingsresultatsteget
            val revurdering = opprettRevurdering(scenario, behandling)

            settPåVentService.settBehandlingPåVent(
                revurdering.id,
                LocalDate.now(),
                SettPåVentÅrsak.AVVENTER_DOKUMENTASJON,
            )

            // Fjerner mocking slik at den siste satsendringen vi fjernet via mocking nå skal komme med.
            unmockkObject(SatsTidspunkt)

            val satsendringTaskDto = SatsendringTaskDto(
                behandling.fagsak.id,
                YearMonth.of(2023, 3),
            )
            val satsendringResultat = autovedtakSatsendringService.kjørBehandling(satsendringTaskDto)

            assertThat(satsendringResultat).isEqualTo(SatsendringSvar.SATSENDRING_KJØRT_OK)

            assertThat(opprettedeTasks.filter { it.type == ReaktiverÅpenBehandlingTask.TASK_STEP_TYPE }).hasSize(1)
        }
    }

    private fun justerLoggTidspunktForÅKunneSatsendreNårDetFinnesÅpenBehandling(behandling: Behandling) {
        jdbcTemplate.update(
            "UPDATE logg SET opprettet_tid = opprettet_tid - interval '12 hours' WHERE fk_behandling_id = ?",
            behandling.id,
        )
    }

    private fun opprettRevurdering(
        scenario: RestScenario,
        behandling: Behandling,
    ) = familieBaSakKlient().opprettBehandling(
        søkersIdent = scenario.søker.ident!!,
        behandlingType = BehandlingType.REVURDERING,
        behandlingÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
        fagsakId = behandling.fagsak.id,
    ).data!!.let { behandlingRepository.getReferenceById(it.behandlingId) }

    private val matrikkeladresse = Matrikkeladresse(
        matrikkelId = 123L,
        bruksenhetsnummer = "H301",
        tilleggsnavn = "navn",
        postnummer = "0202",
        kommunenummer = "2231",
    )
    private val restScenario = RestScenario(
        søker = RestScenarioPerson(fødselsdato = "1993-01-12", fornavn = "Mor", etternavn = "Søker").copy(
            bostedsadresser = mutableListOf(
                Bostedsadresse(
                    angittFlyttedato = LocalDate.now().minusYears(10),
                    gyldigTilOgMed = null,
                    matrikkeladresse = matrikkeladresse,
                ),
            ),
        ),
        barna = listOf(
            RestScenarioPerson(
                fødselsdato = LocalDate.of(2023, 1, 1).toString(),
                fornavn = "Barn",
                etternavn = "Barnesen",
            ).copy(
                bostedsadresser = mutableListOf(
                    Bostedsadresse(
                        angittFlyttedato = LocalDate.now().minusYears(6),
                        gyldigTilOgMed = null,
                        matrikkeladresse = matrikkeladresse,
                    ),
                ),
            ),
        ),
    )

    private fun opprettBehandling(scenario: RestScenario) =
        behandleFødselshendelse(
            nyBehandlingHendelse = NyBehandlingHendelse(
                morsIdent = scenario.søker.ident!!,
                barnasIdenter = listOf(scenario.barna.first().ident!!),
            ),
            behandleFødselshendelseTask = behandleFødselshendelseTask,
            fagsakService = fagsakService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            vedtakService = vedtakService,
            stegService = stegService,
            personidentService = personidentService,
            brevmalService = brevmalService,
        )!!
}
