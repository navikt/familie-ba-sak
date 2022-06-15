package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall.KVARTALSVIS
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall.MÅNEDLIG
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall.UKENTLIG
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall.ÅRLIG
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.KronerPerValutaenhet
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Valutabeløp
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.tilMånedligValutabeløp
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.times
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.MathContext
import java.math.RoundingMode
import java.time.YearMonth

class DifferanseberegningsUtilsTest {
    val utbetalingsbeløpNorge = 2000

    @Test
    fun `Skal multiplisere valutabeløp med valutakurs`() {

        val valutabeløp = 1200.i("EUR")
        val kurs = 9.731.kronerPer("EUR")

        Assertions.assertEquals(11_677.toBigDecimal(), (valutabeløp * kurs)?.round(MathContext(5)))
    }

    @Test
    fun `Skal ikke multiplisere valutabeløp med valutakurs når valuta er forskjellig`() {

        val valutabeløp = 1200.i("EUR")
        val kurs = 9.73.kronerPer("DKK")

        assertThrows<IllegalArgumentException> { valutabeløp * kurs }
    }

    @Test
    fun `Skal konvertere årlig utenlandsk periodebeløp til månedlig`() {

        val månedligValutabeløp = 1200.i("EUR").somUtenlandskPeriodebeløp(ÅRLIG)
            .tilMånedligValutabeløp()

        Assertions.assertEquals(100.i("EUR"), månedligValutabeløp)
    }

    @Test
    fun `Skal konvertere kvartalsvis utenlandsk periodebeløp til månedlig`() {

        val månedligValutabeløp = 300.i("EUR").somUtenlandskPeriodebeløp(KVARTALSVIS)
            .tilMånedligValutabeløp()

        Assertions.assertEquals(100.i("EUR"), månedligValutabeløp)
    }

    @Test
    fun `Månedlig utenlandsk periodebeløp skal ikke endres`() {

        val månedligValutabeløp = 100.i("EUR").somUtenlandskPeriodebeløp(MÅNEDLIG)
            .tilMånedligValutabeløp()

        Assertions.assertEquals(100.i("EUR"), månedligValutabeløp)
    }

    @Test
    fun `Skal konvertere ukentlig utenlandsk periodebeløp til månedlig`() {

        val månedligValutabeløp = 25.i("EUR").somUtenlandskPeriodebeløp(UKENTLIG)
            .tilMånedligValutabeløp()?.rundNed(5)

        Assertions.assertEquals(108.75.i("EUR"), månedligValutabeløp)
    }

    @Test
    fun `Skal ha presisjon i kronekonverteringen til norske kroner`() {
        val månedligValutabeløp = 0.0123767453453.i("EUR").somUtenlandskPeriodebeløp(ÅRLIG)
            .tilMånedligValutabeløp()?.rundNed(10)

        Assertions.assertEquals(0.001031395445.i("EUR"), månedligValutabeløp)
    }

    @Test
    fun `Skal håndtere gjentakende endring og differanseberegning på andel tilkjent ytelse`() {
        val aty1 = differanseBeregnUtbetalingsbeløp(
            lagAndelTilkjentYtelse(beløp = 50),
            100.toBigDecimal()
        )

        Assertions.assertEquals(0, aty1?.kalkulertUtbetalingsbeløp)
        Assertions.assertEquals(-50, aty1?.differanseberegnetPeriodebeløp)
        Assertions.assertEquals(50, aty1?.nasjonaltPeriodebeløp)

        val aty2 = differanseBeregnUtbetalingsbeløp(
            aty1?.copy(kalkulertUtbetalingsbeløp = 1),
            75.toBigDecimal()
        )

        Assertions.assertEquals(0, aty2?.kalkulertUtbetalingsbeløp)
        Assertions.assertEquals(-74, aty2?.differanseberegnetPeriodebeløp)
        Assertions.assertEquals(1, aty2?.nasjonaltPeriodebeløp)

        val aty3 = differanseBeregnUtbetalingsbeløp(
            aty2?.copy(kalkulertUtbetalingsbeløp = 250),
            75.toBigDecimal()
        )

        Assertions.assertEquals(175, aty3?.kalkulertUtbetalingsbeløp)
        Assertions.assertEquals(175, aty3?.differanseberegnetPeriodebeløp)
        Assertions.assertEquals(250, aty3?.nasjonaltPeriodebeløp)
    }

    @Test
    fun `Skal fjerne desimaler i utenlandskperiodebeløp, effektivt øke den norske ytelsen med inntil én krone`() {
        val aty1 = differanseBeregnUtbetalingsbeløp(
            lagAndelTilkjentYtelse(beløp = 50),
            100.987654.toBigDecimal()
        ) // Blir til rundet til 100

        Assertions.assertEquals(0, aty1?.kalkulertUtbetalingsbeløp)
        Assertions.assertEquals(-50, aty1?.differanseberegnetPeriodebeløp)
        Assertions.assertEquals(50, aty1?.nasjonaltPeriodebeløp)
    }
}

fun lagAndelTilkjentYtelse(beløp: Int) = lagAndelTilkjentYtelse(
    fom = YearMonth.now(),
    tom = YearMonth.now().plusYears(1),
    beløp = beløp
)

fun Double.kronerPer(valuta: String) = KronerPerValutaenhet(
    valutakode = valuta,
    kronerPerValutaenhet = this.toBigDecimal()
)

fun Double.i(valuta: String) = Valutabeløp(this.toBigDecimal(), valuta)
fun Int.i(valuta: String) = Valutabeløp(this.toBigDecimal(), valuta)

fun Valutabeløp.somUtenlandskPeriodebeløp(intervall: Intervall): UtenlandskPeriodebeløp =
    UtenlandskPeriodebeløp(
        fom = null,
        tom = null,
        beløp = this.beløp,
        valutakode = this.valutakode,
        intervall = intervall,
        kalkulertMånedligBeløp = intervall.konverterBeløpTilMånedlig(this.beløp)
    )

fun Valutabeløp.rundNed(presisjon: Int) =
    Valutabeløp(this.beløp.round(MathContext(presisjon, RoundingMode.DOWN)), this.valutakode)
