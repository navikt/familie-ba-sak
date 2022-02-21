package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import org.springframework.stereotype.Repository
import java.time.YearMonth
import java.util.concurrent.atomic.AtomicLong

@Repository
class MockKompetanseRepository {

    val barn1 = "111111"
    val barn2 = "222222"
    val barn3 = "333333"

    val kompetanseLøpenummer = AtomicLong()
    fun AtomicLong.neste() = this.addAndGet(1)

    private val malKompetanse = Kompetanse(
        fom = YearMonth.of(2021, 2),
        tom = YearMonth.of(2021, 11),
        barn = setOf(barn1, barn2, barn3),
    )

    private val kompetanser = mutableMapOf<Long, Kompetanse>()

    fun hentKompetanser(behandlingId: Long): Collection<Kompetanse> {

        val kompetanserForBehandling = kompetanser.values.filter { it.behandlingId == behandlingId }
        if (kompetanserForBehandling.size == 0) {
            val kompetanse = malKompetanse.copy(id = kompetanseLøpenummer.neste(), behandlingId = behandlingId)
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
            kompetanser[nyId] = kompetanse.copy(id = nyId)
            return kompetanser[nyId]!!
        }
    }

    fun delete(tilSletting: List<Kompetanse>) {
        tilSletting.forEach { kompetanser.remove(it.id) }
    }
}
