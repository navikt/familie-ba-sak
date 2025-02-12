package no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.transformasjon

import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tomTidslinje
import no.nav.familie.tidslinje.utvidelser.klipp
import java.time.LocalDate

/**
 * Extension-metode for å beskjære (forkorte) en tidslinje etter til-og-med fra en annen tidslinje
 * Etter beskjæringen vil tidslinjen maksimalt strekke seg fra [this]s fraOgMed() og til [tidslinje]s tilOgMed()
 * Perioder som ligger helt utenfor grensene vil forsvinne.
 * Perioden i hver ende som ligger delvis innenfor, vil forkortes.
 * Hvis ny og eksisterende grenseverdi begge er uendelige, vil den nye benyttes
 * Beskjæring mot tom tidslinje vil gi tom tidslinje
 */
fun <I> Tidslinje<I>.beskjærTilOgMedEtter(tidslinje: Tidslinje<*>): Tidslinje<I> =
    when {
        tidslinje.erTom() -> tomTidslinje()
        else ->
            klipp(
                startsTidspunkt = startsTidspunkt,
                sluttTidspunkt = tidslinje.kalkulerSluttTidspunkt(),
            )
    }

/**
 * Extension-metode for å beskjære (forkorte) en tidslinje
 * Etter beskjæringen vil tidslinjen maksimalt strekke seg fra innsendt [fraOgMed] og til [tilOgMed]
 * Perioder som ligger helt utenfor grensene vil forsvinne.
 * Perioden i hver ende som ligger delvis innenfor, vil forkortes.
 * Uendelige endepunkter vil beskjæres til endelig hvis [fraOgMed] eller [tilOgMed] er endelige
 * Endelige endepunkter som beskjæres mot uendelige endepunkter, beholdes
 * Hvis ny og eksisterende grenseverdi begge er uendelige, vil den mest ekstreme benyttes
 */
fun <V> Tidslinje<V>.beskjær(
    fraOgMed: LocalDate,
    tilOgMed: LocalDate,
): Tidslinje<V> =
    when {
        erTom() -> tomTidslinje()
        else ->
            klipp(
                startsTidspunkt = fraOgMed,
                sluttTidspunkt = tilOgMed,
            )
    }

/**
 * Extension-metode for å beskjære fom dato på en tidslinje
 * Etter beskjæringen vil tidslinjen maksimalt strekke seg fra innsendt [fraOgMed] og til eksisterende tilOgMed
 */
fun <V> Tidslinje<V>.beskjærFraOgMed(
    fraOgMed: LocalDate,
): Tidslinje<V> =
    when {
        erTom() -> tomTidslinje()
        else ->
            klipp(
                startsTidspunkt = fraOgMed,
                sluttTidspunkt = kalkulerSluttTidspunkt(),
            )
    }

/**
 * Extension-metode for å beskjære tom dato på en tidslinje
 * Etter beskjæringen vil tidslinjen maksimalt strekke seg fra eksisterende fraOgMed og til innsendt [tilOgMed]
 */
fun <V> Tidslinje<V>.beskjærTilOgMed(
    tilOgMed: LocalDate,
): Tidslinje<V> =
    when {
        erTom() -> tomTidslinje()
        else ->
            klipp(
                startsTidspunkt = startsTidspunkt,
                sluttTidspunkt = tilOgMed,
            )
    }

/**
 * Extension-metode for å beskjære tom dato på et map av tidslinjer
 * Etter beskjæringen vil tidslinjen maksimalt strekke seg fra eksisterende fraOgMed og til innsendt [tilOgMed]
 */
fun <K, V> Map<K, Tidslinje<V>>.beskjærTilOgMed(
    tilOgMed: LocalDate,
): Map<K, Tidslinje<V>> = this.mapValues { (_, tidslinje) -> tidslinje.beskjærTilOgMed(tilOgMed) }
