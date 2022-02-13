package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseStatus

object KompetanseUtil {
    fun finnRestKompetanser(gammelKompetanse: Kompetanse, nyKompetanse: Kompetanse): Collection<Kompetanse> {

        val kompetanseForRestBarn = gammelKompetanse
            .copy(
                barn = gammelKompetanse.barn.minus(nyKompetanse.barn)
            ).takeIf { it.barn.size > 0 }

        val kompetanseForForegåendePerioder = gammelKompetanse
            .copy(
                fom = gammelKompetanse.fom,
                tom = nyKompetanse.fom?.minusMonths(1),
                barn = nyKompetanse.barn
            ).takeIf { it.fom != null && it.fom < it.tom }

        val kompetanseForEtterfølgendePerioder = gammelKompetanse.copy(
            fom = nyKompetanse.tom?.plusMonths(1),
            tom = gammelKompetanse.tom,
            barn = nyKompetanse.barn
        ).takeIf { it.fom != null && it.fom < it.tom }

        return listOf(kompetanseForRestBarn, kompetanseForForegåendePerioder, kompetanseForEtterfølgendePerioder)
            .filterNotNull()
            .map { it.copy(id = 0L) }
    }

    fun revurderStatus(kompetanse: Kompetanse): Kompetanse {
        val skjema = kompetanse.skjema
        val sum = (skjema?.annenForeldersAktivitet?.let { 1 } ?: 0) +
            (skjema?.barnetsBostedsland?.let { 1 } ?: 0) +
            (skjema?.primærland?.let { 1 } ?: 0) +
            (skjema?.sekundærland?.let { 1 } ?: 0) +
            (skjema?.søkersAktivitet?.let { 1 } ?: 0)

        val nyStatus = when (sum) {
            5 -> KompetanseStatus.OK
            in 1..4 -> KompetanseStatus.UFULLSTENDIG
            else -> KompetanseStatus.IKKE_UTFYLT
        }

        return kompetanse.copy(status = nyStatus)
    }

    fun mergeKompetanser(kompetanser: Collection<Kompetanse>): List<Kompetanse> {
        return mergeBarn(mergePerioder(kompetanser))
    }

    private fun mergeBarn(kompetanser: List<Kompetanse>): List<Kompetanse> {
        return kompetanser
            .groupBy { it.copy(barn = emptySet()) }
            .mapValues { (_, kompetanser) ->
                kompetanser
                    .reduce { acc, neste ->
                        neste.copy(id = 0, barn = acc.barn.union(neste.barn))
                    }
            }.values.toList()
    }

    private fun mergePerioder(
        kompetanser: Collection<Kompetanse>
    ): List<Kompetanse> {
        return kompetanser
            .groupBy { it.copy(fom = null, tom = null) }
            .mapValues { (_, kompetanser) ->
                kompetanser
                    .filter { it.fom != null }
                    .sortedBy { it.fom }
                    .fold(emptyList<Kompetanse>()) { acc, neste ->
                        if (acc.isEmpty()) {
                            listOf(neste)
                        } else {
                            val siste: Kompetanse = acc.last()
                            if (siste.tom == null)
                                acc
                            else if (siste.tom >= neste.fom?.minusMonths(1))
                                acc.subList(0, acc.size - 1) + listOf(
                                    siste.copy(
                                        id = 0,
                                        tom = neste.tom?.let { maxOf(siste.tom, it) }
                                    )
                                )
                            else
                                acc + listOf(neste)
                        }
                    }
            }.values.flatten()
    }
}
