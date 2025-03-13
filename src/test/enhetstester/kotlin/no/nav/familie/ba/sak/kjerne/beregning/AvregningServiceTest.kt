package no.nav.familie.ba.sak.kjerne.beregning

import io.mockk.mockk
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.tidslinje.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.YearMonth

class AvregningServiceTest {
    private val søker = lagPerson()
    private val barn1 = lagPerson()
    private val barn2 = lagPerson()
    private val jan25 = jan(2025)
    private val sisteDagIFeb25 = YearMonth.of(2025, 2).sisteDagIInneværendeMåned()

    private val avregningService =
        AvregningService(
            andelTilkjentYtelseRepository = mockk(),
            behandlingHentOgPersisterService = mockk(),
            clockProvider = mockk(),
        )

    @Test
    fun `skal finne etterbetaling for barn`() {
        // Arrange
        val andelerForrigeBehandling = emptyList<AndelTilkjentYtelse>()

        val andelerInneværendeBehandling =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan25,
                    tom = jan25,
                    person = barn1,
                    kalkulertUtbetalingsbeløp = 1000,
                ),
            )

        // Act
        val perioderMedEtterbetalingOgFeilutbetaling =
            avregningService
                .etterbetalingerOgFeilutbetalinger(
                    andelerInneværendeBehandling = andelerInneværendeBehandling,
                    andelerForrigeBehandling = andelerForrigeBehandling,
                    sisteDagIForrigeMåned = sisteDagIFeb25,
                )

        // Assert

        val forventet =
            Periode(
                verdi =
                    EtterbetalingOgFeilutbetaling(
                        etterbetaling = 1000,
                        feilutbetaling = 0,
                    ),
                fom = 1.jan(2025),
                tom = 31.jan(2025),
            )

        assertThat(perioderMedEtterbetalingOgFeilutbetaling).hasSize(1)
        assertThat(perioderMedEtterbetalingOgFeilutbetaling.single()).isEqualTo(forventet)
    }

    @Test
    fun `skal finne feilutbetaling for barn`() {
        // Arrange
        val andelerForrigeBehandling =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan25,
                    tom = jan25,
                    person = barn1,
                    kalkulertUtbetalingsbeløp = 1000,
                ),
            )

        val andelerInneværendeBehandling = emptyList<AndelTilkjentYtelse>()

        // Act
        val perioderMedEtterbetalingOgFeilutbetaling =
            avregningService
                .etterbetalingerOgFeilutbetalinger(
                    andelerInneværendeBehandling = andelerInneværendeBehandling,
                    andelerForrigeBehandling = andelerForrigeBehandling,
                    sisteDagIForrigeMåned = sisteDagIFeb25,
                )

        // Assert

        val forventet =
            Periode(
                verdi =
                    EtterbetalingOgFeilutbetaling(
                        etterbetaling = 0,
                        feilutbetaling = 1000,
                    ),
                fom = 1.jan(2025),
                tom = 31.jan(2025),
            )

        assertThat(perioderMedEtterbetalingOgFeilutbetaling).hasSize(1)
        assertThat(perioderMedEtterbetalingOgFeilutbetaling.single()).isEqualTo(forventet)
    }

    @Test
    fun etterbetalingerOgFeilutbetalinger() {
        // Arrange
        val andelerForrigeBehandling =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan25,
                    tom = jan25,
                    person = barn1,
                    kalkulertUtbetalingsbeløp = 1000,
                ),
            )

        val andelerInneværendeBehandling =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan25,
                    tom = jan25,
                    person = barn1,
                    kalkulertUtbetalingsbeløp = 1000,
                ),
            )

        // Act
        val perioderMedEtterbetalingOgFeilutbetaling =
            avregningService
                .etterbetalingerOgFeilutbetalinger(
                    andelerInneværendeBehandling = andelerInneværendeBehandling,
                    andelerForrigeBehandling = andelerForrigeBehandling,
                    sisteDagIForrigeMåned = sisteDagIFeb25,
                )

        // Assert
        assertThat(perioderMedEtterbetalingOgFeilutbetaling).isEmpty()
    }
}
