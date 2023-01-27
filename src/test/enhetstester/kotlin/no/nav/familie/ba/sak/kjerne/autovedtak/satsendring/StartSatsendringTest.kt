package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.AutovedtakSatsendringService.Companion.harAlleredeSisteSats
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.Satskjøring
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType.ORDINÆR_BARNETRYGD
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType.SMÅBARNSTILLEGG
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType.UTVIDET_BARNETRYGD
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.task.OpprettTaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.time.YearMonth

private const val UGYLDIG_SATS = 1000
private val SATSTIDSPUNKT = YearMonth.of(2023, 3)

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
        every { behandlingRepository.findByFagsakAndAktivAndOpen(any()) } returns null

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
        justRun { opprettTaskService.opprettSatsendringTask(any(), any()) }

        val behandling = lagBehandling()

        every { fagsakRepository.finnLøpendeFagsakerForSatsendring(any()) } returns PageImpl(
            listOf(behandling.fagsak),
            Pageable.ofSize(5),
            0
        )

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
                    ORDINÆR_BARNETRYGD,
                    behandling = behandling,
                    person = lagPerson(),
                    aktør = lagPerson().aktør,
                    periodeIdOffset = 1,
                    beløp = 1676
                ),
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    YearMonth.of(2030, 12),
                    YearMonth.of(2039, 11),
                    ORDINÆR_BARNETRYGD,
                    behandling = behandling,
                    person = lagPerson(),
                    aktør = lagPerson().aktør,
                    periodeIdOffset = 1,
                    beløp = 1054
                )
            )

        startSatsendring.startSatsendring(5)

        verify(exactly = 1) { satskjøringRepository.save(any()) }
    }

    @Test
    fun `skal ikke starte satsendring hvis man har flere satstyper som ikke er skrudd på i featuretoggle`() {
        every { featureToggleService.isEnabled(any(), any()) } returns false
        // every { featureToggleService.isEnabled(FeatureToggleConfig.SATSENDRING_TILLEGG_ORBA, any()) } returns true

        val behandling = lagBehandling()

        every { fagsakRepository.finnLøpendeFagsakerForSatsendring(any()) } returns PageImpl(
            listOf(behandling.fagsak),
            Pageable.ofSize(5),
            0
        )

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
                    ORDINÆR_BARNETRYGD,
                    behandling = behandling,
                    person = lagPerson(),
                    aktør = lagPerson().aktør,
                    periodeIdOffset = 1,
                    beløp = 1676
                ),
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    YearMonth.of(2022, 12),
                    YearMonth.of(2039, 11),
                    SMÅBARNSTILLEGG,
                    behandling = behandling,
                    person = lagPerson(),
                    aktør = lagPerson().aktør,
                    periodeIdOffset = 1,
                    beløp = 660
                )
            )

        startSatsendring.startSatsendring(5)

        verify(exactly = 0) { satskjøringRepository.save(any()) }
    }

    @Test
    fun `start satsendring på sak hvis sakstypen er en av de som er togglet på`() {
        every { featureToggleService.isEnabled(any(), any()) } returns true
        every { featureToggleService.isEnabled(FeatureToggleConfig.SATSENDRING_OPPRETT_TASKER) } returns true
        justRun { opprettTaskService.opprettSatsendringTask(any(), any()) }

        val behandling = lagBehandling()

        every { fagsakRepository.finnLøpendeFagsakerForSatsendring(any()) } returns PageImpl(
            listOf(behandling.fagsak),
            Pageable.ofSize(5),
            0
        )

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
                    ORDINÆR_BARNETRYGD,
                    behandling = behandling,
                    person = lagPerson(),
                    aktør = lagPerson().aktør,
                    periodeIdOffset = 1,
                    beløp = 1676
                ),
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    YearMonth.of(2030, 12),
                    YearMonth.of(2039, 11),
                    ORDINÆR_BARNETRYGD,
                    behandling = behandling,
                    person = lagPerson(),
                    aktør = lagPerson().aktør,
                    periodeIdOffset = 1,
                    beløp = 1054
                )
            )

        startSatsendring.startSatsendring(5)

        verify(exactly = 1) { satskjøringRepository.save(any()) }
    }

    @Test
    fun `Ikke start satsendring på sak hvis ytelsen utløper før satstidspunkt`() {
        every { featureToggleService.isEnabled(any(), any()) } returns true
        every { featureToggleService.isEnabled(FeatureToggleConfig.SATSENDRING_OPPRETT_TASKER) } returns true
        justRun { opprettTaskService.opprettSatsendringTask(any(), any()) }

        val behandling = lagBehandling()

        every { fagsakRepository.finnLøpendeFagsakerForSatsendring(any()) } returns PageImpl(
            listOf(behandling.fagsak),
            Pageable.ofSize(5),
            0
        )

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
                    ytelseType = ORDINÆR_BARNETRYGD,
                    behandling = behandling,
                    person = lagPerson(),
                    aktør = lagPerson().aktør,
                    periodeIdOffset = 1,
                    beløp = 1676
                )
            )

        startSatsendring.startSatsendring(5)

        verify(exactly = 0) { satskjøringRepository.save(any()) }
    }

    @Test
    fun `finnLøpendeFagsaker har totalt antall sider 3, så den skal kalle finnLøpendeFagsaker 3 ganger for å få 5 satsendringer`() {
        every { featureToggleService.isEnabled(any(), any()) } returns true
        every { featureToggleService.isEnabled(any()) } returns true
        justRun { opprettTaskService.opprettSatsendringTask(any(), any()) }

        val behandling = lagBehandling()

        every { fagsakRepository.finnLøpendeFagsakerForSatsendring(any()) } returns PageImpl(
            listOf(behandling.fagsak, behandling.fagsak),
            Pageable.ofSize(2), // 5/2 gir totalt 3 sider, så finnLøpendeFagsakerForSatsendring skal trigges 3 ganger
            5
        )

        every { behandlingRepository.finnSisteIverksatteBehandling(behandling.fagsak.id) } returns behandling

        every {
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
                behandling.id
            )
        } returns
            listOf(
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = YearMonth.of(2022, 12),
                    tom = YearMonth.of(2040, 12),
                    ytelseType = ORDINÆR_BARNETRYGD,
                    behandling = behandling,
                    person = lagPerson(),
                    aktør = lagPerson().aktør,
                    periodeIdOffset = 1,
                    beløp = 1676
                )
            )

        startSatsendring.startSatsendring(5)

        verify(exactly = 5) { satskjøringRepository.save(any()) }
        verify(exactly = 3) { fagsakRepository.finnLøpendeFagsakerForSatsendring(any()) }
    }

    @Test
    fun `Ikke start satsendring på sak som har åpen behandling`() {
        every { featureToggleService.isEnabled(any(), any()) } returns true
        every { featureToggleService.isEnabled(FeatureToggleConfig.SATSENDRING_OPPRETT_TASKER) } returns true
        justRun { opprettTaskService.opprettSatsendringTask(any(), any()) }

        val behandling = lagBehandling()

        every { behandlingRepository.findByFagsakAndAktivAndOpen(any()) } returns behandling

        every { fagsakRepository.finnLøpendeFagsakerForSatsendring(any()) } returns PageImpl(
            listOf(behandling.fagsak),
            Pageable.ofSize(5),
            0
        )

        startSatsendring.startSatsendring(5)

        verify(exactly = 0) { satskjøringRepository.save(any()) }
    }

    @Test
    fun `harAlleredeSatsendring skal returnere true hvis den har siste satsendring`() {
        val behandling = lagBehandling()
        val atyMedBareSmåbarnstillegg =
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(SatsType.SMA, behandling, SMÅBARNSTILLEGG)

        assertThat(harAlleredeSisteSats(atyMedBareSmåbarnstillegg, SATSTIDSPUNKT)).isEqualTo(true)

        val atyMedBareUtvidet =
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(SatsType.UTVIDET_BARNETRYGD, behandling, UTVIDET_BARNETRYGD)

        assertThat(harAlleredeSisteSats(atyMedBareUtvidet, SATSTIDSPUNKT)).isEqualTo(true)

        val atyMedBareOrba =
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(SatsType.ORBA, behandling, ORDINÆR_BARNETRYGD)

        assertThat(harAlleredeSisteSats(atyMedBareOrba, SATSTIDSPUNKT)).isEqualTo(true)

        val atyMedBareTilleggOrba =
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                SatsType.TILLEGG_ORBA,
                behandling,
                ORDINÆR_BARNETRYGD
            )

        assertThat(harAlleredeSisteSats(atyMedBareTilleggOrba, SATSTIDSPUNKT)).isEqualTo(true)

        assertThat(
            harAlleredeSisteSats(
                atyMedBareTilleggOrba + atyMedBareOrba + atyMedBareUtvidet + atyMedBareSmåbarnstillegg,
                SATSTIDSPUNKT
            )
        ).isEqualTo(true)
    }

    @Test
    fun `harAlleredeSatsendring skal returnere false hvis den har gammel satsendring`() {
        val behandling = lagBehandling()
        val atyMedUgyldigSatsSmåbarnstillegg =
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(SatsType.SMA, behandling, SMÅBARNSTILLEGG, UGYLDIG_SATS)

        assertThat(harAlleredeSisteSats(atyMedUgyldigSatsSmåbarnstillegg, SATSTIDSPUNKT)).isEqualTo(false)

        val atyMedUglydligSatsUtvidet =
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                SatsType.UTVIDET_BARNETRYGD,
                behandling,
                UTVIDET_BARNETRYGD,
                UGYLDIG_SATS
            )

        assertThat(harAlleredeSisteSats(atyMedUglydligSatsUtvidet, SATSTIDSPUNKT)).isEqualTo(false)

        val atyMedUgyldigSatsBareOrba =
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(SatsType.ORBA, behandling, ORDINÆR_BARNETRYGD, UGYLDIG_SATS)

        assertThat(harAlleredeSisteSats(atyMedUgyldigSatsBareOrba, SATSTIDSPUNKT)).isEqualTo(false)

        val atyMedUgyldigSatsTilleggOrba =
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                SatsType.TILLEGG_ORBA,
                behandling,
                ORDINÆR_BARNETRYGD,
                UGYLDIG_SATS
            )

        assertThat(harAlleredeSisteSats(atyMedUgyldigSatsTilleggOrba, SATSTIDSPUNKT)).isEqualTo(false)
    }

    @Test
    fun `harAlleredeSatsendring skal returnere false en av satsene ikke er ny`() {
        val behandling = lagBehandling()
        val atyMedUgyldigSatsSmåbarnstillegg =
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(SatsType.SMA, behandling, SMÅBARNSTILLEGG, UGYLDIG_SATS)

        val atyMedGyldigUtvidet =
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                SatsType.UTVIDET_BARNETRYGD,
                behandling,
                UTVIDET_BARNETRYGD
            )

        val atyMedBGyldigOrba =
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(SatsType.ORBA, behandling, ORDINÆR_BARNETRYGD)

        assertThat(
            harAlleredeSisteSats(
                atyMedBGyldigOrba + atyMedGyldigUtvidet + atyMedUgyldigSatsSmåbarnstillegg,
                SATSTIDSPUNKT
            )
        ).isEqualTo(false)
    }

    @Test
    fun `harAlleredeSatsendring skal returnere true på ytelse med rett sats når tom dato er på samme dato som satstidspunkt`() {
        val behandling = lagBehandling()
        val atySomGårUtPåSatstidspunktGyldig =
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = SATSTIDSPUNKT.minusMonths(1),
                tom = SATSTIDSPUNKT,
                ytelseType = ORDINÆR_BARNETRYGD,
                behandling = behandling,
                person = lagPerson(),
                aktør = lagPerson().aktør,
                periodeIdOffset = 1,
                beløp = SatsService.finnSisteSatsFor(SatsType.ORBA).beløp
            )

        assertThat(
            harAlleredeSisteSats(
                listOf(atySomGårUtPåSatstidspunktGyldig),
                SATSTIDSPUNKT
            )
        ).isEqualTo(true)

        val atySomGårUtPåSatstidspunktUgyldig =
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = SATSTIDSPUNKT.minusMonths(1),
                tom = SATSTIDSPUNKT,
                ytelseType = ORDINÆR_BARNETRYGD,
                behandling = behandling,
                person = lagPerson(),
                aktør = lagPerson().aktør,
                periodeIdOffset = 1,
                beløp = UGYLDIG_SATS
            )

        assertThat(
            harAlleredeSisteSats(
                listOf(atySomGårUtPåSatstidspunktUgyldig),
                SATSTIDSPUNKT
            )
        ).isEqualTo(false)
    }

    @Test
    fun `harAlleredeSatsendring skal returnere true hvis ingen aktive andel tilkjent ytelser`() {
        val behandling = lagBehandling()
        val utgåttAndelTilkjentYtelse =
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = SATSTIDSPUNKT.minusMonths(10),
                tom = SATSTIDSPUNKT.minusMonths(1),
                ytelseType = ORDINÆR_BARNETRYGD,
                behandling = behandling,
                person = lagPerson(),
                aktør = lagPerson().aktør,
                periodeIdOffset = 1,
                beløp = SatsService.finnSisteSatsFor(SatsType.ORBA).beløp
            )

        assertThat(
            harAlleredeSisteSats(
                listOf(utgåttAndelTilkjentYtelse),
                SATSTIDSPUNKT
            )
        ).isEqualTo(true)
    }

    @Test
    fun `harAlleredeSatsendring skal returnere true for ny sats når fom er på satstidspunktet`() {
        val behandling = lagBehandling()
        val utgåttAndelTilkjentYtelse =
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = SATSTIDSPUNKT,
                tom = SATSTIDSPUNKT.plusYears(10),
                ytelseType = ORDINÆR_BARNETRYGD,
                behandling = behandling,
                person = lagPerson(),
                aktør = lagPerson().aktør,
                periodeIdOffset = 1,
                beløp = SatsService.finnSisteSatsFor(SatsType.ORBA).beløp
            )

        assertThat(
            harAlleredeSisteSats(
                listOf(utgåttAndelTilkjentYtelse),
                SATSTIDSPUNKT
            )
        ).isEqualTo(true)
    }

    @Test
    fun `harAlleredeSatsendring skal returnere false for gammel sats når fom er på satstidspunktet`() {
        val behandling = lagBehandling()
        val utgåttAndelTilkjentYtelse =
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = SATSTIDSPUNKT,
                tom = SATSTIDSPUNKT.plusYears(10),
                ytelseType = ORDINÆR_BARNETRYGD,
                behandling = behandling,
                person = lagPerson(),
                aktør = lagPerson().aktør,
                periodeIdOffset = 1,
                beløp = UGYLDIG_SATS
            )

        assertThat(
            harAlleredeSisteSats(
                listOf(utgåttAndelTilkjentYtelse),
                SATSTIDSPUNKT
            )
        ).isEqualTo(false)
    }


    private fun lagAndelTilkjentYtelseMedEndreteUtbetalinger(
        satsType: SatsType,
        behandling: Behandling,
        ytelseType: YtelseType,
        beløp: Int? = null
    ) = listOf(
        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom = SatsService.finnSisteSatsFor(satsType).gyldigFom.minusMonths(1).toYearMonth(),
            tom = YearMonth.of(2040, 12),
            ytelseType = ytelseType,
            behandling = behandling,
            person = lagPerson(),
            aktør = lagPerson().aktør,
            periodeIdOffset = 1,
            beløp = beløp ?: SatsService.finnSisteSatsFor(satsType).beløp
        )
    )
}
