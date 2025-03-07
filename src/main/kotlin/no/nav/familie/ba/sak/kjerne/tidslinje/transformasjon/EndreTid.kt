package no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon

import no.nav.familie.tidslinje.Null
import no.nav.familie.tidslinje.PRAKTISK_TIDLIGSTE_DAG
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.TidslinjePeriodeMedDato
import no.nav.familie.tidslinje.tilPeriodeVerdi
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.konverterTilMåned
import no.nav.familie.tidslinje.utvidelser.tilTidslinjePerioderMedDato
import no.nav.familie.tidslinje.utvidelser.trim

/**
 * Extension-metode for å konvertere fra dag-basert tidslinje til måned-basert tidslinje
 * [mapper]-funksjonen tar inn listen av alle dagverdiene i én måned, og returner verdien måneden skal ha
 * Dagverdiene kommer i samme rekkefølge som dagene i måneden, og vil ha null-verdi hvis dagen ikke har en verdi
 */
fun <V> Tidslinje<V>.tilMåned(mapper: (List<V?>) -> V?): Tidslinje<V> =
    this.konverterTilMåned { _, månedListe ->
        val månedVerdier = månedListe.first().map { dag -> dag.periodeVerdi.verdi }
        mapper(månedVerdier).tilPeriodeVerdi()
    }

/**
 * Extention-metode som konverterer en dag-basert tidslinje til en måned-basert tidslinje.
 * [mapper]-funksjonen tar inn verdiene fra de to dagene før og etter månedsskiftet,
 * det vil si verdiene fra siste dag i forrige måned og første dag i inneværemde måned.
 * [mapper]-funksjonen kalles bare dersom begge dagene har en verdi.
 * Return-verdien er innholdet som blir brukt for inneværende måned.
 * Hvis retur-verdien er null, vil den resulterende måneden mangle verdi.
 * Funksjonen vil bruke månedsskiftene fra måneden før tidslinjen starter frem til og med siste måned i tidslinjen.
 */
fun <V> Tidslinje<V>.tilMånedFraMånedsskifteIkkeNull(
    mapper: (innholdSisteDagForrigeMåned: V, innholdFørsteDagDenneMåned: V) -> V?,
): Tidslinje<V> =
    if (this.erTom()) {
        this
    } else {
        this
            .konverterTilMåned(antallMndBakoverITid = 1) { _, (forrigeMåned, inneværendeMåned) ->
                val verdiForrigeMåned = forrigeMåned.lastOrNull()?.periodeVerdi?.verdi
                val verdiInneværendeMåned = inneværendeMåned.firstOrNull()?.periodeVerdi?.verdi

                if (verdiForrigeMåned == null || verdiInneværendeMåned == null) {
                    Null()
                } else {
                    mapper(verdiForrigeMåned, verdiInneværendeMåned).tilPeriodeVerdi()
                }
            }.trim(Null())
    }

/**
 * Extention-metode som konverterer en dag-basert tidslinje til en måned-basert tidslinje.
 * [mapper]-funksjonen tar inn verdiene fra de to dagene før og etter månedsskiftet,
 * det vil si verdiene fra siste dag i forrige måned og første dag i inneværemde måned.
 * Return-verdien er innholdet som blir brukt for inneværende måned.
 * Hvis retur-verdien er null, vil den resulterende måneden mangle verdi.
 * Funksjonen vil bruke månedsskiftene fra måneden før tidslinjen starter frem til og med måneden etter tidslinjen slutter.
 */
fun <V> Tidslinje<V>.tilMånedFraMånedsskifte(
    mapper: (innholdSisteDagForrigeMåned: V?, innholdFørsteDagDenneMåned: V?) -> V?,
): Tidslinje<V> =
    if (this.erTom()) {
        this
    } else {
        this
            .forlengTidslinjeMedEnMåned()
            .konverterTilMåned(antallMndBakoverITid = 1) { dato, (forrigeMåned, inneværendeMåned) ->
                val verdiForrigeMåned = forrigeMåned.lastOrNull()?.periodeVerdi?.verdi
                val verdiInneværendeMåned = inneværendeMåned.firstOrNull()?.periodeVerdi?.verdi

                if (dato == PRAKTISK_TIDLIGSTE_DAG) {
                    verdiInneværendeMåned.tilPeriodeVerdi()
                } else {
                    mapper(verdiForrigeMåned, verdiInneværendeMåned).tilPeriodeVerdi()
                }
            }.trim(Null())
    }

private fun <V> Tidslinje<V>.forlengTidslinjeMedEnMåned(): Tidslinje<V> =
    if (this.innhold.lastOrNull()?.erUendelig == true) {
        this
    } else {
        val fom = this.kalkulerSluttTidspunkt().plusDays(1)
        val tom = fom.plusMonths(1)
        val nyePerioder = this.tilTidslinjePerioderMedDato() + TidslinjePeriodeMedDato<V>(null, fom, tom)

        nyePerioder.tilTidslinje()
    }
