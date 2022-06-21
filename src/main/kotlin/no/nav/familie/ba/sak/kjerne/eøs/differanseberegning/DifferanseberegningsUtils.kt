package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

fun Intervall.konverterBeløpTilMånedlig(beløp: BigDecimal): BigDecimal =
    when (this) {
        Intervall.ÅRLIG -> beløp.divide(12.toBigDecimal(), 10, RoundingMode.HALF_UP)
        Intervall.KVARTALSVIS -> beløp.divide(3.toBigDecimal(), 10, RoundingMode.HALF_UP)
        Intervall.MÅNEDLIG -> beløp
        Intervall.UKENTLIG -> beløp.multiply(4.35.toBigDecimal(), MathContext(10, RoundingMode.HALF_UP))
    }.stripTrailingZeros().toPlainString().toBigDecimal()

/**
 * Kalkulerer nytt utbetalingsbeløp fra [utenlandskPeriodebeløpINorskeKroner]
 * Beløpet konverteres fra desimaltall til heltall ved å strippe desimalene, og dermed øke den norske ytelsen med inntil én krone
 * Må håndtere tilfellet der [kalkulertUtebetalngsbeløp] blir modifisert andre steder i koden, men antar at det aldri vil være negativt
 * [nasjonaltPeriodebeløp] settes til den originale, nasjonale beregningen (aldri negativt)
 * [differanseberegnetBeløp] er differansen mellom [nasjonaltPeriodebeløp] og (avrundet) [utenlandskPeriodebeløpINorskeKroner] (kan bli negativt)
 * [kalkulertUtebetalngsbeløp] blir satt til [differanseberegnetBeløp], med mindre det er negativt. Da blir det 0 (null)
 * Hvis [utenlandskPeriodebeløpINorskeKroner] er <null>, så skal utbetalingsbeløpet reverteres til det originale nasjonale beløpet
 */
fun AndelTilkjentYtelse?.oppdaterDifferanseberegning(
    utenlandskPeriodebeløpINorskeKroner: BigDecimal?
): AndelTilkjentYtelse? {
    val nyAndelTilkjentYtelse = when {
        this == null -> null
        utenlandskPeriodebeløpINorskeKroner == null -> this.utenDifferanseberegning()
        else -> this.medDifferanseberegning(utenlandskPeriodebeløpINorskeKroner)
    }

    return nyAndelTilkjentYtelse
}

private fun AndelTilkjentYtelse.medDifferanseberegning(
    utenlandskPeriodebeløpINorskeKroner: BigDecimal
): AndelTilkjentYtelse {

    val avrundetUtenlandskPeriodebeløp = utenlandskPeriodebeløpINorskeKroner
        .toBigInteger().intValueExact() // Fjern desimaler for å gi fordel til søker

    val nyttDifferanseberegnetBeløp = nasjonaltPeriodebeløp - avrundetUtenlandskPeriodebeløp

    return copy(
        id = 0,
        kalkulertUtbetalingsbeløp = maxOf(nyttDifferanseberegnetBeløp, 0),
        differanseberegnetPeriodebeløp = nyttDifferanseberegnetBeløp
    )
}

private fun AndelTilkjentYtelse.utenDifferanseberegning(): AndelTilkjentYtelse {
    return copy(
        id = 0,
        kalkulertUtbetalingsbeløp = nasjonaltPeriodebeløp,
        differanseberegnetPeriodebeløp = null
    )
}
