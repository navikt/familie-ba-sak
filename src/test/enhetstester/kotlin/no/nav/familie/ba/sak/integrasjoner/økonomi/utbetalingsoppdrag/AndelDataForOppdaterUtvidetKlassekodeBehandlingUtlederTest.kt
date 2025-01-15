package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import no.nav.familie.ba.sak.TestClockProvider
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.YearMonth

class AndelDataForOppdaterUtvidetKlassekodeBehandlingUtlederTest {
    private val clockProvider = TestClockProvider.lagClockProviderMedFastTidspunkt(YearMonth.of(2024, 11))
    private val andelDataForOppdaterUtvidetKlassekodeBehandlingUtleder = AndelDataForOppdaterUtvidetKlassekodeBehandlingUtleder(clockProvider = clockProvider)

    @Nested
    inner class FinnForrigeAndelerForOppdaterUtvidetKlassekodeBehandling {
        @Test
        fun `skal returnere alle utvidet andeler før nåtidspunkt dersom nåtidspunkt ikke treffer noen utvidet andel samt alle øvrige andeler`() {
            // Arrange
            val tilkjentYtelse = lagTilkjentYtelse()
            val denneMåned = YearMonth.now(clockProvider.get())
            val utvidetAndel =
                lagAndelTilkjentYtelse(
                    fom = denneMåned.minusMonths(3),
                    tom = denneMåned.minusMonths(1),
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                )
            val ordinærAndel =
                lagAndelTilkjentYtelse(
                    fom = denneMåned.minusMonths(3),
                    tom = denneMåned.minusMonths(1),
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                )
            tilkjentYtelse.andelerTilkjentYtelse.addAll(
                listOf(
                    utvidetAndel,
                    ordinærAndel,
                ),
            )

            // Act
            val forrigeAndeler = andelDataForOppdaterUtvidetKlassekodeBehandlingUtleder.finnForrigeAndelerForOppdaterUtvidetKlassekodeBehandling(tilkjentYtelse, true)

            // Assert
            assertThat(forrigeAndeler).hasSize(2)
            val utvidetAndelData = forrigeAndeler.single { it.type == YtelsetypeBA.UTVIDET_BARNETRYGD }
            assertThat(utvidetAndelData.fom).isEqualTo(utvidetAndel.stønadFom)
            assertThat(utvidetAndelData.tom).isEqualTo(utvidetAndel.stønadTom)
            assertThat(utvidetAndelData.id).isEqualTo(utvidetAndel.id)
            assertThat(utvidetAndelData.beløp).isEqualTo(utvidetAndel.kalkulertUtbetalingsbeløp)
            assertThat(utvidetAndelData.type).isEqualTo(YtelsetypeBA.UTVIDET_BARNETRYGD)
        }

        @Test
        fun `skal splitte utvidet andel dersom nåtidspunkt treffer andelen samt fjerne alle utvidet andeler etter nåtidspunkt`() {
            // Arrange
            val tilkjentYtelse = lagTilkjentYtelse()
            val denneMåned = YearMonth.now(clockProvider.get())
            val utvidetAndel =
                lagAndelTilkjentYtelse(
                    fom = denneMåned.minusMonths(3),
                    tom = denneMåned.plusMonths(3),
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                )
            val ordinærAndel =
                lagAndelTilkjentYtelse(
                    fom = denneMåned.minusMonths(3),
                    tom = denneMåned.minusMonths(1),
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                )
            tilkjentYtelse.andelerTilkjentYtelse.addAll(
                listOf(
                    utvidetAndel,
                    ordinærAndel,
                ),
            )

            // Act
            val forrigeAndeler = andelDataForOppdaterUtvidetKlassekodeBehandlingUtleder.finnForrigeAndelerForOppdaterUtvidetKlassekodeBehandling(tilkjentYtelse, true)

            // Assert
            assertThat(forrigeAndeler).hasSize(2)
            val utvidetAndelData = forrigeAndeler.single { it.type == YtelsetypeBA.UTVIDET_BARNETRYGD }
            assertThat(utvidetAndelData.fom).isEqualTo(utvidetAndel.stønadFom)
            assertThat(utvidetAndelData.tom).isEqualTo(denneMåned)
            assertThat(utvidetAndelData.id).isEqualTo(utvidetAndel.id)
            assertThat(utvidetAndelData.beløp).isEqualTo(utvidetAndel.kalkulertUtbetalingsbeløp)
            assertThat(utvidetAndelData.type).isEqualTo(YtelsetypeBA.UTVIDET_BARNETRYGD)
        }

        @Test
        fun `skal fjerne utvidet andel dersom nåtidspunkt treffer andelen og andelen kun inneholder 1 mnd`() {
            // Arrange
            val tilkjentYtelse = lagTilkjentYtelse()
            val denneMåned = YearMonth.now(clockProvider.get())
            val utvidetAndel =
                lagAndelTilkjentYtelse(fom = denneMåned, tom = denneMåned, ytelseType = YtelseType.UTVIDET_BARNETRYGD)
            val ordinærAndel =
                lagAndelTilkjentYtelse(
                    fom = denneMåned.minusMonths(3),
                    tom = denneMåned.minusMonths(1),
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                )
            tilkjentYtelse.andelerTilkjentYtelse.addAll(
                listOf(
                    utvidetAndel,
                    ordinærAndel,
                ),
            )

            // Act
            val forrigeAndeler = andelDataForOppdaterUtvidetKlassekodeBehandlingUtleder.finnForrigeAndelerForOppdaterUtvidetKlassekodeBehandling(tilkjentYtelse, true)

            // Assert
            assertThat(forrigeAndeler).hasSize(1)
            assertThat(forrigeAndeler.none { it.type == YtelsetypeBA.UTVIDET_BARNETRYGD }).isTrue
        }

        @Test
        fun `skal returnere tom liste dersom det ikke finnes noen forrige tilkjent ytelse`() {
            // Arrange
            val tilkjentYtelse = lagTilkjentYtelse()
            val denneMåned = YearMonth.now(clockProvider.get())
            val utvidetAndel =
                lagAndelTilkjentYtelse(
                    fom = denneMåned.minusMonths(3),
                    tom = denneMåned.plusMonths(3),
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                )
            val utvidetAndelEtterNåtidspunkt =
                lagAndelTilkjentYtelse(
                    fom = denneMåned.plusMonths(4),
                    tom = denneMåned.plusMonths(10),
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                )
            val ordinærAndel =
                lagAndelTilkjentYtelse(
                    fom = denneMåned.minusMonths(3),
                    tom = denneMåned.minusMonths(1),
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                )
            tilkjentYtelse.andelerTilkjentYtelse.addAll(
                listOf(
                    utvidetAndel,
                    utvidetAndelEtterNåtidspunkt,
                    ordinærAndel,
                ),
            )

            // Act
            val forrigeAndeler = andelDataForOppdaterUtvidetKlassekodeBehandlingUtleder.finnForrigeAndelerForOppdaterUtvidetKlassekodeBehandling(tilkjentYtelse, true)

            // Assert
            assertThat(forrigeAndeler).hasSize(2)
            val utvidetAndelData = forrigeAndeler.single { it.type == YtelsetypeBA.UTVIDET_BARNETRYGD }
            assertThat(utvidetAndelData.fom).isEqualTo(utvidetAndel.stønadFom)
            assertThat(utvidetAndelData.tom).isEqualTo(denneMåned)
            assertThat(utvidetAndelData.id).isEqualTo(utvidetAndel.id)
            assertThat(utvidetAndelData.beløp).isEqualTo(utvidetAndel.kalkulertUtbetalingsbeløp)
            assertThat(utvidetAndelData.type).isEqualTo(YtelsetypeBA.UTVIDET_BARNETRYGD)
        }
    }
}
