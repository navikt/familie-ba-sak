package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt

class KomprimerendeTidslinje<T>(val tidslinje: Tidslinje<T>) : TidslinjeSomStykkerOppTiden<T>(tidslinje) {
    override fun finnInnholdForTidspunkt(tidspunkt: Tidspunkt): T? = tidslinje.hentUtsnitt(tidspunkt)
}

fun <T> Tidslinje<T>.komprimer(): Tidslinje<T> = KomprimerendeTidslinje(this)
