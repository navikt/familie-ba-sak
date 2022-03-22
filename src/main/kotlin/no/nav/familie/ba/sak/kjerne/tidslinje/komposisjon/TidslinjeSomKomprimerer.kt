package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import java.time.temporal.Temporal

class KomprimerendeTidslinje<T, TID : Temporal>(val tidslinje: Tidslinje<T, TID>) :
    TidslinjeSomStykkerOppTiden<T, TID>(tidslinje) {
    override fun finnInnholdForTidspunkt(tidspunkt: Tidspunkt<TID>): T? = tidslinje.hentUtsnitt(tidspunkt)
}

fun <T, TID : Temporal> Tidslinje<T, TID>.komprimer(): Tidslinje<T, TID> = KomprimerendeTidslinje(this)
