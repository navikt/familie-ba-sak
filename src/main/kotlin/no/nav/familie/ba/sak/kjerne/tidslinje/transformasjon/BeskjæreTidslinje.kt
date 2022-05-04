package no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.fraOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TomTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.minsteAv
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.størsteAv
import no.nav.familie.ba.sak.kjerne.tidslinje.tilOgMed

/**
 * Extension-metode for å beskjære (forkorte) en tidslinje etter en annen tidslinje
 * Etter beskjæringen vil tidslinjen maksimalt strekke seg fra [tidslinje]s fraOgMed() og til [tidslinje]s tilOgMed()
 * Perioder som ligger helt utenfor grensene vil forsvinne.
 * Perioden i hver ende som ligger delvis innenfor, vil forkortes.
 * Hvis ny og eksisterende grenseverdi begge er uendelige, vil den nye benyttes
 */
fun <I, T : Tidsenhet> Tidslinje<I, T>.beskjærEtter(tidslinje: Tidslinje<*, T>): Tidslinje<I, T> =
    beskjær(tidslinje.fraOgMed(), tidslinje.tilOgMed())

/**
 * Extension-metode for å beskjære (forkorte) en tidslinje etter til-og-med fra en annen tidslinje
 * Etter beskjæringen vil tidslinjen maksimalt strekke seg fra [this]s fraOgMed() og til [tidslinje]s tilOgMed()
 * Perioder som ligger helt utenfor grensene vil forsvinne.
 * Perioden i hver ende som ligger delvis innenfor, vil forkortes.
 * Hvis ny og eksisterende grenseverdi begge er uendelige, vil den nye benyttes
 */
fun <I, T : Tidsenhet> Tidslinje<I, T>.beskjærTilOgMedEtter(tidslinje: Tidslinje<*, T>): Tidslinje<I, T> =
    beskjær(this.fraOgMed(), tidslinje.tilOgMed())

/**
 * Extension-metode for å beskjære (forkorte) en tidslinje
 * Etter beskjæringen vil tidslinjen maksimalt strekke seg fra innsendt [fraOgMed] og til [tilOgMed]
 * Perioder som ligger helt utenfor grensene vil forsvinne.
 * Perioden i hver ende som ligger delvis innenfor, vil forkortes.
 * Uendelige endepunkter vil beskjæres til endelig hvis [fraOgMed] eller [tilOgMed] er endelige
 * Endelige endepunkter som beskjæres mot uendelige endepunkter, beholdes
 * Hvis ny og eksisterende grenseverdi begge er uendelige, vil den mest ekstreme benyttes
 */
fun <I, T : Tidsenhet> Tidslinje<I, T>.beskjær(fraOgMed: Tidspunkt<T>, tilOgMed: Tidspunkt<T>): Tidslinje<I, T> {

    val tidslinje = this

    return if (tilOgMed < fraOgMed)
        TomTidslinje()
    else object : Tidslinje<I, T>() {
        override fun lagPerioder(): Collection<Periode<I, T>> {
            return tidslinje.perioder()
                .filter { it.fraOgMed <= tilOgMed && it.tilOgMed >= fraOgMed }
                .map {
                    Periode(
                        størsteFraOgMed(fraOgMed, it.fraOgMed),
                        minsteTilOgMed(tilOgMed, it.tilOgMed),
                        it.innhold
                    )
                }
        }
    }
}

/**
 * Finner tidspunkt som representerer største (seneste) fra-og-med fra tidspunktene [t1] og [t2]
 * Tilfellet der både [t1] og [t2] er uendelig lenge siden må håndteres spesielt.
 * Da vil de betraktes som like, men vi velger den der det underliggende tidspunktet er minst/først
 * for å få lengst mulig underliggende tidslinje
 */
fun <T : Tidsenhet> størsteFraOgMed(t1: Tidspunkt<T>, t2: Tidspunkt<T>) = when {
    t1.erUendeligLengeSiden() && t2.erUendeligLengeSiden() -> minsteAv(t1, t2)
    else -> størsteAv(t1, t2)
}

/**
 * Finner tidspunkt som representerer minste (tidligste) til-og-med fra tidspunktene [t1] og [t2]
 * Tilfellet der både [t1] og [t2] er uendelig lenge til må håndteres spesielt.
 * Da vil de betraktes som like, men vi velger den der det underliggende tidspunktet er størst/sist
 * for å få lengst mulig underliggende tidslinje
 */
fun <T : Tidsenhet> minsteTilOgMed(t1: Tidspunkt<T>, t2: Tidspunkt<T>) = when {
    t1.erUendeligLengeTil() && t2.erUendeligLengeTil() -> størsteAv(t1, t2) // Riktig med største
    else -> minsteAv(t1, t2)
}
