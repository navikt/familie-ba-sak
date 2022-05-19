package no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.snittKombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet

/**
 * Extension-metode for å tilpasse en tidslinje til en [mønsterTidslinje]
 * Returnerer en tidlinje av samme type som [this] med samme fordeling av innhold som [mønsterTidslinje]
 * Hvis [mønsterTidslinje] mangler verdi (null) i en periode, så vil resultet også mangle verdi i perioden
 * Hivs [mønsterTidslinje] har innhold som mangler i [this], vil [nyttInnhold] kalles for å generere innholdet
 * Hvis begge har innhold, vil innholdet fra [this] bli brukt
 * Resulterende tidslinje vil strekke seg fra tidligste fra-og-med til seneste til-og-med fra begge tidslinjer
 */
fun <I, M, T : Tidsenhet> Tidslinje<I, T>.tilpassTil(
    mønsterTidslinje: Tidslinje<M, T>,
    nyttInnhold: (M) -> I
) = this.snittKombinerMed(mønsterTidslinje) { thisInnhold: I?, mønsterInnhold: M? ->
    when {
        mønsterInnhold == null -> null
        else -> thisInnhold ?: nyttInnhold(mønsterInnhold)
    }
}
