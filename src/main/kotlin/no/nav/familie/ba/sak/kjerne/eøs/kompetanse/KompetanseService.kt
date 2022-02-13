package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseUtil.mergeKompetanser
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseUtil.revurderStatus
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.blankUt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KompetanseService(val kompetanseRepository: MockKompetanseRepository = MockKompetanseRepository()) {

    fun hentKompetanser(behandlingId: Long): Collection<Kompetanse> {
        return kompetanseRepository.hentKompetanser(behandlingId)
    }

    @Transactional
    fun oppdaterKompetanse(oppdatertKompetanse: Kompetanse): Collection<Kompetanse> {
        val gammelKompetanse = kompetanseRepository.hentKompetanse(oppdatertKompetanse.id)
        val restKompetanser = KompetanseUtil.finnRestKompetanser(gammelKompetanse, oppdatertKompetanse)

        val revurdertKompetanse = revurderStatus(oppdatertKompetanse)
        kompetanseRepository.save(listOf(revurdertKompetanse) + restKompetanser)
        return hentKompetanser(oppdatertKompetanse.behandlingId)
    }

    @Transactional
    fun slettKompetamse(kompetanseId: Long): Collection<Kompetanse> {
        val gammelKompetanse = kompetanseRepository.hentKompetanse(kompetanseId)
        val behandlingId = gammelKompetanse.behandlingId
        val blankKompetamse = gammelKompetanse.blankUt()
        val eksisterendeKompetanser = hentKompetanser(behandlingId)

        val nyeKompetanser =
            mergeKompetanser(eksisterendeKompetanser.minus(gammelKompetanse).plus(blankKompetamse))

        val tilSletting = eksisterendeKompetanser.minus(nyeKompetanser)

        kompetanseRepository.delete(tilSletting)
        kompetanseRepository.save(nyeKompetanser)

        return hentKompetanser(behandlingId)
    }
}
