package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import java.math.BigDecimal

fun Intervall.konverterBeløpTilMånedlig(beløp: BigDecimal) = when (this) {
    Intervall.ÅRLIG -> beløp / 12.toBigDecimal()
    Intervall.KVARTALSVIS -> beløp / 3.toBigDecimal()
    Intervall.MÅNEDLIG -> beløp
    Intervall.UKENTLIG -> beløp.multiply(4.35.toBigDecimal())
}

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
        id = 0,
        kalkulertUtbetalingsbeløp = maxOf(nyttDifferanseberegnetBeløp, 0),
        nasjonaltPeriodebeløp = nyttNasjonaltPeriodebeløp,
        differanseberegnetPeriodebeløp = nyttDifferanseberegnetBeløp
    )
}

private fun AndelTilkjentYtelse.utenDifferanseberegning(): AndelTilkjentYtelse {
    return copy(
        id = 0,
        kalkulertUtbetalingsbeløp = nasjonaltPeriodebeløp ?: kalkulertUtbetalingsbeløp,
        nasjonaltPeriodebeløp = null,
        differanseberegnetPeriodebeløp = null
    )
}
