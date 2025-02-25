package no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.transformasjon

import no.nav.familie.tidslinje.Null
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilPeriodeVerdi
import no.nav.familie.tidslinje.utvidelser.konverterTilMåned
import no.nav.familie.tidslinje.utvidelser.trim

/**
 * Extension-metode for å konvertere fra dag-basert tidslinje til måned-basert tidslinje
 * mapper-funksjonen tar inn listen av alle dagverdiene i én måned, og returner verdien måneden skal ha
 * Dagverdiene kommer i samme rekkefølge som dagene i måneden, og vil ha null-verdi hvis dagen ikke har en verdi
 */
fun <V> Tidslinje<V>.tilMåned(mapper: (List<V?>) -> V?): Tidslinje<V> =
    this.konverterTilMåned { dato, månedListe ->
        val månedVerdier = månedListe.first().map { dag -> dag.periodeVerdi.verdi }
        mapper(månedVerdier).tilPeriodeVerdi()
    }

/**
 * Extention-metode som konverterer en dag-basert tidslinje til en måned-basert tidslinje.
 * <mapper>-funksjonen tar inn verdiene fra de to dagene før og etter månedsskiftet,
 * det vil si verdiene fra siste dag i forrige måned og første dag i inneværemde måned.
 * <mapper>-funksjonen kalles bare dersom begge dagene har en verdi.
 * Return-verdien er innholdet som blir brukt for inneværende måned.
 * Hvis retur-verdien er <null>, vil den resulterende måneden mangle verdi.
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
                val verdiDenneMåned = inneværendeMåned.firstOrNull()?.periodeVerdi?.verdi

                if (verdiForrigeMåned == null || verdiDenneMåned == null) {
                    Null()
                } else {
                    mapper(verdiForrigeMåned, verdiDenneMåned).tilPeriodeVerdi()
                }
            }.trim(Null())
    }

/**
 * Extention-metode som konverterer en dag-basert tidslinje til en måned-basert tidslinje.
 * <mapper>-funksjonen tar inn verdiene fra de to dagene før og etter månedsskiftet,
 * det vil si verdiene fra siste dag i forrige måned og første dag i inneværemde måned.
 * Return-verdien er innholdet som blir brukt for inneværende måned.
 * Hvis retur-verdien er <null>, vil den resulterende måneden mangle verdi.
 * Funksjonen vil bruke månedsskiftene fra måneden før tidslinjen starter frem til og med siste måned i tidslinjen.
 */
fun <V> Tidslinje<V>.tilMånedFraMånedsskifte(
    mapper: (innholdSisteDagForrigeMåned: V?, innholdFørsteDagDenneMåned: V?) -> V?,
): Tidslinje<V> =
    if (this.erTom()) {
        this
    } else {
        this
            .konverterTilMåned(antallMndBakoverITid = 1) { _, (forrigeMåned, inneværendeMåned) ->
                val verdiForrigeMåned = forrigeMåned.lastOrNull()?.periodeVerdi?.verdi
                val verdiDenneMåned = inneværendeMåned.firstOrNull()?.periodeVerdi?.verdi

                mapper(verdiForrigeMåned, verdiDenneMåned).tilPeriodeVerdi()
            }.trim(Null())
    }
