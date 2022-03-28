package no.nav.familie.ba.sak.kjerne.tidslinje.util

import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TidslinjeSomStykkerOppTiden
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.hentUtsnitt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt

class BooleanCharTidslinje<T : Tidsenhet>(
    val charTidslinje: CharTidslinje<T>
) : TidslinjeSomStykkerOppTiden<Boolean, T>(charTidslinje) {
    constructor(tegn: String, startTidspunkt: Tidspunkt<T>) : this(CharTidslinje(tegn, startTidspunkt))

    override fun finnInnholdForTidspunkt(tidspunkt: Tidspunkt<T>) =
        when (charTidslinje.hentUtsnitt(tidspunkt)?.lowercaseChar()) {
            't' -> true
            'f' -> false
            else -> null
        }
}
