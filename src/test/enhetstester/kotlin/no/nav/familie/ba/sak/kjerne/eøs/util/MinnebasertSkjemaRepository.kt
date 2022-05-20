package no.nav.familie.ba.sak.kjerne.eøs.util

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEntitet
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
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

    fun deleteAll() {
        skjemaer.clear()
    }
}

fun <S : PeriodeOgBarnSkjemaEntitet<S>> mockPeriodeBarnRepository(): PeriodeOgBarnSkjemaRepository<S> {

    val minnebasertSkjemaRepository = MinnebasertSkjemaRepository<S>()
    val mockSkjemaRepository = mockk<PeriodeOgBarnSkjemaRepository<S>>()

    val idSlot = slot<Long>()
    val skjemaListeSlot = slot<Iterable<S>>()

    every { mockSkjemaRepository.findByBehandlingId(capture(idSlot)) } answers {
        minnebasertSkjemaRepository.hentSkjemaer(idSlot.captured)
    }

    every { mockSkjemaRepository.getById(capture(idSlot)) } answers {
        minnebasertSkjemaRepository.hentSkjema(idSlot.captured)
    }

    every { mockSkjemaRepository.saveAll(capture(skjemaListeSlot)) } answers {
        minnebasertSkjemaRepository.save(skjemaListeSlot.captured)
    }

    every { mockSkjemaRepository.deleteAll(capture(skjemaListeSlot)) } answers {
        minnebasertSkjemaRepository.delete(skjemaListeSlot.captured)
    }

    every { mockSkjemaRepository.deleteAll() } answers {
        minnebasertSkjemaRepository.deleteAll()
    }

    return mockSkjemaRepository
}
