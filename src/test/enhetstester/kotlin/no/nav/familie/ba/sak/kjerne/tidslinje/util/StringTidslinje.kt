package no.nav.familie.ba.sak.kjerne.tidslinje.util

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.TidslinjePeriodeMedDato
import no.nav.familie.tidslinje.tilTidslinje
import java.time.LocalDate
import java.time.YearMonth

fun List<String?>.tilStringTidslinje(startTidspunkt: LocalDate): Tidslinje<String> {
    val erUendeligLengeSiden = firstOrNull() == "<"
    val erUendeligLengeTil = lastOrNull() == ">"
    val sisteIndeks = filter { it != "<" && it != ">" }.lastIndex

    return this
        .filter { it != "<" && it != ">" }
        .mapIndexed { index, s ->
            TidslinjePeriodeMedDato(
                verdi = s,
                fom = if (index == 0 && erUendeligLengeSiden) null else startTidspunkt.plusDays(index.toLong()),
                tom = if (index == sisteIndeks && erUendeligLengeTil) null else startTidspunkt.plusDays(index.toLong()),
            )
        }.tilTidslinje()
}

fun List<String?>.tilStringTidslinje(startTidspunkt: YearMonth): Tidslinje<String> {
    val erUendeligLengeSiden = firstOrNull() == "<"
    val erUendeligLengeTil = lastOrNull() == ">"
    val sisteIndeks = filter { it != "<" && it != ">" }.lastIndex

    return this
        .filter { it != "<" && it != ">" }
        .mapIndexed { index, s ->
            TidslinjePeriodeMedDato(
                verdi = s,
                fom = if (index == 0 && erUendeligLengeSiden) null else startTidspunkt.plusMonths(index.toLong()).førsteDagIInneværendeMåned(),
                tom = if (index == sisteIndeks && erUendeligLengeTil) null else startTidspunkt.plusMonths(index.toLong()).sisteDagIInneværendeMåned(),
            )
        }.tilTidslinje()
}
