package no.nav.familie.ba.sak.kjerne.beregning

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.TestClockProvider
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.config.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType.ORDINÆR_BARNETRYGD
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType.UTVIDET_BARNETRYGD
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType.INSTITUSJON
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType.NORMAL
import no.nav.familie.ba.sak.kjerne.simulering.domene.AvregningPeriode
import no.nav.familie.ba.sak.kjerne.tidslinje.util.apr
import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jul
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jun
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mai
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mar
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal

class AvregningServiceTest {
    private val søker = lagPerson()
    private val barn1 = lagPerson()
    private val barn2 = lagPerson()

    private val sisteVedtatteBehandling = lagBehandling()
    private val inneværendeBehandling = lagBehandling()

    private val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val fagsakService = mockk<FagsakService>()
    private val beregningService = mockk<BeregningService>()

    private val avregningService =
        AvregningService(
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            clockProvider = TestClockProvider.lagClockProviderMedFastTidspunkt(mar(2025)),
            featureToggleService = featureToggleService,
            fagsakService = fagsakService,
            beregningService = beregningService,
        )

    @BeforeEach
    fun setup() {
        every { behandlingHentOgPersisterService.hent(any()) } returns inneværendeBehandling
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns sisteVedtatteBehandling
        every { featureToggleService.isEnabled(FeatureToggle.BRUK_FUNKSJONALITET_FOR_ULOVFESTET_MOTREGNING) } returns true
    }

