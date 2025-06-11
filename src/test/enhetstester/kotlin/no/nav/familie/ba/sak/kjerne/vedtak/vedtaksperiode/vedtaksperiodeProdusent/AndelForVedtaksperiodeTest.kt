package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent

import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class AndelForVedtaksperiodeTest {
    @Test
    fun `equals blir true for like objekter`() {
        val andel1 =
            AndelForVedtaksperiode(
                kalkulertUtbetalingsbeløp = 1000,
                nasjonaltPeriodebeløp = 2000,
                differanseberegnetPeriodebeløp = 1000,
                type = YtelseType.ORDINÆR_BARNETRYGD,
                prosent = BigDecimal("50.0"),
                sats = 100,
            )

        val andel2 =
            AndelForVedtaksperiode(
                kalkulertUtbetalingsbeløp = 1000,
                nasjonaltPeriodebeløp = 2000,
                differanseberegnetPeriodebeløp = 1000,
                type = YtelseType.ORDINÆR_BARNETRYGD,
                prosent = BigDecimal("50.0"),
                sats = 100,
            )

        assertThat(andel1).isEqualTo(andel2)
        assertThat(andel1.hashCode()).isEqualTo(andel2.hashCode())
    }

    @Test
    fun `equals skal bli false for andeler med forskjellig type`() {
        val andel1 =
            AndelForVedtaksperiode(
                kalkulertUtbetalingsbeløp = 1000,
                nasjonaltPeriodebeløp = 2000,
                differanseberegnetPeriodebeløp = 1000,
                type = YtelseType.ORDINÆR_BARNETRYGD,
                prosent = BigDecimal("50.0"),
                sats = 100,
            )

        val andel2 =
            AndelForVedtaksperiode(
                kalkulertUtbetalingsbeløp = 1000,
                nasjonaltPeriodebeløp = 2000,
                differanseberegnetPeriodebeløp = 1000,
                type = YtelseType.SMÅBARNSTILLEGG,
                prosent = BigDecimal("50.0"),
                sats = 100,
            )

        assertThat(andel1).isNotEqualTo(andel2)
    }

    @Test
    fun `equals skal bli false for andeler med forskjellig prosent`() {
        val andel1 =
            AndelForVedtaksperiode(
                kalkulertUtbetalingsbeløp = 1000,
                nasjonaltPeriodebeløp = 2000,
                differanseberegnetPeriodebeløp = 1000,
                type = YtelseType.ORDINÆR_BARNETRYGD,
                prosent = BigDecimal("50.0"),
                sats = 100,
            )

        val andel2 =
            AndelForVedtaksperiode(
                kalkulertUtbetalingsbeløp = 1000,
                nasjonaltPeriodebeløp = 2000,
                differanseberegnetPeriodebeløp = 1000,
                type = YtelseType.ORDINÆR_BARNETRYGD,
                prosent = BigDecimal("60.0"),
                sats = 100,
            )

        assertThat(andel1).isNotEqualTo(andel2)
    }

    @Test
    fun `equals skal bli true for nullutbetalinger når satsen endrer seg og differanseberegnet periodebeløp er større enn 0`() {
        val andel1 =
            AndelForVedtaksperiode(
                kalkulertUtbetalingsbeløp = 0,
                nasjonaltPeriodebeløp = 2000,
                differanseberegnetPeriodebeløp = 1000,
                type = YtelseType.ORDINÆR_BARNETRYGD,
                prosent = BigDecimal("50.0"),
                sats = 100,
            )

        val andel2 =
            AndelForVedtaksperiode(
                kalkulertUtbetalingsbeløp = 0,
                nasjonaltPeriodebeløp = 2000,
                differanseberegnetPeriodebeløp = 1000,
                type = YtelseType.ORDINÆR_BARNETRYGD,
                prosent = BigDecimal("50.0"),
                sats = 200,
            )

        assertThat(andel1).isEqualTo(andel2)
        assertThat(andel1.hashCode()).isEqualTo(andel2.hashCode())
    }

    @Test
    fun `equals skal bli false for nullutbetalinger når satsen endrer seg og differanseberegnet periodebeløp er mindre enn 0`() {
        val andel1 =
            AndelForVedtaksperiode(
                kalkulertUtbetalingsbeløp = 0,
                nasjonaltPeriodebeløp = 2000,
                differanseberegnetPeriodebeløp = -200,
                type = YtelseType.ORDINÆR_BARNETRYGD,
                prosent = BigDecimal("50.0"),
                sats = 100,
            )

        val andel2 =
            AndelForVedtaksperiode(
                kalkulertUtbetalingsbeløp = 0,
                nasjonaltPeriodebeløp = 2000,
                differanseberegnetPeriodebeløp = -100,
                type = YtelseType.ORDINÆR_BARNETRYGD,
                prosent = BigDecimal("50.0"),
                sats = 200,
            )

        assertThat(andel1).isNotEqualTo(andel2)
    }

    @Test
    fun `equals skal bli false dersom ene kalkulerte utbetalingsbeløpet er null, og det andre ikke er null`() {
        val andel1 =
            AndelForVedtaksperiode(
                kalkulertUtbetalingsbeløp = 0,
                nasjonaltPeriodebeløp = 2000,
                differanseberegnetPeriodebeløp = 1000,
                type = YtelseType.ORDINÆR_BARNETRYGD,
                prosent = BigDecimal("50.0"),
                sats = 100,
            )

        val andel2 =
            AndelForVedtaksperiode(
                kalkulertUtbetalingsbeløp = 1000,
                nasjonaltPeriodebeløp = 2000,
                differanseberegnetPeriodebeløp = 1000,
                type = YtelseType.ORDINÆR_BARNETRYGD,
                prosent = BigDecimal("50.0"),
                sats = 100,
            )

        assertThat(andel1).isNotEqualTo(andel2)
    }

    @Test
    fun `equals skal bli false dersom ingen kalkulerte utbetalingsbeløp er null og satsene er ulike`() {
        val andel1 =
            AndelForVedtaksperiode(
                kalkulertUtbetalingsbeløp = 1000,
                nasjonaltPeriodebeløp = 2000,
                differanseberegnetPeriodebeløp = 1000,
                type = YtelseType.ORDINÆR_BARNETRYGD,
                prosent = BigDecimal("50.0"),
                sats = 100,
            )

        val andel2 =
            AndelForVedtaksperiode(
                kalkulertUtbetalingsbeløp = 1000,
                nasjonaltPeriodebeløp = 2000,
                differanseberegnetPeriodebeløp = 1000,
                type = YtelseType.ORDINÆR_BARNETRYGD,
                prosent = BigDecimal("50.0"),
                sats = 200,
            )

        assertThat(andel1).isNotEqualTo(andel2)
    }

    @Test
    fun `equals skal bli true dersom ingen kalkulerte utbetalingsbeløp er null og satsene er like`() {
        val andel1 =
            AndelForVedtaksperiode(
                kalkulertUtbetalingsbeløp = 2000,
                nasjonaltPeriodebeløp = 2000,
                differanseberegnetPeriodebeløp = 1000,
                type = YtelseType.ORDINÆR_BARNETRYGD,
                prosent = BigDecimal("50.0"),
                sats = 100,
            )

        val andel2 =
            AndelForVedtaksperiode(
                kalkulertUtbetalingsbeløp = 1000,
                nasjonaltPeriodebeløp = 2000,
                differanseberegnetPeriodebeløp = 1000,
                type = YtelseType.ORDINÆR_BARNETRYGD,
                prosent = BigDecimal("50.0"),
                sats = 100,
            )

        assertThat(andel1).isEqualTo(andel2)
        assertThat(andel1.hashCode()).isEqualTo(andel2.hashCode())
    }
}
