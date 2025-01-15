package no.nav.familie.ba.sak.kjerne.autovedtak.oppdaterutvidetklassekode

import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OppdaterUtvidetKlassekodeUtvidetAndelerSplitterTest {
    @Test
    fun `skal splitte utvidet andel dersom nåtidspunkt treffer andelen`() {
        // Arrange
        val tilkjentYtelse = lagTilkjentYtelse()
        val denneMåned = inneværendeMåned()
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
        val forrigeAndeler = OppdaterUtvidetKlassekodeUtvidetAndelerSplitter.splittUtvidetAndelerIInneværendeMåned(tilkjentYtelse.andelerTilkjentYtelse)

        // Assert
        assertThat(forrigeAndeler).hasSize(3)
        val utvidetAndelData = forrigeAndeler.filter { it.type == YtelseType.UTVIDET_BARNETRYGD }
        assertThat(utvidetAndelData).hasSize(2)
        val førsteUtvidetAndelData = utvidetAndelData.first()
        assertThat(førsteUtvidetAndelData.stønadFom).isEqualTo(utvidetAndel.stønadFom)
        assertThat(førsteUtvidetAndelData.stønadTom).isEqualTo(denneMåned)
        assertThat(førsteUtvidetAndelData.kalkulertUtbetalingsbeløp).isEqualTo(utvidetAndel.kalkulertUtbetalingsbeløp)
        assertThat(førsteUtvidetAndelData.type).isEqualTo(YtelseType.UTVIDET_BARNETRYGD)

        val andreUtvidetAndelData = utvidetAndelData.last()
        assertThat(andreUtvidetAndelData.stønadFom).isEqualTo(denneMåned.plusMonths(1))
        assertThat(andreUtvidetAndelData.stønadTom).isEqualTo(utvidetAndel.stønadTom)
        assertThat(andreUtvidetAndelData.kalkulertUtbetalingsbeløp).isEqualTo(utvidetAndel.kalkulertUtbetalingsbeløp)
        assertThat(andreUtvidetAndelData.type).isEqualTo(YtelseType.UTVIDET_BARNETRYGD)
    }

    @Test
    fun `skal returnere alle utvidet andeler uendret dersom nåtidspunkt ikke treffer noen utvidet andeler`() {
        // Arrange
        val tilkjentYtelse = lagTilkjentYtelse()
        val denneMåned = inneværendeMåned()
        val utvidetAndelFørNåtidspunkt =
            lagAndelTilkjentYtelse(
                fom = denneMåned.minusMonths(3),
                tom = denneMåned.minusMonths(1),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
            )
        val utvidetAndelEtterNåtidspunkt =
            lagAndelTilkjentYtelse(
                fom = denneMåned.plusMonths(1),
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
                utvidetAndelFørNåtidspunkt,
                utvidetAndelEtterNåtidspunkt,
                ordinærAndel,
            ),
        )

        // Act
        val forrigeAndeler = OppdaterUtvidetKlassekodeUtvidetAndelerSplitter.splittUtvidetAndelerIInneværendeMåned(tilkjentYtelse.andelerTilkjentYtelse)

        // Assert
        assertThat(forrigeAndeler).hasSize(3)
        val utvidetAndelData = forrigeAndeler.filter { it.type == YtelseType.UTVIDET_BARNETRYGD }
        assertThat(utvidetAndelData).hasSize(2)
        val førsteUtvidetAndelData = utvidetAndelData.first()
        assertThat(førsteUtvidetAndelData.stønadFom).isEqualTo(utvidetAndelFørNåtidspunkt.stønadFom)
        assertThat(førsteUtvidetAndelData.stønadTom).isEqualTo(utvidetAndelFørNåtidspunkt.stønadTom)
        assertThat(førsteUtvidetAndelData.kalkulertUtbetalingsbeløp).isEqualTo(utvidetAndelFørNåtidspunkt.kalkulertUtbetalingsbeløp)
        assertThat(førsteUtvidetAndelData.type).isEqualTo(YtelseType.UTVIDET_BARNETRYGD)

        val andreUtvidetAndelData = utvidetAndelData.last()
        assertThat(andreUtvidetAndelData.stønadFom).isEqualTo(utvidetAndelEtterNåtidspunkt.stønadFom)
        assertThat(andreUtvidetAndelData.stønadTom).isEqualTo(utvidetAndelEtterNåtidspunkt.stønadTom)
        assertThat(andreUtvidetAndelData.kalkulertUtbetalingsbeløp).isEqualTo(utvidetAndelEtterNåtidspunkt.kalkulertUtbetalingsbeløp)
        assertThat(andreUtvidetAndelData.type).isEqualTo(YtelseType.UTVIDET_BARNETRYGD)
    }

    @Test
    fun `skal ikke splitte utvidet andel dersom nåtidspunkt treffer andelen og andelen kun inneholder 1 mnd`() {
        // Arrange
        val tilkjentYtelse = lagTilkjentYtelse()
        val denneMåned = inneværendeMåned()
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
        val forrigeAndeler = OppdaterUtvidetKlassekodeUtvidetAndelerSplitter.splittUtvidetAndelerIInneværendeMåned(tilkjentYtelse.andelerTilkjentYtelse)

        // Assert
        assertThat(forrigeAndeler).hasSize(2)
        val utvidetAndelData = forrigeAndeler.single { it.type == YtelseType.UTVIDET_BARNETRYGD }
        assertThat(utvidetAndelData.stønadFom).isEqualTo(utvidetAndel.stønadFom)
        assertThat(utvidetAndelData.stønadTom).isEqualTo(utvidetAndel.stønadTom)
        assertThat(utvidetAndelData.kalkulertUtbetalingsbeløp).isEqualTo(utvidetAndel.kalkulertUtbetalingsbeløp)
        assertThat(utvidetAndelData.type).isEqualTo(YtelseType.UTVIDET_BARNETRYGD)
    }
}
