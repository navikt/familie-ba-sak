package no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.komposisjon

import no.nav.familie.tidslinje.Null
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.Udefinert
import no.nav.familie.tidslinje.Verdi
import no.nav.familie.tidslinje.tilPeriodeVerdi
import no.nav.familie.tidslinje.utvidelser.biFunksjon
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.map
import no.nav.familie.tidslinje.utvidelser.slåSammen

/**
 * Extension-metode for å kombinere to tidslinjer der begge har verdi
 * Kombinasjonen baserer seg på å iterere gjennom alle tidspunktene
 * fra minste fraOgMed til største tilOgMed fra begge tidslinjene
 * Tidsenhet (T) må være av samme type
 * Hver av tidslinjene kan ha ulik innholdstype, hhv V og H
 * Hvis innholdet V eller H mangler innhold, så vil ikke resulterende tidslinje få innhold for det tidspunktet
 * Kombintor-funksjonen tar ellers V og H og returnerer (nullable) R
 * Hvis kombinator-funksjonen returner <null>, antas det at tidslinjen ikke skal ha verdi for tidspunktet
 * Resultatet er en tidslinje med tidsenhet T og innhold R
 */
fun <V, H, R> Tidslinje<V>.kombinerUtenNullMed(
    høyreTidslinje: Tidslinje<H>,
    kombineringsfunksjon: (V, H) -> R?,
): Tidslinje<R> =
    this.biFunksjon(høyreTidslinje) { periodeverdiVenstre, periodeverdiHøyre ->
        when {
            periodeverdiVenstre is Verdi && periodeverdiHøyre is Verdi ->
                kombineringsfunksjon(periodeverdiVenstre.verdi, periodeverdiHøyre.verdi).tilPeriodeVerdi()

            else -> Null()
        }
    }

/**
 * Extension-metode for å kombinere liste av tidslinjer
 * Kombinasjonen baserer seg på å iterere gjennom alle tidspunktene
 * fra minste <fraOgMed()> til største <tilOgMed()> fra alle tidslinjene
 * Innhold (I) og tidsenhet (T) må være av samme type
 * Kombintor-funksjonen tar inn Iterable<I> og returner (nullable) R
 * Null-verdier fjernes før de sendes til kombinator-funksjonen, som betyr at en tom iterator kan bli sendt
 * Hvis reesultatet fra kombinatoren er null, tolkes det som at det ikke skal være innhold
 * Resultatet er en tidslinje med tidsenhet T og innhold R
 */
fun <V, R> Collection<Tidslinje<V>>.kombinerUtenNull(
    listeKombinator: (Iterable<V>) -> R?,
): Tidslinje<R> = kombinerNullableKombinator { it.filterNotNull().let(listeKombinator) }

/**
 * Extension-metode for å kombinere liste av tidslinjer
 * Kombinasjonen baserer seg på å iterere gjennom alle tidspunktene
 * fra minste <fraOgMed()> til største <tilOgMed()> fra alle tidslinjene
 * Innhold (I) og tidsenhet (T) må være av samme type
 * Kombintor-funksjonen tar inn Iterable<I> og returner (nullable) R
 * Null-verdier fjernes, og listen av verdier sendes til kombinator-funksjonen bare hvis den inneholder verdier
 * Hvis reesultatet fra kombinatoren er null, tolkes det som at det ikke skal være innhold
 * Resultatet er en tidslinje med tidsenhet T og innhold R
 */
fun <V, R> Collection<Tidslinje<V>>.kombinerUtenNullOgIkkeTom(
    listeKombinator: (Iterable<V>) -> R?,
): Tidslinje<R> = kombinerNullableKombinator { it.filterNotNull().takeIf { it.isNotEmpty() }?.let(listeKombinator) }

/**
 * Extension-metode for å kombinere liste av tidslinjer
 * Kombinasjonen baserer seg på å iterere gjennom alle tidspunktene
 * fra minste <fraOgMed()> til største <tilOgMed()> fra alle tidslinjene
 * Innhold (I) og tidsenhet (T) må være av samme type
 * Resultatet er en tidslinje med tidsenhet T og innhold Iterable<I>
 */
