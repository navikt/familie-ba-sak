package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.Satskjøring
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.task.OpprettTaskService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class StartSatsendringTest {

    private val fagsakRepository: FagsakRepository = mockk()
    private val behandlingRepository: BehandlingRepository = mockk()
    private val opprettTaskService: OpprettTaskService = mockk()
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService =
        mockk()
    private val satskjøringRepository: SatskjøringRepository = mockk()
    private val featureToggleService: FeatureToggleService = mockk()

    lateinit var startSatsendring: StartSatsendring

    @BeforeEach
    fun setUp() {
        val satsSlot = slot<Satskjøring>()
        every { satskjøringRepository.save(capture(satsSlot)) } answers { satsSlot.captured }

        startSatsendring = StartSatsendring(
            fagsakRepository,
            behandlingRepository,
            opprettTaskService,
            andelerTilkjentYtelseOgEndreteUtbetalingerService,
            satskjøringRepository,
            featureToggleService
        )
    }

    @Test
    fun `start satsendring kun for saker som er feature togglet`() {
        every { featureToggleService.isEnabled(any(), any()) } returns false
        every { featureToggleService.isEnabled(FeatureToggleConfig.SATSENDRING_TILLEGG_ORBA, any()) } returns true
        every { featureToggleService.isEnabled(FeatureToggleConfig.SATSENDRING_OPPRETT_TASKER) } returns true
        justRun { opprettTaskService.opprettSatsendringTask(any()) }

        val behandling = lagBehandling()

        every { fagsakRepository.finnLøpendeFagsakerForSatsendring(any()) } returns listOf(behandling.fagsak)

        every { behandlingRepository.finnSisteIverksatteBehandling(behandling.fagsak.id) } returns behandling

        every {
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
                behandling.id
            )
        } returns
            listOf(
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    YearMonth.of(2022, 12),
                    YearMonth.of(2039, 11),
                    YtelseType.ORDINÆR_BARNETRYGD,
                    behandling = behandling,
                    person = lagPerson(),
                    aktør = lagPerson().aktør,
                    periodeIdOffset = 1,
                    beløp = 1676
                ),
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    YearMonth.of(2030, 12),
                    YearMonth.of(2039, 11),
                    YtelseType.ORDINÆR_BARNETRYGD,
                    behandling = behandling,
                    person = lagPerson(),
                    aktør = lagPerson().aktør,
                    periodeIdOffset = 1,
                    beløp = 1054
                )
            )

        startSatsendring.startSatsendring()

        verify(exactly = 1) { satskjøringRepository.save(any()) }
    }

    @Test
    fun `skal ikke starte satsendring hvis man har flere satstyper som ikke er skrudd på i featuretoggle`() {
        every { featureToggleService.isEnabled(any(), any()) } returns false
        // every { featureToggleService.isEnabled(FeatureToggleConfig.SATSENDRING_TILLEGG_ORBA, any()) } returns true

        val behandling = lagBehandling()

        every { fagsakRepository.finnLøpendeFagsakerForSatsendring(any()) } returns listOf(behandling.fagsak)

        every { behandlingRepository.finnSisteIverksatteBehandling(behandling.fagsak.id) } returns behandling

        every {
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
                behandling.id
            )
        } returns
            listOf(
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    YearMonth.of(2022, 12),
                    YearMonth.of(2039, 11),
                    YtelseType.ORDINÆR_BARNETRYGD,
                    behandling = behandling,
                    person = lagPerson(),
                    aktør = lagPerson().aktør,
                    periodeIdOffset = 1,
                    beløp = 1676
                ),
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    YearMonth.of(2022, 12),
                    YearMonth.of(2039, 11),
                    YtelseType.SMÅBARNSTILLEGG,
                    behandling = behandling,
                    person = lagPerson(),
                    aktør = lagPerson().aktør,
                    periodeIdOffset = 1,
                    beløp = 660
                )
            )

        startSatsendring.startSatsendring()

        verify(exactly = 0) { satskjøringRepository.save(any()) }
    }

    @Test
    fun `start satsendring på sak hvis sakstypen er en av de som er togglet på`() {
        every { featureToggleService.isEnabled(any(), any()) } returns true
        every { featureToggleService.isEnabled(FeatureToggleConfig.SATSENDRING_OPPRETT_TASKER) } returns true
        justRun { opprettTaskService.opprettSatsendringTask(any()) }

        val behandling = lagBehandling()

        every { fagsakRepository.finnLøpendeFagsakerForSatsendring(any()) } returns listOf(behandling.fagsak)

        every { behandlingRepository.finnSisteIverksatteBehandling(behandling.fagsak.id) } returns behandling

        every {
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
                behandling.id
            )
        } returns
            listOf(
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    YearMonth.of(2022, 12),
                    YearMonth.of(2039, 11),
                    YtelseType.ORDINÆR_BARNETRYGD,
                    behandling = behandling,
                    person = lagPerson(),
                    aktør = lagPerson().aktør,
                    periodeIdOffset = 1,
                    beløp = 1676
                ),
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    YearMonth.of(2030, 12),
                    YearMonth.of(2039, 11),
                    YtelseType.ORDINÆR_BARNETRYGD,
                    behandling = behandling,
                    person = lagPerson(),
                    aktør = lagPerson().aktør,
                    periodeIdOffset = 1,
                    beløp = 1054
                )
            )

        startSatsendring.startSatsendring()

        verify(exactly = 1) { satskjøringRepository.save(any()) }
    }

    @Test
    fun `Ikke start satsendring på sak hvis ytelsen utløper før satstidspunkt`() {
        every { featureToggleService.isEnabled(any(), any()) } returns true
        every { featureToggleService.isEnabled(FeatureToggleConfig.SATSENDRING_OPPRETT_TASKER) } returns true
        justRun { opprettTaskService.opprettSatsendringTask(any()) }

        val behandling = lagBehandling()

        every { fagsakRepository.finnLøpendeFagsakerForSatsendring(any()) } returns listOf(behandling.fagsak)

        every { behandlingRepository.finnSisteIverksatteBehandling(behandling.fagsak.id) } returns behandling

        every {
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
                behandling.id
            )
        } returns
            listOf(
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = YearMonth.of(2022, 12),
                    tom = YearMonth.of(2023, 2),
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    behandling = behandling,
                    person = lagPerson(),
                    aktør = lagPerson().aktør,
                    periodeIdOffset = 1,
                    beløp = 1676
                )
            )

        startSatsendring.startSatsendring()

        verify(exactly = 0) { satskjøringRepository.save(any()) }
    }
}
