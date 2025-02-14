package no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.komposisjon

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.common.sisteDagIForrigeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.tidslinje.Null
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.familie.tidslinje.utvidelser.trim
import java.time.LocalDate
import java.time.YearMonth

fun erUnder18ÅrVilkårTidslinje(fødselsdato: LocalDate): Tidslinje<Boolean> =
    opprettBooleanTidslinje(
        fraDato = fødselsdato.førsteDagINesteMåned(),
        tilDato = fødselsdato.plusYears(18).sisteDagIForrigeMåned(),
    )

fun erUnder6ÅrTidslinje(person: Person): Tidslinje<Boolean> =
    opprettBooleanTidslinje(
        fraDato = person.fødselsdato.førsteDagIInneværendeMåned(),
        tilDato = person.fødselsdato.plusYears(6).sisteDagIForrigeMåned(),
    )

fun erTilogMed3ÅrTidslinje(fødselsdato: LocalDate): Tidslinje<Boolean> =
    opprettBooleanTidslinje(
        fraDato = fødselsdato.førsteDagINesteMåned(),
        tilDato = fødselsdato.plusYears(3).sisteDagIMåned(),
    )

fun opprettBooleanTidslinje(
    fraÅrMåned: YearMonth,
    tilÅrMåned: YearMonth,
) = listOf(
    Periode(
        verdi = true,
        fom = fraÅrMåned.førsteDagIInneværendeMåned(),
        tom = tilÅrMåned.sisteDagIInneværendeMåned(),
    ),
).tilTidslinje()

fun opprettBooleanTidslinje(
    fraDato: LocalDate,
    tilDato: LocalDate,
) = listOf(Periode(verdi = true, fom = fraDato, tom = tilDato)).tilTidslinje()

fun <V> Tidslinje<V>.verdiPåTidspunkt(tidspunkt: LocalDate): V? = this.tilPerioder().verdiPåidspunkt(tidspunkt)

fun <V> Collection<Periode<V>>.verdiPåidspunkt(tidspunkt: LocalDate): V? = this.firstOrNull { it.omfatter(tidspunkt) }?.verdi

private fun <V> Periode<V>.omfatter(tidspunkt: LocalDate) =
    when {
        fom == null && tom == null -> true
        fom == null -> tom!!.isSameOrAfter(tidspunkt)
        tom == null -> fom!!.isSameOrBefore(tidspunkt)
        else -> fom!!.isSameOrBefore(tidspunkt) && tom!!.isSameOrAfter(tidspunkt)
    }

fun <V> Periode<V>.tilTidslinje(): Tidslinje<V> = listOf(this).tilTidslinje()

fun <V> Tidslinje<V>.trimNull(): Tidslinje<V> = trim(Null())
