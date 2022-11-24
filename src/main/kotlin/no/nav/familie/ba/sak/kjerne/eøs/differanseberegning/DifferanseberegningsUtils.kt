package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.common.del
import no.nav.familie.ba.sak.common.multipliser
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.mapIkkeNull
import java.math.BigDecimal

fun Intervall.konverterBeløpTilMånedlig(beløp: BigDecimal): BigDecimal =
    when (this) {
        Intervall.ÅRLIG -> beløp.del(12.toBigDecimal(), 10)
        Intervall.KVARTALSVIS -> beløp.del(3.toBigDecimal(), 10)
        Intervall.MÅNEDLIG -> beløp
        Intervall.UKENTLIG -> beløp.multipliser(4.35.toBigDecimal(), 10)
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

fun AndelTilkjentYtelse.medDifferanseberegning(
    utenlandskPeriodebeløpINorskeKroner: BigDecimal
): AndelTilkjentYtelse {
    val avrundetUtenlandskPeriodebeløp = utenlandskPeriodebeløpINorskeKroner
        .toBigInteger().intValueExact() // Fjern desimaler for å gi fordel til søker

    val nyttDifferanseberegnetBeløp = (
        nasjonaltPeriodebeløp
            ?: kalkulertUtbetalingsbeløp
        ) - avrundetUtenlandskPeriodebeløp

    return copy(
        id = 0,
        kalkulertUtbetalingsbeløp = maxOf(nyttDifferanseberegnetBeløp, 0),
        differanseberegnetPeriodebeløp = nyttDifferanseberegnetBeløp
    )
}

private fun AndelTilkjentYtelse.utenDifferanseberegning(): AndelTilkjentYtelse {
    return copy(
        id = 0,
        kalkulertUtbetalingsbeløp = nasjonaltPeriodebeløp ?: this.kalkulertUtbetalingsbeløp,
        differanseberegnetPeriodebeløp = null
    )
}

fun <T : Tidsenhet> Tidslinje<AndelTilkjentYtelse, T>.utenDifferanseberegning() =
    mapIkkeNull { it.utenDifferanseberegning() }

fun Tidslinje<AndelTilkjentYtelse, Måned>.oppdaterDifferanseberegning(
    differanseberegnetBeløpTidslinje: Tidslinje<Int, Måned>
): Tidslinje<AndelTilkjentYtelse, Måned> {
    return this.kombinerMed(differanseberegnetBeløpTidslinje) { andel, differanseberegning ->
        andel.oppdaterDifferanseberegning(differanseberegning?.toBigDecimal())
    }
}
