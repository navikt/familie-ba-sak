package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt

class KomprimerendeTidslinje<I, T : Tidsenhet>(val tidslinje: Tidslinje<I, T>) :
    TidslinjeSomStykkerOppTiden<I, T>(tidslinje) {
    override fun finnInnholdForTidspunkt(tidspunkt: Tidspunkt<T>): I? = tidslinje.innholdForTidspunkt(tidspunkt)
}

fun <I, T : Tidsenhet> Tidslinje<I, T>.komprimer(): Tidslinje<I, T> = KomprimerendeTidslinje(this)
