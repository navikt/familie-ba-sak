package no.nav.familie.ba.sak.kjerne.eøs.felles.beregning

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjema
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEntitet
import no.nav.familie.ba.sak.kjerne.eøs.felles.utenPeriode
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunktEllerUendeligSent
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunktEllerUendeligTidlig
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.familie.tidslinje.Periode as FamilieFellesPeriode
import no.nav.familie.tidslinje.Tidslinje as FamilieFellesTidslinje

fun <S : PeriodeOgBarnSkjema<S>> S.tilTidslinje() = listOf(this).tilTidslinje()

internal fun <S : PeriodeOgBarnSkjema<S>> Iterable<S>.tilTidslinje() =
    tidslinje {
        this.map {
            Periode(
                it.fom.tilTidspunktEllerUendeligTidlig(),
                it.tom.tilTidspunktEllerUendeligSent(),
                it.utenPeriode(),
            )
        }
    }

fun <S : PeriodeOgBarnSkjema<S>> Iterable<S>.tilSeparateFamilieFellesTidslinjerForBarna(): Map<Aktør, FamilieFellesTidslinje<S>> {
    val skjemaer = this
    if (skjemaer.toList().isEmpty()) return emptyMap()

    val alleBarnAktørIder = skjemaer.map { it.barnAktører }.reduce { akk, neste -> akk + neste }

    return alleBarnAktørIder.associateWith { aktør ->
        skjemaer
            .filter { it.barnAktører.contains(aktør) }
            .map {
                FamilieFellesPeriode(
                    fom = it.fom?.førsteDagIInneværendeMåned(),
                    tom = it.tom?.sisteDagIInneværendeMåned(),
                    verdi = it.kopier(fom = null, tom = null, barnAktører = setOf(aktør)),
                )
            }.tilTidslinje()
    }
}

fun <S : PeriodeOgBarnSkjemaEntitet<S>> Map<Aktør, FamilieFellesTidslinje<S>>.tilSkjemaer() =
    this
        .flatMap { (aktør, tidslinjer) -> tidslinjer.tilSkjemaer(aktør) }
        .slåSammen()

private fun <S : PeriodeOgBarnSkjema<S>> FamilieFellesTidslinje<S>.tilSkjemaer(aktør: Aktør) =
    this.tilPerioder().mapNotNull { periode ->
        periode.verdi?.kopier(
            fom = periode.fom?.toYearMonth(),
            tom = periode.tom?.toYearMonth(),
            barnAktører = setOf(aktør),
        )
    }