fun <V> Collection<Tidslinje<V>>.kombiner() = this.kombinerNullableKombinator { if (it.toList().isNotEmpty()) it else null }

/**
 * Extension-metode for å kombinere en nøkkel-verdi-map'er der verdiene er tidslinjer, med en enkelt tidslinje
 * Innholdet i tidslinjene i map'en på venstre side må alle være av typen V
 * Innholdet i tidslinjen på høyre side er av typen H
 * Kombinator-funksjonen kalles for hvert tidspunkt med med verdien for det tidspunktet fra høyre tidslinje og
 * vedien fra den enkelte av venstre tidslinjer etter tur.
 * Kombinator-funksjonen blir IKKE kalt Hvis venstre, høyre eller begge tidslinjer mangler verdi for et tidspunkt
 * Resultatet er en ny map der nøklene er av type K, og tidslinjene har innhold av typen (nullable) R.
 */
fun <K, V, H, R> Map<K, Tidslinje<V>>.kombinerKunVerdiMed(
    høyreTidslinje: Tidslinje<H>,
    kombinator: (V, H) -> R?,
): Map<K, Tidslinje<R>> {
    val venstreTidslinjer = this

    return venstreTidslinjer.mapValues { (_, venstreTidslinje) ->
        venstreTidslinje.kombinerUtenNullMed(høyreTidslinje, kombinator)
    }
}

/**
 * Extension-metode for å kombinere tre tidslinjer
 * Kombinasjonen baserer seg på å iterere gjennom alle tidspunktene
 * fra minste <fraOgMed()> til største <tilOgMed()> fra alle tidslinjene
 * Tidsenhet (T) må være av samme type
 * Hver av tidslinjene kan ha ulik innholdstype, hhv A, B og C
 * Kombintor-funksjonen tar inn (nullable) av A, B og C og returner (nullable) R
 * Resultatet er en tidslinje med tidsenhet T og innhold R
 */
fun <A, B, C, R> Tidslinje<A>.kombinerKunVerdiMed(
    tidslinjeB: Tidslinje<B>,
    tidslinjeC: Tidslinje<C>,
    kombinator: (A, B, C) -> R?,
): Tidslinje<R> =
    this.kombinerMed(tidslinjeB, tidslinjeC) { a, b, c ->
        when {
            a != null && b != null && c != null -> kombinator(a, b, c)
            else -> null
        }
    }

fun <V> Tidslinje<V>.erIkkeTom() = !this.erTom()

fun <V, H> Tidslinje<V>.harOverlappMed(tidslinje: Tidslinje<H>) = this.kombinerUtenNullMed(tidslinje) { v, h -> true }.erIkkeTom()

fun <V, H> Tidslinje<V>.harIkkeOverlappMed(tidslinje: Tidslinje<H>) = !this.harOverlappMed(tidslinje)

fun <V, H> Tidslinje<V>.kombinerMedNullable(
    høyreTidslinje: Tidslinje<H>?,
    kombinator: (V?, H?) -> V?,
): Tidslinje<V> =
    if (høyreTidslinje != null) {
        kombinerMed(høyreTidslinje, kombinator)
    } else {
        this
    }

/**
 * Extension-metode for å kombinere liste av tidslinjer
 * Kombinasjonen baserer seg på å iterere gjennom alle tidspunktene
 * fra minste <fraOgMed()> til største <tilOgMed()> fra alle tidslinjene
 * Innhold (I) og tidsenhet (T) må være av samme type
 * Kombintor-funksjonen tar inn Iterable<I> og returner (nullable) R
 * Resultatet er en tidslinje med tidsenhet T og innhold R
 */
@Deprecated("Skal flyttes til familie-felles")
fun <V, R> Collection<Tidslinje<V>>.kombinerNullableKombinator(listeKombinator: (Iterable<V>) -> R?): Tidslinje<R> =
    this.slåSammen().map {
        when (it) {
            is Verdi -> {
                val resultat = listeKombinator(it.verdi)
                if (resultat != null) Verdi(resultat) else Null()
            }

            is Null -> Null()
            is Udefinert -> Udefinert()
        }
    }
