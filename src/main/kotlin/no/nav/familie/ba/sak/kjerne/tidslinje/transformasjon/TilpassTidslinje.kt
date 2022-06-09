package no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet

/**
 * Extension-metode for å tilpasse en tidslinje til en [mønsterTidslinje]
 * Returnerer en tidlinje av samme type som [this] med samme fordeling av innhold som [mønsterTidslinje]
 * Hvis [mønsterTidslinje] mangler verdi (null) i en periode, så vil resultet også mangle verdi i perioden
 * Hivs [mønsterTidslinje] har innhold, vil [tilpassInnhold] kalles for eventuelt å generere innholdet
 * Resulterende tidslinje vil strekke seg fra tidligste fra-og-med til seneste til-og-med fra begge tidslinjer
 */
fun <I, M, T : Tidsenhet> Tidslinje<I, T>.tilpassTil(
    mønsterTidslinje: Tidslinje<M, T>,
    tilpassInnhold: (I?, M) -> I
) = this.kombinerMed(mønsterTidslinje) { thisInnhold: I?, mønsterInnhold: M? ->
    when {
        mønsterInnhold == null -> null
        else -> tilpassInnhold(thisInnhold, mønsterInnhold)
    }
}
