package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.beregning

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.MAX_MÅNED
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.inneholder

/**
 * Reduser innholdet i this-kompetansen med innholdet i oppdaterKompetanse
 * En viktig forutsetning er at oppdatertKompetanse alltid er "mindre" enn kompetansen som reduseres
 */
fun Kompetanse.trekkFra(oppdatertKompetanse: Kompetanse): Collection<Kompetanse> {

    val gammelKompetanse = this
    val kompetanseForRestBarn = gammelKompetanse
        .copy(
            barnAktører = gammelKompetanse.barnAktører.minus(oppdatertKompetanse.barnAktører)
        ).takeIf { it.barnAktører.isNotEmpty() }

    val kompetanseForForegåendePerioder = gammelKompetanse
        .copy(
            fom = gammelKompetanse.fom,
            tom = oppdatertKompetanse.fom?.minusMonths(1),
            barnAktører = oppdatertKompetanse.barnAktører
        ).takeIf { it.fom != null && it.fom <= it.tom }

    val kompetanseForEtterfølgendePerioder = gammelKompetanse.copy(
        fom = oppdatertKompetanse.tom?.plusMonths(1),
        tom = gammelKompetanse.tom,
        barnAktører = oppdatertKompetanse.barnAktører
    ).takeIf { it.fom != null && it.fom <= (it.tom ?: MAX_MÅNED) }

    return listOfNotNull(kompetanseForRestBarn, kompetanseForForegåendePerioder, kompetanseForEtterfølgendePerioder)
}

fun Iterable<Kompetanse>.trekkFra(skalFjernes: Kompetanse) =
    this.flatMap { kompetanse ->
        if (kompetanse.inneholder(skalFjernes))
            kompetanse.trekkFra(skalFjernes)
        else
            listOf(kompetanse)
    }
