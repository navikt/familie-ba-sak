package no.nav.familie.ba.sak.kjerne.tidslinje.util

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.map
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk

fun <T : Tidsenhet> Tidslinje<Char, T>.somRegelverk() = this.map {
    when (it?.lowercaseChar()) {
        'e' -> Regelverk.EØS_FORORDNINGEN
        'n' -> Regelverk.NASJONALE_REGLER
        else -> null
    }
}
