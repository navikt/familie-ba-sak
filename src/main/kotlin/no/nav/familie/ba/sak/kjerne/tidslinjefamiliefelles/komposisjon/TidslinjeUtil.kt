package no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.komposisjon

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.common.sisteDagIForrigeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import java.time.LocalDate
import java.time.YearMonth

fun erUnder18ÅrVilkårTidslinje(fødselsdato: LocalDate): Tidslinje<Boolean> =
    opprettBooleanTidslinje(
        fraDato = fødselsdato.førsteDagINesteMåned(),
        tilDato = fødselsdato.plusYears(18).sisteDagIForrigeMåned(),
    )

fun erUnder6ÅrTidslinje(person: Person): Tidslinje<Boolean> =
    opprettBooleanTidslinje(
        fraDato = person.fødselsdato,
        tilDato = person.fødselsdato.plusYears(6).sisteDagIForrigeMåned(),
    )

fun erTilogMed3ÅrTidslinje(fødselsdato: LocalDate): Tidslinje<Boolean> =
    opprettBooleanTidslinje(
        fraDato = fødselsdato,
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
