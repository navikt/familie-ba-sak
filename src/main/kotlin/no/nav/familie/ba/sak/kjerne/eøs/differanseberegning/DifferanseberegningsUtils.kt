package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSeparateTidslinjerForBarna
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TomTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.tidspunktKombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import java.math.BigDecimal
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
        UKENTLIG -> konverterFraUkentligTilMånedligBeløp(beløp, erSkuddår)
    }

    /***
     * Det finnes ingen offisiell dokumentasjon på hvordan vi skal konvertere fra ukentlig til
     * månedlig betaling i NAV per dags dato (19.05.2022). Til nå har saksbehandlere gjort dette manuelt og
     * brukt 52/12=4.33 for å konvertere fra ukentlig til månedlige beløp. Teamet har blitt enige om at vi burde
     * ta utgangspunkt i antall dager i et år. Siden valutajusteringen kun skjer en gang i året bruker
     * vi et gjennomsnitt for hver måned, slik at vi konverterer likt for måneder med forskjellig lengde.
     ***/
    private fun konverterFraUkentligTilMånedligBeløp(beløp: Double, erSkuddår: Boolean): Double {
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

fun beregnMånedligUtbetalingsbeløpUtlandINok(
    satsUtland: Int,
    kurs: Double,
    intervall: Intervall,
    erSkuddår: Boolean
): Int {
    val beløpINorskeKroner = satsUtland * kurs
    val utbetalingsbeløpUtlandINok = intervall.konverterBeløpTilMånedlig(
        beløp = beløpINorskeKroner,
        erSkuddår = erSkuddår
    )
    val avrundetUtbetalingsbeløpUtlandINok = utbetalingsbeløpUtlandINok.roundToInt()

    return avrundetUtbetalingsbeløpUtlandINok
}

fun kalkulerUtenlandskPeriodebeøpINorskeKroner(
    utenlandskePeriodebeløp: Collection<UtenlandskPeriodebeløp>,
    valutakurser: Collection<Valutakurs>
): Map<Aktør, Tidslinje<BigDecimal, Måned>> {

    val utenlandskePeriodebeløpTidslinjer = utenlandskePeriodebeløp.tilSeparateTidslinjerForBarna()
    val valutakursTidslinjer = valutakurser.tilSeparateTidslinjerForBarna()

    val alleBarnAktørIder = utenlandskePeriodebeløpTidslinjer.keys + valutakursTidslinjer.keys

    return alleBarnAktørIder.associateWith { aktør ->
        val utenlandskePeriodebeløpTidslinje = utenlandskePeriodebeløpTidslinjer.getOrDefault(aktør, TomTidslinje())
        val valutakursTidslinje = valutakursTidslinjer.getOrDefault(aktør, TomTidslinje())

        utenlandskePeriodebeløpTidslinje.tidspunktKombinerMed(valutakursTidslinje) { tidspunkt, upb, vk ->
            when {
                upb == null || vk == null -> null
                upb.valutakode != vk.valutakode -> null
                upb.beløp == null || vk.kurs == null -> null
                else -> upb.tilMånedligBeløp(tidspunkt)?.multiply(vk.kurs)
            }
        }
    }
}

fun <T : Tidsenhet> UtenlandskPeriodebeløp.tilMånedligBeløp(tidspunkt: Tidspunkt<T>): BigDecimal? {
    if (this.beløp == null || this.intervall == null)
        return null

    return Intervall.valueOf(this.intervall).konverterBeløpTilMånedlig(this.beløp.toDouble(), tidspunkt.erSkuddår())
        .toBigDecimal()
}

fun <T : Tidsenhet> Tidspunkt<T>.erSkuddår() = this.tilLocalDateEllerNull()?.isLeapYear ?: false
