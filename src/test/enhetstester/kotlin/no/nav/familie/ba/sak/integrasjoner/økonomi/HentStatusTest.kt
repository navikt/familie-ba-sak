package no.nav.familie.ba.sak.integrasjoner.økonomi

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.datagenerator.årMnd
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.OppdaterTilkjentYtelseService
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.UtbetalingsoppdragGenerator
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.steg.StatusFraOppdrag
import no.nav.familie.ba.sak.kjerne.steg.StatusFraOppdragMedTask
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.task.StatusFraOppdragTask
import no.nav.familie.ba.sak.task.dto.StatusFraOppdragDTO
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month

class HentStatusTest {
    private val økonomiKlient = mockk<ØkonomiKlient>()

    private val oppdragBackendKlient = mockk<OppdragBackendKlient>()

    private val beregningService: BeregningService = mockk()

    lateinit var statusFraOppdrag: StatusFraOppdrag

    private val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()

    private val oppdaterTilkjentYtelseService = mockk<OppdaterTilkjentYtelseService>()

    private val utbetalingsoppdragGenerator: UtbetalingsoppdragGenerator = mockk()

    private val featureToggleService: FeatureToggleService = mockk()

    @BeforeEach
    fun setUp() {
        val økonomiService =
            ØkonomiService(
                økonomiKlient = økonomiKlient,
                oppdragBackendKlient = oppdragBackendKlient,
                tilkjentYtelseValideringService = mockk(),
                tilkjentYtelseRepository = tilkjentYtelseRepository,
                utbetalingsoppdragGenerator = utbetalingsoppdragGenerator,
                behandlingHentOgPersisterService = mockk(),
                oppdaterTilkjentYtelseService = oppdaterTilkjentYtelseService,
                featureToggleService = featureToggleService,
            )
        statusFraOppdrag =
            StatusFraOppdrag(
                økonomiService = økonomiService,
                taskRepository = mockk<TaskRepositoryWrapper>().also { every { it.save(any()) } returns mockk() },
            )

        every { featureToggleService.isEnabled(toggle = any()) } returns false
        every { featureToggleService.isEnabled(FeatureToggle.OPPDRAG_MIGRERING_IVERKSETT_OPPDRAG_GCP) } returns true
    }

    @Test
    fun `henter status fra økonomi for behandling der alle utbetalingene hører til denne behandlinga`() {
        // Arrange
        val tilfeldigPerson = tilfeldigPerson()
        val nyBehandling = lagBehandling()
        lagTilkjentYtelse(nyBehandling, listOf(lagUtbetalingsperiode(nyBehandling)))

        every {
            oppdragBackendKlient.hentStatus(
                match { it.behandlingsId == nyBehandling.id.toString() },
            )
        } returns OppdragStatus.KVITTERT_OK
        val andelerTilkjentYtelse =
            listOf(
                lagAndelTilkjentYtelse(
                    årMnd("2019-04"),
                    årMnd("2020-03"),
                    YtelseType.ORDINÆR_BARNETRYGD,
                    10,
                    behandling = nyBehandling,
                    person = tilfeldigPerson,
                    aktør = mockk(),
                    tilkjentYtelse = mockk(),
                    kildeBehandlingId = null,
                ),
            )

        every { beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(any()) } returns andelerTilkjentYtelse

        // Act
        val nesteSteg =
            statusFraOppdrag.utførStegOgAngiNeste(nyBehandling, statusFraOppdragMedTask(tilfeldigPerson, nyBehandling))

        // Assert
        assertThat(nesteSteg).isEqualTo(StegType.IVERKSETT_MOT_FAMILIE_TILBAKE)
        verify { oppdragBackendKlient.hentStatus(match { it.behandlingsId == nyBehandling.id.toString() }) }
    }

    @Test
    fun `kan håndtere nullutbetaling uten tidligere historikk`() {
        // Arrange
        val tilfeldigPerson = tilfeldigPerson()
        val nyBehandling = lagBehandling()
        lagTilkjentYtelse(nyBehandling, listOf())

        every {
            oppdragBackendKlient.hentStatus(
                match { it.behandlingsId == nyBehandling.id.toString() },
            )
        } returns OppdragStatus.KVITTERT_OK
        val andelerTilkjentYtelse =
            listOf(
                lagAndelTilkjentYtelse(
                    årMnd("2019-04"),
                    årMnd("2020-03"),
                    YtelseType.ORDINÆR_BARNETRYGD,
                    0,
                    behandling = nyBehandling,
                    person = tilfeldigPerson,
                    aktør = mockk(),
                    tilkjentYtelse = mockk(),
                    kildeBehandlingId = null,
                ),
            )

        every { beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(any()) } returns andelerTilkjentYtelse

        // Act
        val nesteSteg =
            statusFraOppdrag.utførStegOgAngiNeste(nyBehandling, statusFraOppdragMedTask(tilfeldigPerson, nyBehandling))

        // Assert
        assertThat(nesteSteg).isEqualTo(StegType.IVERKSETT_MOT_FAMILIE_TILBAKE)
        verify(exactly = 0) { oppdragBackendKlient.hentStatus(any()) }
    }

    private fun lagTilkjentYtelse(
        behandling: Behandling,
        utbetalingsperiode: List<Utbetalingsperiode>,
    ) {
        val nyTilkjentYtelse =
            lagInitiellTilkjentYtelse(
                behandling = behandling,
                utbetalingsoppdrag = jsonMapper.writeValueAsString(lagUtbetalingsoppdrag(utbetalingsperiode = utbetalingsperiode)),
            )
        every { tilkjentYtelseRepository.findByBehandling(behandling.id) } returns nyTilkjentYtelse
    }

    private fun statusFraOppdragMedTask(
        tilfeldigPerson: Person,
        nyBehandling: Behandling,
    ) = StatusFraOppdragMedTask(
        statusFraOppdragDTO =
            StatusFraOppdragDTO(
                fagsystem = "BA",
                personIdent = tilfeldigPerson.aktør.aktivFødselsnummer(),
                behandlingsId = nyBehandling.id,
                vedtaksId = 0L,
            ),
        task =
            Task(
                type = StatusFraOppdragTask.TASK_STEP_TYPE,
                payload = "",
            ),
    )

    private fun lagUtbetalingsperiode(nyBehandling: Behandling) =
        Utbetalingsperiode(
            vedtakdatoFom =
                LocalDate.of(
                    2019,
                    Month.APRIL,
                    1,
                ),
            vedtakdatoTom = LocalDate.of(2020, Month.MARCH, 31),
            erEndringPåEksisterendePeriode = false,
            periodeId = 1L,
            behandlingId = nyBehandling.id,
            datoForVedtak = LocalDate.of(2020, Month.APRIL, 1),
            klassifisering = "BATR",
            sats = BigDecimal.ONE,
            satsType = Utbetalingsperiode.SatsType.MND,
            utbetalesTil = "utbetalesTil",
        )
}
