package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt

/**
 * Extension-funksjon som slår sammen påfølgende perioder der innholdet er likt
 * Benytter TidslinjeSomStykkerOppTiden, som bygger sammenslåtte perioder som default
 */
fun <I, T : Tidsenhet> Tidslinje<I, T>.slåSammenLike(): Tidslinje<I, T> {
    val tidslinje = this
    return object : TidslinjeSomStykkerOppTiden<I, T>(tidslinje) {
        override fun finnInnholdForTidspunkt(tidspunkt: Tidspunkt<T>): I? =
            tidslinje.innholdForTidspunkt(tidspunkt)
    }
}
