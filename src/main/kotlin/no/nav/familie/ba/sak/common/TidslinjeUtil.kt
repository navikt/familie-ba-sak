package no.nav.familie.ba.sak.common

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import java.time.LocalDate

fun erUnder18ÅrVilkårTidslinje(fødselsdato: LocalDate): Tidslinje<Boolean, Måned> = tidslinje {
    listOf(
        Periode(
            fødselsdato.toYearMonth().tilTidspunkt().neste(),
            fødselsdato.til18ÅrsVilkårsdato().toYearMonth().tilTidspunkt().forrige(),
            true
        )
    )
}

fun erUnder6ÅrTidslinje(person: Person) = tidslinje {
    listOf(
        Periode(
            person.fødselsdato.toYearMonth().tilTidspunkt(),
            person.fødselsdato.toYearMonth().plusYears(6).tilTidspunkt().forrige(),
            true
        )
    )
}

fun erTilogMed3ÅrTidslinje(fødselsdato: LocalDate): Tidslinje<Boolean, Måned> = tidslinje {
    listOf(
        Periode(
            fødselsdato.toYearMonth().tilTidspunkt().neste(),
            fødselsdato.plusYears(3).toYearMonth().tilTidspunkt(),
            true
        )
    )
}
