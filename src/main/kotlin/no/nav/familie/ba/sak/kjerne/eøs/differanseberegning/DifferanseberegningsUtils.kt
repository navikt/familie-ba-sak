package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

data class KronerPerValutaenhet(
    val kronerPerValutaenhet: BigDecimal,
    val valutakode: String,
)

data class Valutabeløp(
    val beløp: BigDecimal,
    val valutakode: String
)

enum class Intervall {
    ÅRLIG,
    KVARTALSVIS,
    MÅNEDLIG,
    UKENTLIG;

    fun konverterBeløpTilMånedlig(beløp: BigDecimal, erSkuddår: Boolean) = when (this) {
        ÅRLIG -> beløp / 12.toBigDecimal()
        KVARTALSVIS -> beløp / 3.toBigDecimal()
        MÅNEDLIG -> beløp
        UKENTLIG -> konverterFraUkentligTilMånedligBeløp(beløp, erSkuddår)
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
}

fun <T : Tidsenhet> UtenlandskPeriodebeløp?.tilMånedligValutabeløp(tidspunkt: Tidspunkt<T>): Valutabeløp? {
    if (this?.beløp == null || this.intervall == null || this.valutakode == null)
        return null

    val månedligBeløp =
        Intervall.valueOf(this.intervall).konverterBeløpTilMånedlig(this.beløp, tidspunkt.erSkuddår())

    return Valutabeløp(månedligBeløp, this.valutakode)
}

fun Valutakurs?.tilKronerPerValutaenhet(): KronerPerValutaenhet? {
    if (this?.kurs == null || this.valutakode == null)
        return null

    return KronerPerValutaenhet(this.kurs, this.valutakode)
}

fun <T : Tidsenhet> Tidspunkt<T>.erSkuddår() = this.tilLocalDateEllerNull()?.isLeapYear ?: false

/**
 * Kalkulerer nytt utebetalingsbeløp fra [utenlandskPeriodebeløpINorskeKroner], såkalt differanseberegning
 * Må håndtere tilfellet der [kalkulertUtebetalngsbeløp] blir modifisert andre steder i koden, men antar at det aldri vil være negativt
 * [nasjonaltPeriodebeløp] settes til den originale, nasjonale beregningen (aldri negativt)
 * [differanseberegnetBeløp] er differansen mellom [nasjonaltPeriodebeløp] og (avrundet) [utenlandskPeriodebeløpINorskeKroner] (kan bli negativt)
 * [kalkulertUtebetalngsbeløp] blir satt til [differanseberegnetBeløp], med mindre det er negativt. Da blir det 0 (null)
 */
fun AndelTilkjentYtelse?.kalkulerFraUtenlandskPeriodebeløp(utenlandskPeriodebeløpINorskeKroner: BigDecimal): AndelTilkjentYtelse? {
    if (this == null)
        return null

    val nyttNasjonaltPeriodebeløp = when {
        differanseberegnetBeløp == null || nasjonaltPeriodebeløp == null -> kalkulertUtbetalingsbeløp
        differanseberegnetBeløp < 0 && kalkulertUtbetalingsbeløp > 0 -> kalkulertUtbetalingsbeløp
        differanseberegnetBeløp != kalkulertUtbetalingsbeløp -> kalkulertUtbetalingsbeløp
        else -> nasjonaltPeriodebeløp
    }

    val avrundetUtenlandskPeriodebeløp = utenlandskPeriodebeløpINorskeKroner
        .round(MathContext(0, RoundingMode.DOWN)).intValueExact() // Rund ned for å gi fordel til bruker
    val nyttDifferanseberegnetBeløp = nyttNasjonaltPeriodebeløp - avrundetUtenlandskPeriodebeløp

    return copy(
        id = 0, // Lager en ny instans
        kalkulertUtbetalingsbeløp = maxOf(nyttDifferanseberegnetBeløp, 0),
        nasjonaltPeriodebeløp = nyttNasjonaltPeriodebeløp,
        differanseberegnetBeløp = nyttDifferanseberegnetBeløp
    )
}

operator fun Valutabeløp?.times(kronerPerValutaenhet: KronerPerValutaenhet?): BigDecimal? {
    if (this == null || kronerPerValutaenhet == null)
        return null

    if (this.valutakode != kronerPerValutaenhet.valutakode)
        throw IllegalArgumentException("Valutabeløp har valutakode $valutakode, som avviker fra ${kronerPerValutaenhet.valutakode}")

    return this.beløp * kronerPerValutaenhet.kronerPerValutaenhet
}
