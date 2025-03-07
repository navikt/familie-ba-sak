package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tomTidslinje

/**
 * Extension-metode for å kombinere to nøkkel-verdi-map'er der verdiene er tidslinjer
 * Nøkkelen må være av samme type K
 * Verdiene i tidslinjene i map'en på venstre side må alle være av typen V
 * Verdiene i tidslinjene i map'en på høyre side må alle være av typen H
 * Kombinator-funksjonen kalles med verdiene av fra venstre og høyre tidslinje for samme nøkkel og tidspunkt.
 * Kombinator-funksjonen blir IKKE kalt hvis venstre, høyre eller begge tidslinjer mangler verdi for et tidspunkt
 * Resultatet er en ny map der nøklene er av type K, og tidslinjene har innhold av typen (nullable) R.
 * Bare nøkler som finnes i begge map'ene vil finnes i den resulterende map'en
 */
fun <K, V, H, R> Map<K, Tidslinje<V>>.joinIkkeNull(
    høyreTidslinjer: Map<K, Tidslinje<H>>,
    kombinator: (V, H) -> R?,
): Map<K, Tidslinje<R>> {
    val venstreTidslinjer = this
    val alleNøkler = venstreTidslinjer.keys.intersect(høyreTidslinjer.keys)

    return alleNøkler.associateWith { nøkkel ->
        val venstreTidslinje = venstreTidslinjer.getOrDefault(nøkkel, tomTidslinje())
        val høyreTidslinje = høyreTidslinjer.getOrDefault(nøkkel, tomTidslinje())

        venstreTidslinje.kombinerUtenNullMed(høyreTidslinje, kombinator)
    }
}
