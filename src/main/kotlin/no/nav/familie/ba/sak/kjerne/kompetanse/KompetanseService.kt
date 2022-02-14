package no.nav.familie.ba.sak.kjerne.kompetanse

import no.nav.familie.ba.sak.kjerne.kompetanse.domene.Kompetanse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KompetanseService(val kompetanseRepository: MockKompetanseRepository) {

    fun hentKompetanser(behandlingId: Long): Collection<Kompetanse> {
        return kompetanseRepository.hentKompetanser(behandlingId)
    }

    @Transactional
    fun oppdaterKompetanse(nyKompetanse: Kompetanse): Collection<Kompetanse> {
        val gammelKompetanse = kompetanseRepository.hentKompetanse(nyKompetanse.id)
        val restKompetanser = KompetanseUtil.finnRestKompetanser(gammelKompetanse, nyKompetanse)

        return kompetanseRepository.save(listOf(nyKompetanse) + restKompetanser)
    }
}
