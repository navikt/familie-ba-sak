package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import kotlin.math.roundToInt

enum class Intervall {
    ÅRLIG,
    KVARTALSVIS,
    MÅNEDLIG,
    UKENTLIG;

    fun konverterBeløpTilMånedlig(beløp: Double, erSkuddår: Boolean) = when (this) {
        ÅRLIG -> beløp / 12
        KVARTALSVIS -> beløp / 4
        MÅNEDLIG -> beløp
        UKENTLIG -> konverterFraUkentligTilMåentligBeløp(beløp, erSkuddår)
    }

    /***
     * Det finnes ingen offisiell dokumentasjon på hvordan vi skal konvertere fra ukentlig til
     * månedlig betaling i NAV per dags dato (19.05.2022). Til nå har saksbehandlere gjort dette manuelt og
     * brukt 52/12=4.33 for å konvertere fra ukentlig til månedlige beløp. Teamet har blitt enige om at vi burde
     * ta utgangspunkt i antall dager i et år. Siden valutajusteringen kun skjer en gang i året bruker
     * vi et gjennomsnitt for hver måned, slik at vi konverterer likt for måneder med forskjellig lengde.
     ***/
    private fun konverterFraUkentligTilMåentligBeløp(beløp: Double, erSkuddår: Boolean): Double {
        val dagerIÅret: Double = if (erSkuddår) 366.0 else 365.0
        val ukerIÅret: Double = dagerIÅret / 7.0
        val ukerIEnMåned: Double = ukerIÅret / 12.0

        return beløp * ukerIEnMåned
    }
}

fun beregnDifferanseOrdinær(utbetalingsbeløpNorge: Int, utbetalingsbeløpUtlandINok: Int): Int {
    val differanse = utbetalingsbeløpNorge - utbetalingsbeløpUtlandINok

    return if (differanse < 0) 0
    else differanse
}

fun beregnUtbetalingsbeløpUtlandINok(satsUtland: Int, kurs: Double, intervall: Intervall, erSkuddår: Boolean): Int {
    val beløpINorskeKroner = satsUtland * kurs
    val utbetalingsbeløpUtlandINok = intervall.konverterBeløpTilMånedlig(
        beløp = beløpINorskeKroner,
        erSkuddår = erSkuddår
    )
    val avrundetUtbetalingsbeløpUtlandINok = utbetalingsbeløpUtlandINok.roundToInt()

    return avrundetUtbetalingsbeløpUtlandINok
}
