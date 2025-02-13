package no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.util

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.konverterTilMåned
import java.time.LocalDate
import java.time.YearMonth

fun List<String>.tilStringTidslinje(startTidspunkt: LocalDate): Tidslinje<String> {
    val erUendeligLengeSiden = firstOrNull() == "<"
    val erUendeligLengeTil = lastOrNull() == ">"
    val sisteIndeks = filter { it !in "<>" }.lastIndex

    return this
        .filter { it !in "<>" }
        .mapIndexed { index, s ->
            Periode(
                verdi = s,
                fom = if (index == 0 && erUendeligLengeSiden) null else startTidspunkt.plusDays(index.toLong()),
                tom = if (index == sisteIndeks && erUendeligLengeTil) null else startTidspunkt.plusDays(index.toLong()),
            )
        }.tilTidslinje()
}

fun List<String>.tilStringTidslinje(startTidspunkt: YearMonth): Tidslinje<String> {
    val erUendeligLengeSiden = firstOrNull() == "<"
    val erUendeligLengeTil = lastOrNull() == ">"
    val sisteIndeks = filter { it !in "<>" }.lastIndex

    return this
        .filter { it !in "<>" }
        .mapIndexed { index, s ->
            Periode(
                verdi = s,
                fom = if (index == 0 && erUendeligLengeSiden) null else startTidspunkt.plusMonths(index.toLong()).førsteDagIInneværendeMåned(),
                tom = if (index == sisteIndeks && erUendeligLengeTil) null else startTidspunkt.plusMonths(index.toLong()).sisteDagIInneværendeMåned(),
            )
        }.tilTidslinje()
        .konverterTilMåned { _, månedListe ->
            månedListe.first().first().periodeVerdi
        }
}
