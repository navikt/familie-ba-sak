package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

fun Intervall.konverterBeløpTilMånedlig(beløp: BigDecimal): BigDecimal {
    val konvertertBeløp = when (this) {
        Intervall.ÅRLIG -> beløp.divide(12.toBigDecimal(), 10, RoundingMode.HALF_DOWN)
        Intervall.KVARTALSVIS -> beløp.divide(3.toBigDecimal(), 10, RoundingMode.HALF_DOWN)
        Intervall.MÅNEDLIG -> beløp
        Intervall.UKENTLIG -> beløp.multiply(4.35.toBigDecimal(), MathContext(10, RoundingMode.HALF_DOWN))
    }
    return try {
        konvertertBeløp.setScale(4)
    } catch (e: ArithmeticException) {
        konvertertBeløp.setScale(4, RoundingMode.HALF_DOWN)
    }.stripTrailingZeros().toPlainString().toBigDecimal()
}

/**
 * Kalkulerer nytt utbetalingsbeløp fra [utenlandskPeriodebeløpINorskeKroner]
 * Beløpet konverteres fra desimaltall til heltall ved å strippe desimalene, og dermed øke den norske ytelsen med inntil én krone
 * Må håndtere tilfellet der [kalkulertUtebetalngsbeløp] blir modifisert andre steder i koden, men antar at det aldri vil være negativt
 * [nasjonaltPeriodebeløp] settes til den originale, nasjonale beregningen (aldri negativt)
 * [differanseberegnetBeløp] er differansen mellom [nasjonaltPeriodebeløp] og (avrundet) [utenlandskPeriodebeløpINorskeKroner] (kan bli negativt)
 * [kalkulertUtebetalngsbeløp] blir satt til [differanseberegnetBeløp], med mindre det er negativt. Da blir det 0 (null)
 */
fun AndelTilkjentYtelse?.kalkulerFraUtenlandskPeriodebeløp(utenlandskPeriodebeløpINorskeKroner: BigDecimal): AndelTilkjentYtelse? {
    if (this == null)
        return null

    val nyttNasjonaltPeriodebeløp = when {
        differanseberegnetPeriodebeløp == null || nasjonaltPeriodebeløp == null -> kalkulertUtbetalingsbeløp
        differanseberegnetPeriodebeløp < 0 && kalkulertUtbetalingsbeløp > 0 -> kalkulertUtbetalingsbeløp
        differanseberegnetPeriodebeløp != kalkulertUtbetalingsbeløp -> kalkulertUtbetalingsbeløp
        else -> nasjonaltPeriodebeløp
    }

    val avrundetUtenlandskPeriodebeløp = utenlandskPeriodebeløpINorskeKroner
        .toBigInteger().intValueExact() // Fjern desimaler for å gi fordel til søker

    val nyttDifferanseberegnetBeløp = nyttNasjonaltPeriodebeløp - avrundetUtenlandskPeriodebeløp

    return copy(
        id = 0, // Lager en ny instans
        kalkulertUtbetalingsbeløp = maxOf(nyttDifferanseberegnetBeløp, 0),
        nasjonaltPeriodebeløp = nyttNasjonaltPeriodebeløp,
        differanseberegnetPeriodebeløp = nyttDifferanseberegnetBeløp
    )
}
