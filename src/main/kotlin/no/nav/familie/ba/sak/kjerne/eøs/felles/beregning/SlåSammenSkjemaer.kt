package no.nav.familie.ba.sak.kjerne.eøs.felles.beregning

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjema
import no.nav.familie.ba.sak.kjerne.eøs.felles.utenBarn
import no.nav.familie.ba.sak.kjerne.eøs.felles.utenPeriode
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.komposisjon.kombinerUtenNull
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.utvidelser.slåSammenLikePerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioder

fun <T : PeriodeOgBarnSkjema<T>> Collection<T>.slåSammen(): Collection<T> {
    if (this.isEmpty()) {
        return this
    }

    val kompetanseSettTidslinje: Tidslinje<Set<T>> =
        this
            .map { it.tilTidslinje() }
            .kombinerUtenNull {
                it
                    .groupingBy { it.utenBarn() }
                    .reduce { _, acc, kompetanse -> acc.leggSammenBarn(kompetanse) }
                    .values
                    .toSet()
            }

    val kompetanserSlåttSammenVertikalt =
        kompetanseSettTidslinje.tilPerioder().flatMap { periode ->
            periode.verdi?.settFomOgTom(periode) ?: emptyList()
        }

    val kompetanseSlåttSammenHorisontalt =
        kompetanserSlåttSammenVertikalt
            .groupBy { it.utenPeriode() }
            .mapValues { (_, kompetanser) -> kompetanser.tilTidslinje().slåSammenLikePerioder() }
            .mapValues { (_, tidslinje) -> tidslinje.tilPerioder() }
            .values
            .flatten()
            .mapNotNull { periode -> periode.verdi?.settFomOgTom(periode) }

    return kompetanseSlåttSammenHorisontalt
}

private fun <T : PeriodeOgBarnSkjema<T>> T.leggSammenBarn(kompetanse: T) =
    this.kopier(
        fom = this.fom,
        tom = this.tom,
        barnAktører = this.barnAktører + kompetanse.barnAktører,
    )

fun <T : PeriodeOgBarnSkjema<T>> Iterable<T>?.settFomOgTom(periode: Periode<*>) = this?.map { skjema -> skjema.settFomOgTom(periode) }

fun <T : PeriodeOgBarnSkjema<T>> T.settFomOgTom(periode: Periode<*>) =
    this.kopier(
        fom = periode.fom?.toYearMonth(),
        tom = periode.tom?.toYearMonth(),
        barnAktører = this.barnAktører,
    )
