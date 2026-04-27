package no.nav.familie.ba.sak.kjerne.beregning

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.TestClockProvider
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.simulering.domene.AvregningPeriode
import no.nav.familie.ba.sak.kjerne.tidslinje.util.apr
import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jul
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mar
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class AvregningServiceTest {
    private val søker = lagPerson()
    private val barn1 = lagPerson()
    private val barn2 = lagPerson()

    private val sisteVedtatteBehandling = lagBehandling()
    private val inneværendeBehandling = lagBehandling()

    private val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val tilbakekrevingService = mockk<TilbakekrevingService>()
    private val featureToggleService = mockk<FeatureToggleService>()

    private val avregningService =
        AvregningService(
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            clockProvider = TestClockProvider.lagClockProviderMedFastTidspunkt(mar(2025)),
            tilbakekrevingService = tilbakekrevingService,
            featureToggleService = featureToggleService,
        )

    @BeforeEach
    fun setup() {
        every { behandlingHentOgPersisterService.hent(any()) } returns inneværendeBehandling
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns sisteVedtatteBehandling
        every { tilbakekrevingService.hentBehandlingKnyttetTilÅpenTilbakekreving(any()) } returns null
        every { featureToggleService.isEnabled(any<FeatureToggle>()) } returns false
        every { featureToggleService.isEnabled(any<FeatureToggle>(), any<Boolean>()) } returns false
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
        fun `skal returnere tom liste hvis opprettet årsak er endre migreringsdato`() {
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

            val endreMigreringsdatoBehandling = lagBehandling(behandlingKategori = BehandlingKategori.NASJONAL, årsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO)

            every { behandlingHentOgPersisterService.hent(any()) } returns endreMigreringsdatoBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteVedtatteBehandling.id) } returns andelerForrigeBehandling
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(endreMigreringsdatoBehandling.id) } returns andelerInneværendeBehandling

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
    inner class `avregning på tvers av behandlinger` {
        private val fagsak = lagFagsak()

        private val vedtattBehandling1 =
            lagBehandling(
                fagsak = fagsak,
                status = BehandlingStatus.AVSLUTTET,
                resultat = Behandlingsresultat.INNVILGET,
                aktivertTid = LocalDateTime.of(2024, 1, 1, 0, 0),
            )
        private val vedtattBehandling2 =
            lagBehandling(
                fagsak = fagsak,
                status = BehandlingStatus.AVSLUTTET,
                resultat = Behandlingsresultat.INNVILGET,
                aktivertTid = LocalDateTime.of(2024, 6, 1, 0, 0),
            )
        private val gjeldendBehandling =
            lagBehandling(
                fagsak = fagsak,
            )

        @BeforeEach
        fun setup() {
            every { featureToggleService.isEnabled(FeatureToggle.UTLED_AVREGNING_PÅ_TVERS_AV_BEHANDLINGER) } returns true
        }

        @Test
        fun `skal returnere tom liste når det ikke finnes åpen tilbakekreving`() {
            // Arrange
            every { behandlingHentOgPersisterService.hent(gjeldendBehandling.id) } returns gjeldendBehandling
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns null
            every { tilbakekrevingService.hentBehandlingKnyttetTilÅpenTilbakekreving(fagsak.id) } returns null

            // Act
            val perioder = avregningService.hentPerioderMedAvregning(gjeldendBehandling.id)

            // Assert
            assertThat(perioder).isEmpty()
        }

        @Test
        fun `skal returnere tom liste for EØS behandling selv med åpen tilbakekreving`() {
            // Arrange
            val eøsBehandling = lagBehandling(fagsak = fagsak, behandlingKategori = BehandlingKategori.EØS)

            every { behandlingHentOgPersisterService.hent(eøsBehandling.id) } returns eøsBehandling

            // Act
            val perioder = avregningService.hentPerioderMedAvregning(eøsBehandling.id)

            // Assert
            assertThat(perioder).isEmpty()
        }

        @Test
        fun `skal finne avregningsperioder på tvers av behandlinger`() {
            // Arrange
            every { behandlingHentOgPersisterService.hent(gjeldendBehandling.id) } returns gjeldendBehandling
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns null
            every { tilbakekrevingService.hentBehandlingKnyttetTilÅpenTilbakekreving(fagsak.id) } returns vedtattBehandling2
            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(vedtattBehandling2) } returns vedtattBehandling1

            val andelerBehandling1 =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = feb(2025),
                        person = barn1,
                        behandling = vedtattBehandling1,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = feb(2025),
                        person = barn2,
                        behandling = vedtattBehandling1,
                        kalkulertUtbetalingsbeløp = 2000,
                    ),
                )

            val andelerGjeldende =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = feb(2025),
                        person = barn1,
                        behandling = gjeldendBehandling,
                        kalkulertUtbetalingsbeløp = 2000,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = feb(2025),
                        person = barn2,
                        behandling = gjeldendBehandling,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                )

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(vedtattBehandling1.id) } returns andelerBehandling1
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(gjeldendBehandling.id) } returns andelerGjeldende

            // Act
            val perioder = avregningService.hentPerioderMedAvregning(gjeldendBehandling.id)

            // Assert
            val forventet =
                AvregningPeriode(
                    totalEtterbetaling = 2000.toBigDecimal(),
                    totalFeilutbetaling = 2000.toBigDecimal(),
                    fom = 1.jan(2025),
                    tom = 28.feb(2025),
                )
            assertThat(perioder).hasSize(1)
            assertThat(perioder.single()).isEqualTo(forventet)
        }

        @Test
        fun `skal returnere tom liste når det ikke er avregning mellom behandlinger i kjeden`() {
            // Arrange
            every { behandlingHentOgPersisterService.hent(gjeldendBehandling.id) } returns gjeldendBehandling
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns null
            every { tilbakekrevingService.hentBehandlingKnyttetTilÅpenTilbakekreving(fagsak.id) } returns vedtattBehandling2
            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(vedtattBehandling2) } returns vedtattBehandling1

            val andelerBehandling1 =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = feb(2025),
                        person = barn1,
                        behandling = vedtattBehandling1,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                )

            val andelerGjeldende =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = feb(2025),
                        person = barn1,
                        behandling = gjeldendBehandling,
                        kalkulertUtbetalingsbeløp = 2000,
                    ),
                )

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(vedtattBehandling1.id) } returns andelerBehandling1
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(gjeldendBehandling.id) } returns andelerGjeldende

            // Act
            val perioder = avregningService.hentPerioderMedAvregning(gjeldendBehandling.id)

            // Assert
            assertThat(perioder).isEmpty()
        }

        @Test
        fun `behandlingHarPerioderSomAvregnes returnerer true ved avregning på tvers`() {
            // Arrange
            every { behandlingHentOgPersisterService.hent(gjeldendBehandling.id) } returns gjeldendBehandling
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns null
            every { tilbakekrevingService.hentBehandlingKnyttetTilÅpenTilbakekreving(fagsak.id) } returns vedtattBehandling2
            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(vedtattBehandling2) } returns vedtattBehandling1

            val andelerBehandling1 =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = feb(2025),
                        person = barn1,
                        behandling = vedtattBehandling1,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = feb(2025),
                        person = barn2,
                        behandling = vedtattBehandling1,
                        kalkulertUtbetalingsbeløp = 2000,
                    ),
                )

            val andelerGjeldende =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = feb(2025),
                        person = barn1,
                        behandling = gjeldendBehandling,
                        kalkulertUtbetalingsbeløp = 2000,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = feb(2025),
                        person = barn2,
                        behandling = gjeldendBehandling,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                )

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(vedtattBehandling1.id) } returns andelerBehandling1
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(gjeldendBehandling.id) } returns andelerGjeldende

            // Act
            val harAvregning = avregningService.behandlingHarPerioderSomAvregnes(gjeldendBehandling.id)

            // Assert
            assertThat(harAvregning).isTrue()
        }

        @Test
        fun `skal detektere avregning når nåværende behandling har etterbetaling i periode med åpen tilbakekreving`() {
            // B1: barn1 = 2000/mnd for jan-feb 2025
            // B2: barn1 = 1000/mnd for jan-feb 2025 (reduksjon → tilbakekreving opprettet, knyttet til B2)
            // B3 (gjeldende): barn1 = 1000/mnd, barn2 = 1500/mnd for jan-feb 2025 (barn2 innvilget)
            //
            // Forventet: avregning fordi etterbetalingen for barn2 (B3→B2) vil bli avregnet mot
            // feilutbetalingen for barn1 (B2→B1) i økonomi, siden tilbakekrevingen er åpen.

            // Arrange
            every { behandlingHentOgPersisterService.hent(gjeldendBehandling.id) } returns gjeldendBehandling
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns vedtattBehandling2
            every { tilbakekrevingService.hentBehandlingKnyttetTilÅpenTilbakekreving(fagsak.id) } returns vedtattBehandling2
            every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(vedtattBehandling2) } returns vedtattBehandling1

            val andelerBehandling1 =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = feb(2025),
                        person = barn1,
                        behandling = vedtattBehandling1,
                        kalkulertUtbetalingsbeløp = 2000,
                    ),
                )

            val andelerBehandling2 =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = feb(2025),
                        person = barn1,
                        behandling = vedtattBehandling2,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                )

            val andelerGjeldende =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = feb(2025),
                        person = barn1,
                        behandling = gjeldendBehandling,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = jan(2025),
                        tom = feb(2025),
                        person = barn2,
                        behandling = gjeldendBehandling,
                        kalkulertUtbetalingsbeløp = 1500,
                    ),
                )

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(vedtattBehandling1.id) } returns andelerBehandling1
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(vedtattBehandling2.id) } returns andelerBehandling2
            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(gjeldendBehandling.id) } returns andelerGjeldende

            // Act
            val perioder = avregningService.hentPerioderMedAvregning(gjeldendBehandling.id)

            // Assert
            assertThat(perioder).isNotEmpty()
        }
    }
}