    @Nested
    inner class `etterbetalingerOgFeilutbetalinger - ett barn` {
        @Test
        fun `skal returnere tom liste hvis det ikke finnes andeler`() {
            // Arrange
            val andelerForrigeBehandling = emptyList<AndelTilkjentYtelse>()
            val andelerInneværendeBehandling = emptyList<AndelTilkjentYtelse>()

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteVedtatteBehandling.id) } returns andelerForrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(inneværendeBehandling.id) } returns andelerInneværendeBehandling

            // Act
            val perioderMedEtterbetalingOgFeilutbetaling =
                avregningService.hentPerioderMedAvregning(behandlingId = inneværendeBehandling.id)

            // Assert
            assertThat(perioderMedEtterbetalingOgFeilutbetaling).isEmpty()
        }

        @Test
        fun `skal returnere tom liste hvis ingen andeler er endret`() {
            // Arrange
            val andelerForrigeBehandling =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = jan(2025),
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                )

            val andelerInneværendeBehandling =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = jan(2025),
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                )

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteVedtatteBehandling.id) } returns andelerForrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(inneværendeBehandling.id) } returns andelerInneværendeBehandling

            // Act
            val perioderMedEtterbetalingOgFeilutbetaling =
                avregningService.hentPerioderMedAvregning(behandlingId = inneværendeBehandling.id)

            // Assert
            assertThat(perioderMedEtterbetalingOgFeilutbetaling).isEmpty()
        }

        @Test
        fun `skal ikke returnere perioder med kun feilutbetaling`() {
            // Arrange
            val andelerForrigeBehandling =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = jan(2025),
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                )

            val andelerInneværendeBehandling = emptyList<AndelTilkjentYtelse>()

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteVedtatteBehandling.id) } returns andelerForrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(inneværendeBehandling.id) } returns andelerInneværendeBehandling

            // Act
            val perioderMedEtterbetalingOgFeilutbetaling =
                avregningService.hentPerioderMedAvregning(behandlingId = inneværendeBehandling.id)

            // Assert
            assertThat(perioderMedEtterbetalingOgFeilutbetaling).isEmpty()
        }

        @Test
        fun `skal ikke returnere perioder med kun etterbetaling`() {
            // Arrange
            val andelerForrigeBehandling = emptyList<AndelTilkjentYtelse>()

            val andelerInneværendeBehandling =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = jan(2025),
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                )

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteVedtatteBehandling.id) } returns andelerForrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(inneværendeBehandling.id) } returns andelerInneværendeBehandling

            // Act
            val perioderMedEtterbetalingOgFeilutbetaling =
                avregningService.hentPerioderMedAvregning(behandlingId = inneværendeBehandling.id)

            // Assert
            assertThat(perioderMedEtterbetalingOgFeilutbetaling).isEmpty()
        }
    }

    @Nested
    inner class `etterbetalingerOgFeilutbetalinger - to barn` {
        @Test
        fun `skal returnere tom liste hvis ingen andeler er endret for flere barn`() {
            // Arrange
            val andelerForrigeBehandling =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = jan(2025),
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = jan(2025),
                        person = barn2,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                )

            val andelerInneværendeBehandling =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = jan(2025),
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = jan(2025),
                        person = barn2,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                )

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteVedtatteBehandling.id) } returns andelerForrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(inneværendeBehandling.id) } returns andelerInneværendeBehandling

            // Act
            val perioderMedEtterbetalingOgFeilutbetaling =
                avregningService.hentPerioderMedAvregning(behandlingId = inneværendeBehandling.id)

            // Assert
            assertThat(perioderMedEtterbetalingOgFeilutbetaling).isEmpty()
        }

        @Test
        fun `skal summere etterbetaling for flere barn`() {
            // Arrange
            val andelerForrigeBehandling =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = jan(2025),
                        person = søker,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = jan(2025),
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = jan(2025),
                        person = barn2,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                )

            val andelerInneværendeBehandling =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = jan(2025),
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 2000,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = jan(2025),
                        person = barn2,
                        kalkulertUtbetalingsbeløp = 2000,
                    ),
                )

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteVedtatteBehandling.id) } returns andelerForrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(inneværendeBehandling.id) } returns andelerInneværendeBehandling

            // Act
            val perioderMedEtterbetalingOgFeilutbetaling =
                avregningService.hentPerioderMedAvregning(behandlingId = inneværendeBehandling.id)

            // Assert
            val forventet =
                AvregningPeriode(
                    totalEtterbetaling = 2000.toBigDecimal(),
                    totalFeilutbetaling = 1000.toBigDecimal(),
                    fom = 1.jan(2025),
                    tom = 31.jan(2025),
                )

            assertThat(perioderMedEtterbetalingOgFeilutbetaling.single()).isEqualTo(forventet)
        }

        @Test
        fun `skal summere feilutbetaling for flere barn`() {
            // Arrange
            val andelerForrigeBehandling =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = jan(2025),
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 2000,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = jan(2025),
                        person = barn2,
                        kalkulertUtbetalingsbeløp = 2000,
                    ),
                )

            val andelerInneværendeBehandling =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = jan(2025),
                        person = søker,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = jan(2025),
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = jan(2025),
                        person = barn2,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                )

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteVedtatteBehandling.id) } returns andelerForrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(inneværendeBehandling.id) } returns andelerInneværendeBehandling

            // Act
            val perioderMedEtterbetalingOgFeilutbetaling =
                avregningService.hentPerioderMedAvregning(behandlingId = inneværendeBehandling.id)

            // Assert
            val forventet =
                AvregningPeriode(
                    totalEtterbetaling = 1000.toBigDecimal(),
                    totalFeilutbetaling = 2000.toBigDecimal(),
                    fom = 1.jan(2025),
                    tom = 31.jan(2025),
                )

            assertThat(perioderMedEtterbetalingOgFeilutbetaling.single()).isEqualTo(forventet)
        }

        @Test
        fun `skal returnere feilutbetaling og etterbetaling for flere barn i samme periode`() {
            // Arrange
            val andelerForrigeBehandling =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = feb(2025),
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = feb(2025),
                        person = barn2,
                        kalkulertUtbetalingsbeløp = 2000,
                    ),
                )
            val andelerInneværendeBehandling =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = feb(2025),
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 2000,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = feb(2025),
                        person = barn2,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                )

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteVedtatteBehandling.id) } returns andelerForrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(inneværendeBehandling.id) } returns andelerInneværendeBehandling

            // Act
            val perioderMedEtterbetalingOgFeilutbetaling =
                avregningService.hentPerioderMedAvregning(behandlingId = inneværendeBehandling.id)

            // Assert
            val forventet =
                AvregningPeriode(
                    totalEtterbetaling = 2000.toBigDecimal(),
                    totalFeilutbetaling = 2000.toBigDecimal(),
                    fom = 1.jan(2025),
                    tom = 28.feb(2025),
                )

            assertThat(perioderMedEtterbetalingOgFeilutbetaling.single()).isEqualTo(forventet)
        }

        @Test
        fun `skal returnere tom liste hvis feilutbetaling og etterbetaling er i forskjellige perioder for flere barn`() {
            // Arrange
            val andelerForrigeBehandling =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jul(2024),
                        tom = jul(2024),
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = jan(2025),
                        person = barn2,
                        kalkulertUtbetalingsbeløp = 2000,
                    ),
                )
            val andelerInneværendeBehandling =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jul(2024),
                        tom = jul(2024),
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 2000,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = jan(2025),
                        person = barn2,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                )

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteVedtatteBehandling.id) } returns andelerForrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(inneværendeBehandling.id) } returns andelerInneværendeBehandling

            // Act
            val perioderMedEtterbetalingOgFeilutbetaling =
                avregningService.hentPerioderMedAvregning(behandlingId = inneværendeBehandling.id)

            // Assert
            assertThat(perioderMedEtterbetalingOgFeilutbetaling).isEmpty()
        }

        @Test
        fun `skal returnere to perioder hvis det er etterbetalingen endres i utbetalingsperioden`() {
            // Arrange
            val andelerForrigeBehandling =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan(2024),
                        tom = apr(2024),
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = jan(2024),
                        tom = apr(2024),
                        person = barn2,
                        kalkulertUtbetalingsbeløp = 2000,
                    ),
                )

            val andelerInneværendeBehandling =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan(2024),
                        tom = feb(2024),
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 2000,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = mar(2024),
                        tom = apr(2024),
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 3000,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = jan(2024),
                        tom = apr(2024),
                        person = barn2,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                )

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteVedtatteBehandling.id) } returns andelerForrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(inneværendeBehandling.id) } returns andelerInneværendeBehandling

            // Act
            val perioderMedEtterbetalingOgFeilutbetaling =
                avregningService.hentPerioderMedAvregning(behandlingId = inneværendeBehandling.id)

            // Assert
            val forventet =
                listOf(
                    AvregningPeriode(
                        totalEtterbetaling = 2000.toBigDecimal(),
                        totalFeilutbetaling = 2000.toBigDecimal(),
                        fom = 1.jan(2024),
                        tom = 29.feb(2024),
                    ),
                    AvregningPeriode(
                        totalEtterbetaling = 4000.toBigDecimal(),
                        totalFeilutbetaling = 2000.toBigDecimal(),
                        fom = 1.mar(2024),
                        tom = 30.apr(2024),
                    ),
                )

            assertThat(perioderMedEtterbetalingOgFeilutbetaling).isEqualTo(forventet)
        }

        @Test
        fun `skal returnere tom liste hvis toggle er skrudd av`() {
            // Arrange
            val andelerForrigeBehandling =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = jan(2025),
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = jan(2025),
                        person = barn2,
                        kalkulertUtbetalingsbeløp = 2000,
                    ),
                )
            val andelerInneværendeBehandling =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = jan(2025),
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 2000,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = jan(2025),
                        person = barn2,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                )

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteVedtatteBehandling.id) } returns andelerForrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(inneværendeBehandling.id) } returns andelerInneværendeBehandling
            every { featureToggleService.isEnabled(FeatureToggle.BRUK_FUNKSJONALITET_FOR_ULOVFESTET_MOTREGNING) } returns false

            // Act
            val perioderMedEtterbetalingOgFeilutbetaling =
                avregningService.hentPerioderMedAvregning(behandlingId = inneværendeBehandling.id)

            // Assert
            assertThat(perioderMedEtterbetalingOgFeilutbetaling).isEmpty()
        }

        @Test
        fun `skal returnere tom liste hvis behandlingskategori er EØS`() {
            // Arrange
            val andelerForrigeBehandling =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = jan(2025),
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = jan(2025),
                        person = barn2,
                        kalkulertUtbetalingsbeløp = 2000,
                    ),
                )
            val andelerInneværendeBehandling =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = jan(2025),
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 2000,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = jan(2025),
                        person = barn2,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                )

            val eøsBehandling = lagBehandling(behandlingKategori = BehandlingKategori.EØS)

            every { behandlingHentOgPersisterService.hent(any()) } returns eøsBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteVedtatteBehandling.id) } returns andelerForrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(eøsBehandling.id) } returns andelerInneværendeBehandling

            // Act
            val perioderMedEtterbetalingOgFeilutbetaling =
                avregningService.hentPerioderMedAvregning(behandlingId = inneværendeBehandling.id)

            // Assert
            assertThat(perioderMedEtterbetalingOgFeilutbetaling).isEmpty()
        }

        @Test
        fun `skal returnere tom liste hvis behandlingstype er TEKNISK_ENDRING`() {
            // Arrange
            val andelerForrigeBehandling =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = jan(2025),
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = jan(2025),
                        person = barn2,
                        kalkulertUtbetalingsbeløp = 2000,
                    ),
                )
            val andelerInneværendeBehandling =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = jan(2025),
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 2000,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = jan(2025),
                        person = barn2,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                )

            val tekniskEndringBehandling = lagBehandling(behandlingType = BehandlingType.TEKNISK_ENDRING)

            every { behandlingHentOgPersisterService.hent(any()) } returns tekniskEndringBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteVedtatteBehandling.id) } returns andelerForrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(tekniskEndringBehandling.id) } returns andelerInneværendeBehandling

            // Act
            val perioderMedEtterbetalingOgFeilutbetaling =
                avregningService.hentPerioderMedAvregning(behandlingId = inneværendeBehandling.id)

            // Assert
            assertThat(perioderMedEtterbetalingOgFeilutbetaling).isEmpty()
        }

        @Test
        fun `skal kun returnere perioder til og med forrige måned`() {
            // Arrange
            val andelerForrigeBehandling =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = apr(2025),
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = apr(2025),
                        person = barn2,
                        kalkulertUtbetalingsbeløp = 2000,
                    ),
                )

            val andelerInneværendeBehandling =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = apr(2025),
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 2000,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = apr(2025),
                        person = barn2,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                )

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteVedtatteBehandling.id) } returns andelerForrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(inneværendeBehandling.id) } returns andelerInneværendeBehandling

            // Act
            val perioderMedEtterbetalingOgFeilutbetaling =
                avregningService.hentPerioderMedAvregning(behandlingId = inneværendeBehandling.id)

            // Assert
            val forventet =
                AvregningPeriode(
                    totalEtterbetaling = 2000.toBigDecimal(),
                    totalFeilutbetaling = 2000.toBigDecimal(),
                    fom = 1.jan(2025),
                    tom = 28.feb(2025),
                )

            assertThat(perioderMedEtterbetalingOgFeilutbetaling.single()).isEqualTo(forventet)
        }
    }

    @Nested
    inner class HentOverlappendePerioderMedAndreFagsaker {
        @Test
        fun `Returner tom liste hvis fagsaktype ikke er SKJERMET_BARN, INSTITUSJON, or BARN_ENSLIG_MINDREÅRIG`() {
            every { behandlingHentOgPersisterService.hent(any()) } returns lagBehandling()

            val result = avregningService.hentOverlappendePerioderMedAndreFagsaker(behandlingId = inneværendeBehandling.id)

            assertThat(result).isEmpty()
        }

        @ParameterizedTest
        @EnumSource(FagsakType::class, names = ["SKJERMET_BARN", "INSTITUSJON", "BARN_ENSLIG_MINDREÅRIG" ])
        fun `returner tom liste hvis det ikke finnes noen andre fagsaker på aktøren`(fagsakType: FagsakType) {
            val fagsakSkjermetBarn = lagFagsak(type = fagsakType)
            every { behandlingHentOgPersisterService.hent(any()) } returns lagBehandling(fagsak = fagsakSkjermetBarn)
            every { fagsakService.hentAlleFagsakerForAktør(any()) } returns listOf(fagsakSkjermetBarn)

            val result = avregningService.hentOverlappendePerioderMedAndreFagsaker(behandlingId = inneværendeBehandling.id)

            assertThat(result).isEmpty()
        }

        @Test
        fun `returner overlappende periode med fagsakId på andre fagsaker`() {
            val fagsakForInstitusjon = lagFagsak(id = 1, type = INSTITUSJON, aktør = barn1.aktør)
            val behandlingForInstitusjon = lagBehandling(id = 1, fagsak = fagsakForInstitusjon)
            val normalFagsak = lagFagsak(id = 2, type = NORMAL, aktør = søker.aktør)
            val normalBehandling = lagBehandling(id = 2, fagsak = normalFagsak)

            every { behandlingHentOgPersisterService.hent(behandlingForInstitusjon.id) } returns behandlingForInstitusjon
            every { behandlingHentOgPersisterService.hent(normalBehandling.id) } returns normalBehandling

            every { fagsakService.hentAlleFagsakerForAktør(any()) } returns listOf(fagsakForInstitusjon, normalFagsak)
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingForInstitusjon.id) } returns
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = apr(2025),
                        prosent = BigDecimal(100),
                        behandling = behandlingForInstitusjon,
                        aktør = barn1.aktør,
                    ),
                )
            every { beregningService.hentRelevanteTilkjentYtelserForBarn(any(), any()) } returns
                listOf(
                    lagTilkjentYtelse(
                        behandling = normalBehandling,
                        lagAndelerTilkjentYtelse = {
                            setOf(
                                lagAndelTilkjentYtelse(
                                    fom = mar(2025),
                                    tom = jun(2025),
                                    prosent = BigDecimal(100),
                                    behandling = normalBehandling,
                                    ytelseType = ORDINÆR_BARNETRYGD,
                                    aktør = barn1.aktør,
                                ),
                                lagAndelTilkjentYtelse(
                                    fom = mar(2025),
                                    tom = jun(2025),
                                    prosent = BigDecimal(100),
                                    behandling = normalBehandling,
                                    ytelseType = UTVIDET_BARNETRYGD,
                                    aktør = søker.aktør,
                                ),
                            )
                        },
                    ),
                )

            val result = avregningService.hentOverlappendePerioderMedAndreFagsaker(behandlingId = behandlingForInstitusjon.id)

            assertThat(result).hasSize(1)
            assertThat(result.first().fagsaker).containsOnly(2)
            assertThat(result.first().fom).isEqualTo(mar(2025).førsteDagIInneværendeMåned())
            assertThat(result.first().tom).isEqualTo(apr(2025).sisteDagIInneværendeMåned())
        }

        @Test
        fun `returner tom liste hvis ikke overlappende`() {
            val fagsakForInstitusjon1 = lagFagsak(id = 1, type = INSTITUSJON)
            val behandlingForInstitusjon1 = lagBehandling(id = 1, fagsak = fagsakForInstitusjon1)
            val fagsakForInstitusjon2 = lagFagsak(id = 2, type = INSTITUSJON)
            val behandlingForInstitusjon2 = lagBehandling(id = 2, fagsak = fagsakForInstitusjon2)

            every { behandlingHentOgPersisterService.hent(behandlingForInstitusjon1.id) } returns behandlingForInstitusjon1
            every { behandlingHentOgPersisterService.hent(behandlingForInstitusjon2.id) } returns behandlingForInstitusjon2

            every { fagsakService.hentAlleFagsakerForAktør(any()) } returns listOf(fagsakForInstitusjon1, fagsakForInstitusjon2)
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingForInstitusjon1.id) } returns
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = apr(2025),
                        prosent = BigDecimal(100),
                        behandling = behandlingForInstitusjon1,
                    ),
                )
            every { beregningService.hentRelevanteTilkjentYtelserForBarn(any(), any()) } returns
                listOf(
                    lagTilkjentYtelse(
                        behandling = behandlingForInstitusjon2,
                        lagAndelerTilkjentYtelse = {
                            setOf(
                                lagAndelTilkjentYtelse(
                                    fom = mai(2025),
                                    tom = jun(2025),
                                    prosent = BigDecimal(100),
                                    behandling = behandlingForInstitusjon2,
                                ),
                            )
                        },
                    ),
                )

            val result = avregningService.hentOverlappendePerioderMedAndreFagsaker(behandlingId = behandlingForInstitusjon1.id)

            assertThat(result).hasSize(0)
        }
    }
}
