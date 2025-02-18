package no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.transformasjon

import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.beskjærEtter
import no.nav.familie.tidslinje.utvidelser.filtrer
import no.nav.familie.tidslinje.utvidelser.kombinerMed

fun <V> Tidslinje<V>.filtrerIkkeNull(filter: (V) -> Boolean): Tidslinje<V> = filtrer { it != null && filter(it) }

/**
 * Extension-metode for å filtrere tidslinjen mot en boolsk tidslinje
 * Resultatet får samme lengde som tidslinjen det opereres på
 * Det vil finnes perioder som tilsvarer periodene fra kilde-tidslinjen,
 * men innholdet blir null hvis den boolske tidslinjen er false
 */
fun <V> Tidslinje<V>.filtrerMed(boolskTidslinje: Tidslinje<Boolean>): Tidslinje<V> =
    this
        .kombinerMed(boolskTidslinje) { verdi, erSann ->
            when (erSann) {
                true -> verdi
                else -> null
            }
        }.beskjærEtter(this)

/**
 * Extension-metode for å filtrere innholdet i en map av tidslinjer
 */
fun <K, V> Map<K, Tidslinje<V>>.filtrerHverKunVerdi(
    filter: (V) -> Boolean,
) = mapValues { (_, tidslinje) -> tidslinje.filtrer { if (it != null) filter(it) else false } }
