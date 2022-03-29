package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene

import no.nav.familie.ba.sak.kjerne.personident.Aktør
import java.time.YearMonth
import java.util.concurrent.atomic.AtomicLong

class MinnebasertKompetanseRepository {

    val barn1 = Aktør("1111111111111")
    val barn2 = Aktør("2222222222222")
    val barn3 = Aktør("3333333333333")

    val kompetanseLøpenummer = AtomicLong()
    fun AtomicLong.neste() = this.addAndGet(1)

    private val malKompetanse = Kompetanse(
        fom = YearMonth.of(2021, 2),
        tom = YearMonth.of(2021, 11),
        barnAktører = setOf(barn1, barn2, barn3),
    )

    private val kompetanser = mutableMapOf<Long, Kompetanse>()

    fun hentKompetanser(behandlingId: Long): List<Kompetanse> {

        val kompetanserForBehandling = kompetanser.values.filter { it.behandlingId == behandlingId }
        if (kompetanserForBehandling.size == 0) {
            val kompetanse =
                malKompetanse.copy()
                    .also { it.behandlingId = behandlingId }
                    .also { it.id = kompetanseLøpenummer.neste() }
            kompetanser[kompetanse.id] = kompetanse
        }

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
            kompetanser[nyId] = kompetanse.copy().also { it.id = nyId }
            return kompetanser[nyId]!!
        }
    }

    fun delete(tilSletting: Iterable<Kompetanse>) {
        tilSletting.forEach { kompetanser.remove(it.id) }
    }
}
