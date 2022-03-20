package no.nav.familie.ba.sak.kjerne.tidslinje.util

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsrom
import java.time.YearMonth

fun jan(år: Int) = Tidspunkt.med(YearMonth.of(år, 1))
fun feb(år: Int) = Tidspunkt.med(YearMonth.of(år, 2))
fun mar(år: Int) = Tidspunkt.med(YearMonth.of(år, 3))
fun apr(år: Int) = Tidspunkt.med(YearMonth.of(år, 4))
fun mai(år: Int) = Tidspunkt.med(YearMonth.of(år, 5))
fun jun(år: Int) = Tidspunkt.med(YearMonth.of(år, 6))
fun jul(år: Int) = Tidspunkt.med(YearMonth.of(år, 7))
fun aug(år: Int) = Tidspunkt.med(YearMonth.of(år, 8))
fun sep(år: Int) = Tidspunkt.med(YearMonth.of(år, 9))
fun okt(år: Int) = Tidspunkt.med(YearMonth.of(år, 10))
fun nov(år: Int) = Tidspunkt.med(YearMonth.of(år, 11))
fun des(år: Int) = Tidspunkt.med(YearMonth.of(år, 12))

fun <T> Tidsrom.med(t: T) = Periode(this, t)
