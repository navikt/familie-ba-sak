package no.nav.familie.ba.sak.kjerne.beregning

import io.mockk.mockk
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.tidslinje.util.apr
import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jul
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mar
import no.nav.familie.tidslinje.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AvregningServiceTest {
    private val søker = lagPerson()
    private val barn1 = lagPerson()
    private val barn2 = lagPerson()
    private val juli24 = jul(2024)
    private val jan25 = jan(2025)
    private val mar25 = mar(2025)

    private val avregningService =
        AvregningService(
            andelTilkjentYtelseRepository = mockk(),
            behandlingHentOgPersisterService = mockk(),
            clockProvider = mockk(),
        )

    @Nested
    inner class `etterbetalingerOgFeilutbetalinger - ett barn` {
        @Test
        fun `skal ikke finne etterbetaling eller feilutbetaling for barn`() {
            // Arrange
            val andelerForrigeBehandling = emptyList<AndelTilkjentYtelse>()
            val andelerInneværendeBehandling = emptyList<AndelTilkjentYtelse>()

            // Act
            val perioderMedEtterbetalingOgFeilutbetaling =
                avregningService
                    .etterbetalingerOgFeilutbetalinger(
                        andelerInneværendeBehandling = andelerInneværendeBehandling,
                        andelerForrigeBehandling = andelerForrigeBehandling,
                        inneværendeMåned = mar25,
                    )

            // Assert
            assertThat(perioderMedEtterbetalingOgFeilutbetaling).isEmpty()
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
                        inneværendeMåned = mar25,
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
                        inneværendeMåned = mar25,
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
        fun `skal finne etterbetaling hvis forrige andel har lavere beløp`() {
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
                        kalkulertUtbetalingsbeløp = 2000,
                    ),
                )

            // Act
            val perioderMedEtterbetalingOgFeilutbetaling =
                avregningService
                    .etterbetalingerOgFeilutbetalinger(
                        andelerInneværendeBehandling = andelerInneværendeBehandling,
                        andelerForrigeBehandling = andelerForrigeBehandling,
                        inneværendeMåned = mar25,
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

            assertThat(perioderMedEtterbetalingOgFeilutbetaling.single()).isEqualTo(forventet)
        }

        @Test
        fun `skal finne feilutbetaling hvis forrige andel har høyere beløp`() {
            // Arrange
            val andelerForrigeBehandling =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan25,
                        tom = jan25,
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 2000,
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
                        inneværendeMåned = mar25,
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

            assertThat(perioderMedEtterbetalingOgFeilutbetaling.single()).isEqualTo(forventet)
        }

        @Test
        fun `skal returnere tom liste hvis alle beløp er like store`() {
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
                        inneværendeMåned = mar25,
                    )

            // Assert
            assertThat(perioderMedEtterbetalingOgFeilutbetaling).isEmpty()
        }

        @Test
        fun `skal bare inkludere perioder tom forrige måned`() {
            // Arrange
            val andelerForrigeBehandling =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan25,
                        tom = apr(2025),
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                )

            val andelerInneværendeBehandling =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan25,
                        tom = apr(2025),
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 2000,
                    ),
                )

            // Act
            val perioderMedEtterbetalingOgFeilutbetaling =
                avregningService
                    .etterbetalingerOgFeilutbetalinger(
                        andelerInneværendeBehandling = andelerInneværendeBehandling,
                        andelerForrigeBehandling = andelerForrigeBehandling,
                        inneværendeMåned = mar25,
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
                    tom = 28.feb(2025),
                )

            assertThat(perioderMedEtterbetalingOgFeilutbetaling.single()).isEqualTo(forventet)
        }
    }

    @Nested
    inner class `etterbetalingerOgFeilutbetalinger - to barn` {
        @Test
        fun `skal returnere tom liste hvis alle beløp er like store for flere barn`() {
            // Arrange
            val andelerForrigeBehandlingBarn1 =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan25,
                        tom = jan25,
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                )

            val andelerInneværendeBehandlingBarn1 =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan25,
                        tom = jan25,
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                )

            val andelerForrigeBehandlingBarn2 =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan25,
                        tom = jan25,
                        person = barn2,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                )

            val andelerInneværendeBehandlingBarn2 =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan25,
                        tom = jan25,
                        person = barn2,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                )

            val andelerInneværendeBehandling = andelerInneværendeBehandlingBarn1 + andelerInneværendeBehandlingBarn2
            val andelerForrigeBehandling = andelerForrigeBehandlingBarn1 + andelerForrigeBehandlingBarn2

            // Act
            val perioderMedEtterbetalingOgFeilutbetaling =
                avregningService
                    .etterbetalingerOgFeilutbetalinger(
                        andelerInneværendeBehandling = andelerInneværendeBehandling,
                        andelerForrigeBehandling = andelerForrigeBehandling,
                        inneværendeMåned = mar25,
                    )

            // Assert
            assertThat(perioderMedEtterbetalingOgFeilutbetaling).isEmpty()
        }

        @Test
        fun `skal summere feilutbetaling for flere barn`() {
            // Arrange
            val andelerForrigeBehandlingBarn1 =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan25,
                        tom = jan25,
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 2000,
                    ),
                )

            val andelerInneværendeBehandlingBarn1 =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan25,
                        tom = jan25,
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                )

            val andelerForrigeBehandlingBarn2 =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan25,
                        tom = jan25,
                        person = barn2,
                        kalkulertUtbetalingsbeløp = 2000,
                    ),
                )

            val andelerInneværendeBehandlingBarn2 =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan25,
                        tom = jan25,
                        person = barn2,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                )

            val andelerInneværendeBehandling = andelerInneværendeBehandlingBarn1 + andelerInneværendeBehandlingBarn2
            val andelerForrigeBehandling = andelerForrigeBehandlingBarn1 + andelerForrigeBehandlingBarn2

            // Act
            val perioderMedEtterbetalingOgFeilutbetaling =
                avregningService
                    .etterbetalingerOgFeilutbetalinger(
                        andelerInneværendeBehandling = andelerInneværendeBehandling,
                        andelerForrigeBehandling = andelerForrigeBehandling,
                        inneværendeMåned = mar25,
                    )

            // Assert
            val forventet =
                Periode(
                    verdi =
                        EtterbetalingOgFeilutbetaling(
                            etterbetaling = 0,
                            feilutbetaling = 2000,
                        ),
                    fom = 1.jan(2025),
                    tom = 31.jan(2025),
                )

            assertThat(perioderMedEtterbetalingOgFeilutbetaling.single()).isEqualTo(forventet)
        }

        @Test
        fun `skal summere etterbetaling for flere barn`() {
            // Arrange
            val andelerForrigeBehandlingBarn1 =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan25,
                        tom = jan25,
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                )

            val andelerInneværendeBehandlingBarn1 =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan25,
                        tom = jan25,
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 2000,
                    ),
                )

            val andelerForrigeBehandlingBarn2 =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan25,
                        tom = jan25,
                        person = barn2,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                )

            val andelerInneværendeBehandlingBarn2 =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan25,
                        tom = jan25,
                        person = barn2,
                        kalkulertUtbetalingsbeløp = 2000,
                    ),
                )

            val andelerInneværendeBehandling = andelerInneværendeBehandlingBarn1 + andelerInneværendeBehandlingBarn2
            val andelerForrigeBehandling = andelerForrigeBehandlingBarn1 + andelerForrigeBehandlingBarn2

            // Act
            val perioderMedEtterbetalingOgFeilutbetaling =
                avregningService
                    .etterbetalingerOgFeilutbetalinger(
                        andelerInneværendeBehandling = andelerInneværendeBehandling,
                        andelerForrigeBehandling = andelerForrigeBehandling,
                        inneværendeMåned = mar25,
                    )

            // Assert
            val forventet =
                Periode(
                    verdi =
                        EtterbetalingOgFeilutbetaling(
                            etterbetaling = 2000,
                            feilutbetaling = 0,
                        ),
                    fom = 1.jan(2025),
                    tom = 31.jan(2025),
                )

            assertThat(perioderMedEtterbetalingOgFeilutbetaling.single()).isEqualTo(forventet)
        }

        @Test
        fun `skal regne feilutbetaling og etterbetaling for forskjellige barn i samme periode`() {
            // Arrange
            val andelerForrigeBehandlingBarn1 =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan25,
                        tom = jan25,
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                )

            val andelerInneværendeBehandlingBarn1 =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan25,
                        tom = jan25,
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 2000,
                    ),
                )

            val andelerForrigeBehandlingBarn2 =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan25,
                        tom = jan25,
                        person = barn2,
                        kalkulertUtbetalingsbeløp = 2000,
                    ),
                )

            val andelerInneværendeBehandlingBarn2 =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan25,
                        tom = jan25,
                        person = barn2,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                )

            val andelerInneværendeBehandling = andelerInneværendeBehandlingBarn1 + andelerInneværendeBehandlingBarn2
            val andelerForrigeBehandling = andelerForrigeBehandlingBarn1 + andelerForrigeBehandlingBarn2

            // Act
            val perioderMedEtterbetalingOgFeilutbetaling =
                avregningService
                    .etterbetalingerOgFeilutbetalinger(
                        andelerInneværendeBehandling = andelerInneværendeBehandling,
                        andelerForrigeBehandling = andelerForrigeBehandling,
                        inneværendeMåned = mar25,
                    )

            // Assert
            val forventet =
                Periode(
                    verdi =
                        EtterbetalingOgFeilutbetaling(
                            etterbetaling = 1000,
                            feilutbetaling = 1000,
                        ),
                    fom = 1.jan(2025),
                    tom = 31.jan(2025),
                )

            assertThat(perioderMedEtterbetalingOgFeilutbetaling.single()).isEqualTo(forventet)
        }

        @Test
        fun `skal regne feilutbetaling og etterbetaling for forskjellige barn i forskjellige perioder`() {
            // Arrange
            val andelerForrigeBehandlingBarn1 =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = juli24,
                        tom = juli24,
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                )

            val andelerInneværendeBehandlingBarn1 =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = juli24,
                        tom = juli24,
                        person = barn1,
                        kalkulertUtbetalingsbeløp = 2000,
                    ),
                )

            val andelerForrigeBehandlingBarn2 =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan25,
                        tom = jan25,
                        person = barn2,
                        kalkulertUtbetalingsbeløp = 2000,
                    ),
                )

            val andelerInneværendeBehandlingBarn2 =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan25,
                        tom = jan25,
                        person = barn2,
                        kalkulertUtbetalingsbeløp = 1000,
                    ),
                )

            val andelerInneværendeBehandling = andelerInneværendeBehandlingBarn1 + andelerInneværendeBehandlingBarn2
            val andelerForrigeBehandling = andelerForrigeBehandlingBarn1 + andelerForrigeBehandlingBarn2

            // Act
            val perioderMedEtterbetalingOgFeilutbetaling =
                avregningService
                    .etterbetalingerOgFeilutbetalinger(
                        andelerInneværendeBehandling = andelerInneværendeBehandling,
                        andelerForrigeBehandling = andelerForrigeBehandling,
                        inneværendeMåned = mar25,
                    )

            // Assert
            val forventet =
                listOf(
                    Periode(
                        verdi =
                            EtterbetalingOgFeilutbetaling(
                                etterbetaling = 1000,
                                feilutbetaling = 0,
                            ),
                        fom = 1.jul(2024),
                        tom = 31.jul(2024),
                    ),
                    Periode(
                        verdi =
                            EtterbetalingOgFeilutbetaling(
                                etterbetaling = 0,
                                feilutbetaling = 1000,
                            ),
                        fom = 1.jan(2025),
                        tom = 31.jan(2025),
                    ),
                )

            assertThat(perioderMedEtterbetalingOgFeilutbetaling).isEqualTo(forventet)
        }
    }
}
