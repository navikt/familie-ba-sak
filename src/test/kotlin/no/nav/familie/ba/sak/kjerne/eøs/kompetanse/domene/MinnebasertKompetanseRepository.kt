package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene

import java.util.concurrent.atomic.AtomicLong

class MinnebasertKompetanseRepository {

    private val kompetanseLøpenummer = AtomicLong()
    private fun AtomicLong.neste() = this.addAndGet(1)

    private val kompetanser = mutableMapOf<Long, Kompetanse>()

    fun hentKompetanser(behandlingId: Long): List<Kompetanse> {

        return kompetanser.values
            .filter { it.behandlingId == behandlingId }
            .sortedBy { it.id }
    }

    fun hentKompetanse(kompetanseId: Long): Kompetanse =
        kompetanser[kompetanseId] ?: throw IllegalArgumentException("Finner ikke kompetanse for id $kompetanseId")

    fun save(kompetanser: Iterable<Kompetanse>) = kompetanser.map { save(it) }

    fun save(kompetanse: Kompetanse): Kompetanse {
        if (kompetanse.id != 0L) {
            kompetanser[kompetanse.id] = kompetanse
            return kompetanse
        } else {
            val nyId = kompetanseLøpenummer.neste()
            kompetanser[nyId] = kompetanse.also { it.id = nyId }
            return kompetanser[nyId]!!
        }
    }

    fun delete(tilSletting: Iterable<Kompetanse>) {
        tilSletting.forEach { kompetanser.remove(it.id) }
    }
}
