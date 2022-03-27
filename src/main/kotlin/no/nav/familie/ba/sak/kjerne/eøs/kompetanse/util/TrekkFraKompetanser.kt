package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.util

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.inneholder

fun Kompetanse.trekkFra(oppdatertKompetanse: Kompetanse): Collection<Kompetanse> {
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
        ).takeIf { it.fom != null && it.fom <= it.tom }

    val kompetanseForEtterfølgendePerioder = gammelKompetanse.copy(
        fom = oppdatertKompetanse.tom?.plusMonths(1),
        tom = gammelKompetanse.tom,
        barnAktørIder = oppdatertKompetanse.barnAktørIder
    ).takeIf { it.fom != null && it.fom <= it.tom }

    return listOf(kompetanseForRestBarn, kompetanseForForegåendePerioder, kompetanseForEtterfølgendePerioder)
        .filterNotNull()
}

fun Iterable<Kompetanse>.trekkFra(skalFjernes: Iterable<Kompetanse>) =
    this.flatMap { kompetanse ->
        skalFjernes.find { kompetanse.inneholder(it) }?.let { kompetanse.trekkFra(it) } ?: listOf(kompetanse)
    }

fun Iterable<Kompetanse>.trekkFra(skalFjernes: Kompetanse) =
    this.flatMap { kompetanse ->
        if (kompetanse.inneholder(skalFjernes))
            kompetanse.trekkFra(skalFjernes)
        else
            listOf(kompetanse)
    }
