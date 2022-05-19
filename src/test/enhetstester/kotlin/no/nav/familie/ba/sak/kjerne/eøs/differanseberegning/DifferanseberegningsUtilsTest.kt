package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class DifferanseberegningsUtilsTest {
    val utbetalingsbeløpNorge = 2000

    @Test
    fun `Skal gi riktig differanse for årlig utbetaling ved ordinær sekundærlandssak`() {

        val utenlandskSatsÅrlig = 1200

        val utbetalingsbeløpUtlandINok = beregnUtbetalingsbeløpUtlandINok(
            satsUtland = utenlandskSatsÅrlig, kurs = 10.0, intervall = Intervall.ÅRLIG, erSkuddår = false
        )

        Assertions.assertEquals(1000, utbetalingsbeløpUtlandINok)
        Assertions.assertEquals(1000, beregnDifferanseOrdinær(utbetalingsbeløpNorge, utbetalingsbeløpUtlandINok))
    }

    @Test
    fun `Skal gi riktig differanse for kvartalvis utbetaling ved ordinær sekundærlandssak`() {

        val utenlandskSatsKvartalsvis = 400

        val utbetalingsbeløpUtlandINok = beregnUtbetalingsbeløpUtlandINok(
            satsUtland = utenlandskSatsKvartalsvis, kurs = 10.0, intervall = Intervall.KVARTALSVIS, erSkuddår = false
        )

        Assertions.assertEquals(1000, utbetalingsbeløpUtlandINok)
        Assertions.assertEquals(1000, beregnDifferanseOrdinær(utbetalingsbeløpNorge, utbetalingsbeløpUtlandINok))
    }

    @Test
    fun `Skal gi riktig differanse for månedlig utbetaling ved ordinær sekundærlandssak`() {

        val utenlandskSatsMåendlig = 100

        val utbetalingsbeløpUtlandINok = beregnUtbetalingsbeløpUtlandINok(
            satsUtland = utenlandskSatsMåendlig, kurs = 10.0, intervall = Intervall.MÅNEDLIG, erSkuddår = false
        )

        Assertions.assertEquals(1000, utbetalingsbeløpUtlandINok)
        Assertions.assertEquals(1000, beregnDifferanseOrdinær(utbetalingsbeløpNorge, utbetalingsbeløpUtlandINok))
    }

    @Test
    fun `Skal gi riktig differanse for ukentlig utbetaling ved ordinær sekundærlandssak`() {

        val utenlandskSatsUkentlig = 25

        val utbetalingsbeløpUtlandINok = beregnUtbetalingsbeløpUtlandINok(
            satsUtland = utenlandskSatsUkentlig, kurs = 10.0, intervall = Intervall.UKENTLIG, erSkuddår = false
        )

        Assertions.assertEquals(1086, utbetalingsbeløpUtlandINok)
        Assertions.assertEquals(914, beregnDifferanseOrdinær(utbetalingsbeløpNorge, utbetalingsbeløpUtlandINok))
    }

    @Test
    fun `Skal gi riktig differanse for ukentlig utbetaling ved skuddår ved ordinær sekundærlandssak`() {

        val utenlandskSatsUkentlig = 25

        val utbetalingsbeløpUtlandINok = beregnUtbetalingsbeløpUtlandINok(
            satsUtland = utenlandskSatsUkentlig, kurs = 10.0, intervall = Intervall.UKENTLIG, erSkuddår = true
        )

        Assertions.assertEquals(1089, utbetalingsbeløpUtlandINok)
        Assertions.assertEquals(911, beregnDifferanseOrdinær(utbetalingsbeløpNorge, utbetalingsbeløpUtlandINok))
    }

    @Test
    fun `Skal gi null dersom utenandsk beløp er større en det norske når det ikke er småbarnstilleg eller utvidet`() {

        Assertions.assertEquals(
            0,
            beregnDifferanseOrdinær(utbetalingsbeløpNorge = 1000, utbetalingsbeløpUtlandINok = 2000)
        )
    }
}
