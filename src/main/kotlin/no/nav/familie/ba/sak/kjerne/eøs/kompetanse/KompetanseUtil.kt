package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse

object KompetanseUtil {
    fun finnRestKompetanser(gammelKompetanse: Kompetanse, nyKompetanse: Kompetanse): Collection<Kompetanse> {

        val kompetanseForRestBarn = gammelKompetanse
            .copy(
                barn = gammelKompetanse.barn.minus(nyKompetanse.barn)
            ).takeIf { it.barn.size > 0 }

        val kompetanseForForegåendePerioder = gammelKompetanse
            .copy(
                fom = gammelKompetanse.fom,
                tom = nyKompetanse.fom.minusMonths(1),
                barn = nyKompetanse.barn
            ).takeIf { it.fom < it.tom }

        val kompetanseForEtterfølgendePerioder = gammelKompetanse.copy(
            fom = nyKompetanse.tom.plusMonths(1),
            tom = gammelKompetanse.tom,
            barn = nyKompetanse.barn
        ).takeIf { it.fom < it.tom }

        return listOf(kompetanseForRestBarn, kompetanseForForegåendePerioder, kompetanseForEtterfølgendePerioder)
            .filterNotNull()
            .map { it.copy(id = 0L) }
    }
}
