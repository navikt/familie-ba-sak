package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class DifferanseberegningsUtilsTest {

    @Test
    fun `Skal gi riktig utenlandsk beløp for årlig utbetaling`() {

        val utenlandskSatsÅrlig = 1200

        Assertions.assertEquals(
            1000,
            beregnUtbetalingsbeløpUtlandINok(
                satsUtland = utenlandskSatsÅrlig,
                kurs = 10.0,
                intervall = Intervall.ÅRLIG,
                erSkuddår = false
            )
        )
    }

    @Test
    fun `Skal gi riktig utenlandsk beløp for kvartalvis utbetaling`() {

        val utenlandskSatsKvartalsvis = 400

        Assertions.assertEquals(
            1000,
            beregnUtbetalingsbeløpUtlandINok(
                satsUtland = utenlandskSatsKvartalsvis,
                kurs = 10.0,
                intervall = Intervall.KVARTALSVIS,
                erSkuddår = false
            )
        )
    }

    @Test
    fun `Skal gi riktig utenlandsk beløp for månedlig utbetaling`() {

        val utenlandskSatsMåendlig = 100

        Assertions.assertEquals(
            1000,
            beregnUtbetalingsbeløpUtlandINok(
                satsUtland = utenlandskSatsMåendlig,
                kurs = 10.0,
                intervall = Intervall.MÅNEDLIG,
                erSkuddår = false
            )
        )
    }

    @Test
    fun `Skal gi riktig utenlandsk beløp for ukentlig utbetaling`() {

        val utenlandskSatsUkentlig = 25

        Assertions.assertEquals(
            1086,
            beregnUtbetalingsbeløpUtlandINok(
                satsUtland = utenlandskSatsUkentlig,
                kurs = 10.0,
                intervall = Intervall.UKENTLIG,
                erSkuddår = false
            )
        )
    }

    @Test
    fun `Skal gi riktig utenlandsk beløp for ukentlig utbetaling ved skuddår`() {

        val utenlandskSatsUkentlig = 25

        Assertions.assertEquals(
            1089,
            beregnUtbetalingsbeløpUtlandINok(
                satsUtland = utenlandskSatsUkentlig,
                kurs = 10.0,
                intervall = Intervall.UKENTLIG,
                erSkuddår = true
            )
        )
    }

    @Test
    fun `Skal gi null dersom utenandsk beløp er større en det norske når det ikke er småbarnstilleg eller utvidet`() {

        Assertions.assertEquals(
            0,
            beregnDifferanseOrdinær(utbetalingsbeløpNorge = 1000, utbetalingsbeløpUtlandINok = 2000)
        )
    }
}
