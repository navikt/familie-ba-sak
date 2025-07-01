package no.nav.familie.ba.sak.kjerne.beregning

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.TestClockProvider
import no.nav.familie.ba.sak.config.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.simulering.domene.AvregningPeriode
import no.nav.familie.ba.sak.kjerne.tidslinje.util.apr
import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jul
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mar
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AvregningServiceTest {
    private val søker = lagPerson()
    private val barn1 = lagPerson()
    private val barn2 = lagPerson()

    private val sisteIverksatteBehandling = lagBehandling()
    private val inneværendeBehandling = lagBehandling()

    private val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val unleashService = mockk<UnleashNextMedContextService>()
    private val fagsakService = mockk<FagsakService>()

    private val avregningService =
        AvregningService(
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            clockProvider = TestClockProvider.lagClockProviderMedFastTidspunkt(mar(2025)),
            unleashService = unleashService,
            fagsakService = fagsakService,
            beregningService = mockk(),
        )

    @BeforeEach
    fun setup() {
        every { behandlingHentOgPersisterService.hent(any()) } returns inneværendeBehandling
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(any()) } returns sisteIverksatteBehandling
        every { unleashService.isEnabled(FeatureToggle.BRUK_FUNKSJONALITET_FOR_ULOVFESTET_MOTREGNING) } returns true
    }

    @Nested
    inner class `etterbetalingerOgFeilutbetalinger - ett barn` {
        @Test
        fun `skal returnere tom liste hvis det ikke finnes andeler`() {
            // Arrange
            val andelerForrigeBehandling = emptyList<AndelTilkjentYtelse>()
            val andelerInneværendeBehandling = emptyList<AndelTilkjentYtelse>()

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteIverksatteBehandling.id) } returns andelerForrigeBehandling
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

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteIverksatteBehandling.id) } returns andelerForrigeBehandling
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

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteIverksatteBehandling.id) } returns andelerForrigeBehandling
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

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteIverksatteBehandling.id) } returns andelerForrigeBehandling
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

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteIverksatteBehandling.id) } returns andelerForrigeBehandling
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

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteIverksatteBehandling.id) } returns andelerForrigeBehandling
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

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteIverksatteBehandling.id) } returns andelerForrigeBehandling
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

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteIverksatteBehandling.id) } returns andelerForrigeBehandling
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

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteIverksatteBehandling.id) } returns andelerForrigeBehandling
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

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteIverksatteBehandling.id) } returns andelerForrigeBehandling
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

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteIverksatteBehandling.id) } returns andelerForrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(inneværendeBehandling.id) } returns andelerInneværendeBehandling
            every { unleashService.isEnabled(FeatureToggle.BRUK_FUNKSJONALITET_FOR_ULOVFESTET_MOTREGNING) } returns false

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
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteIverksatteBehandling.id) } returns andelerForrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(eøsBehandling.id) } returns andelerInneværendeBehandling

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

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteIverksatteBehandling.id) } returns andelerForrigeBehandling
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
}
