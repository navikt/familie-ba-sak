package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.inneholder
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Uendelighet
import java.time.YearMonth

fun Kompetanse.minus(oppdatertKompetanse: Kompetanse): Collection<Kompetanse> {
    val gammelKompetanse = this
    val kompetanseForRestBarn = gammelKompetanse
        .copy(
            barnAktørIder = gammelKompetanse.barnAktørIder.minus(oppdatertKompetanse.barnAktørIder)
        ).takeIf { it.barnAktørIder.size > 0 }

    val kompetanseForForegåendePerioder = gammelKompetanse
        .copy(
            fom = gammelKompetanse.fom,
            tom = oppdatertKompetanse.fom?.minusMonths(1),
            barnAktørIder = oppdatertKompetanse.barnAktørIder
        ).takeIf { it.fom != null && it.fom < it.tom }

    val kompetanseForEtterfølgendePerioder = gammelKompetanse.copy(
        fom = oppdatertKompetanse.tom?.plusMonths(1),
        tom = gammelKompetanse.tom,
        barnAktørIder = oppdatertKompetanse.barnAktørIder
    ).takeIf { it.fom != null && it.fom < it.tom }

    return listOf(kompetanseForRestBarn, kompetanseForForegåendePerioder, kompetanseForEtterfølgendePerioder)
        .filterNotNull()
}

fun Iterable<Kompetanse>.minus(skalFjernes: Iterable<Kompetanse>) =
    this.flatMap { kompetanse ->
        skalFjernes.find { kompetanse.inneholder(it) }?.let { kompetanse.minus(it) } ?: listOf(kompetanse)
    }

internal fun YearMonth?.tilTidspunktEllerUendeligLengeSiden(default: () -> YearMonth) =
    this.tilTidspunktEllerUendelig(default, Uendelighet.FORTID)

internal fun YearMonth?.tilTidspunktEllerUendeligLengeTil(default: () -> YearMonth) =
    this.tilTidspunktEllerUendelig(default, Uendelighet.FREMTID)

private fun YearMonth?.tilTidspunktEllerUendelig(default: () -> YearMonth, uendelighet: Uendelighet) =
    this?.let { MånedTidspunkt(it, Uendelighet.INGEN) } ?: MånedTidspunkt(default(), uendelighet)
