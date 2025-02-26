package no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.util

import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.mapVerdi
import java.time.YearMonth

fun String.somBoolskTidslinje(startTidspunkt: YearMonth) = this.tilCharTidslinje(startTidspunkt).somBoolsk()

fun Tidslinje<Char>.somBoolsk() =
    this.mapVerdi {
        when (it?.lowercaseChar()) {
            't' -> true
            'f' -> false
            else -> null
        }
    }
