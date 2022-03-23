package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseStatus

object KompetanseUtil {
    fun finnRestKompetanser(gammelKompetanse: Kompetanse, nyKompetanse: Kompetanse): Collection<Kompetanse> {

        val kompetanseForRestBarn = gammelKompetanse
            .copy(
                barnAktørIder = gammelKompetanse.barnAktørIder.minus(nyKompetanse.barnAktørIder)
            ).takeIf { it.barnAktørIder.size > 0 }

        val kompetanseForForegåendePerioder = gammelKompetanse
            .copy(
                fom = gammelKompetanse.fom,
                tom = nyKompetanse.fom?.minusMonths(1),
                barnAktørIder = nyKompetanse.barnAktørIder
            ).takeIf { it.fom != null && it.fom < it.tom }

        val kompetanseForEtterfølgendePerioder = gammelKompetanse.copy(
            fom = nyKompetanse.tom?.plusMonths(1),
            tom = gammelKompetanse.tom,
            barnAktørIder = nyKompetanse.barnAktørIder
        ).takeIf { it.fom != null && it.fom < it.tom }

        return listOf(kompetanseForRestBarn, kompetanseForForegåendePerioder, kompetanseForEtterfølgendePerioder)
            .filterNotNull()
    }

    fun revurderStatus(kompetanser: List<Kompetanse>): List<Kompetanse> =
        kompetanser.map { revurderStatus(it) }

    fun revurderStatus(kompetanse: Kompetanse): Kompetanse {
        val sum = (kompetanse.annenForeldersAktivitet?.let { 1 } ?: 0) +
            (kompetanse.barnetsBostedsland?.let { 1 } ?: 0) +
            (kompetanse.primærland?.let { 1 } ?: 0) +
            (kompetanse.sekundærland?.let { 1 } ?: 0) +
            (kompetanse.søkersAktivitet?.let { 1 } ?: 0)

        val nyStatus = when (sum) {
            5 -> KompetanseStatus.OK
            in 1..4 -> KompetanseStatus.UFULLSTENDIG
            else -> KompetanseStatus.IKKE_UTFYLT
        }

        return kompetanse.copy(status = nyStatus)
    }

    fun mergeKompetanser(kompetanser: Collection<Kompetanse>): Collection<Kompetanse> {
        return mergePerioder(mergeBarn(kompetanser))
    }

    private fun mergeBarn(kompetanser: Collection<Kompetanse>): Collection<Kompetanse> {
        return kompetanser
            // Ta bort barn og id, slik at vi grupperer på like perioder og likt innhold
            .groupBy { it.copy(barnAktørIder = emptySet()) }
            .mapValues { (_, kompetanser) ->
                kompetanser
                    .reduce { acc, neste ->
                        neste.copy(barnAktørIder = acc.barnAktørIder.union(neste.barnAktørIder))
                    }
            }.values
    }

    private fun mergePerioder(
        kompetanser: Collection<Kompetanse>
    ): Collection<Kompetanse> {
        val nøytralisertePerioder = kompetanser
            .groupBy { it.copy(fom = null, tom = null) }
        return nøytralisertePerioder
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
                                    siste.copy(tom = neste.tom?.let { maxOf(siste.tom, it) })
                                )
                            else
                                acc + listOf(neste)
                        }
                    }
            }.values.flatten()
    }
}
