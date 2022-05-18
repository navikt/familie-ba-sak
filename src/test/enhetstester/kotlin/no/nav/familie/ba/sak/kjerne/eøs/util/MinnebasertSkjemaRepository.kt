package no.nav.familie.ba.sak.kjerne.eøs.util

import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEntitet
import java.util.concurrent.atomic.AtomicLong

class MinnebasertSkjemaRepository<S> where S : PeriodeOgBarnSkjemaEntitet<S> {

    private val løpenummer = AtomicLong()
    private fun AtomicLong.neste() = this.addAndGet(1)

    private val skjemaer = mutableMapOf<Long, S>()

    fun hentSkjemaer(behandlingId: Long): List<S> {

        return skjemaer.values
            .filter { it.behandlingId == behandlingId }
            .sortedBy { it.id }
    }

    fun hentSkjema(skjemaId: Long): S =
        skjemaer[skjemaId] ?: throw IllegalArgumentException("Finner ikke skjema for id $skjemaId")

    fun save(skjemaer: Iterable<S>) = skjemaer.map { save(it) }

    fun save(skjema: S): S {
        if (skjema.id != 0L) {
            skjemaer[skjema.id] = skjema
            return skjema
        } else {
            val nyId = løpenummer.neste()
            skjemaer[nyId] = skjema.also { it.id = nyId }
            return skjemaer[nyId]!!
        }
    }

    fun delete(tilSletting: Iterable<S>) {
        tilSletting.forEach { skjemaer.remove(it.id) }
    }
}
