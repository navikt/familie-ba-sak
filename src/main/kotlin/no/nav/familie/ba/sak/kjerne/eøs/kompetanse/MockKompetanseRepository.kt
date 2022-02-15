package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseSkjema
import org.springframework.stereotype.Repository
import java.time.YearMonth

@Repository
class MockKompetanseRepository {

    val barn1 = "111111"
    val barn2 = "222222"
    val barn3 = "333333"

    private val malKompetanse = Kompetanse(
        id = 1L,
        fom = YearMonth.of(2021, 2),
        tom = YearMonth.of(2021, 11),
        barn = setOf(barn1, barn2, barn3),
        skjema = KompetanseSkjema()
    )

    private val kompetanser = mutableMapOf(
        Pair(
            1L,
            malKompetanse.copy(id = 1)
        )
    )

    fun hentKompetanser(behandlingId: Long): Collection<Kompetanse> = kompetanser.values
        .map { it.copy(behandlingId = behandlingId) }

    fun hentKompetanse(kompetanseId: Long): Kompetanse =
        kompetanser[kompetanseId] ?: throw IllegalArgumentException("Finner ikke kompetanse for id $kompetanseId")

    fun save(kompetanser: Iterable<Kompetanse>) = kompetanser.map { save(it) }

    fun save(kompetanse: Kompetanse): Kompetanse {
        if (kompetanse.id != 0L) {
            kompetanser[kompetanse.id] = kompetanse
            return kompetanse
        } else {
            val nyId = kompetanser.keys.maxOf { it } + 1
            kompetanser[nyId] = kompetanse.copy(id = nyId)
            return kompetanser[nyId]!!
        }
    }

    fun delete(tilSletting: List<Kompetanse>) {
        tilSletting.forEach { kompetanser.remove(it.id) }
    }
}
