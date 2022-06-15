package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import java.math.BigDecimal

fun Intervall.konverterBeløpTilMånedlig(beløp: BigDecimal, erSkuddår: Boolean) = when (this) {
    Intervall.ÅRLIG -> beløp / 12.toBigDecimal()
    Intervall.KVARTALSVIS -> beløp / 3.toBigDecimal()
    Intervall.MÅNEDLIG -> beløp
    Intervall.UKENTLIG -> konverterFraUkentligTilMånedligBeløp(beløp, erSkuddår)
}

/***
 * Det finnes ingen offisiell dokumentasjon på hvordan vi skal konvertere fra ukentlig til
 * månedlig betaling i NAV per dags dato (19.05.2022). Til nå har saksbehandlere gjort dette manuelt og
 * brukt 52/12=4.33 for å konvertere fra ukentlig til månedlige beløp. Teamet har blitt enige om at vi burde
 * ta utgangspunkt i antall dager i et år. Siden valutajusteringen kun skjer en gang i året bruker
 * vi et gjennomsnitt for hver måned, slik at vi konverterer likt for måneder med forskjellig lengde.
 ***/
private fun konverterFraUkentligTilMånedligBeløp(beløp: BigDecimal, erSkuddår: Boolean): BigDecimal {
    val dagerIÅret: Double = if (erSkuddår) 366.0 else 365.0
    val ukerIÅret: Double = dagerIÅret / 7.0
    val ukerIEnMåned: Double = ukerIÅret / 12.0

    return beløp.multiply(BigDecimal.valueOf(ukerIEnMåned))
}

fun <T : Tidsenhet> Tidspunkt<T>.erSkuddår() = this.tilLocalDateEllerNull()?.isLeapYear ?: false

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
