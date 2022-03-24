package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseUtil.revurderStatus
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseUtil.slåSammenKompetanser
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.blankUt
import org.springframework.http.HttpStatus
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

        validerOppdatering(oppdatertKompetanse, gammelKompetanse)

        val restKompetanser = KompetanseUtil.finnRestKompetanser(gammelKompetanse, oppdatertKompetanse)

        val revurderteKompetanser = slåSammenKompetanser(revurderStatus(restKompetanser + oppdatertKompetanse))
        val tilLagring = revurderteKompetanser - gammelKompetanse
        val tilSletting = listOf(gammelKompetanse) - revurderteKompetanser

        if (tilLagring != tilSletting) {
            kompetanseRepository.delete(tilSletting)
            kompetanseRepository.save(tilLagring)
        }
        return hentKompetanser(oppdatertKompetanse.behandlingId)
    }

    @Transactional
    fun slettKompetanse(kompetanseId: Long): Collection<Kompetanse> {
        val gammelKompetanse = kompetanseRepository.hentKompetanse(kompetanseId)
        val behandlingId = gammelKompetanse.behandlingId
        val eksisterendeKompetanser = hentKompetanser(behandlingId)
        val blankKompetamse = gammelKompetanse.blankUt()

        val oppdaterteKompetanser =
            eksisterendeKompetanser.minus(gammelKompetanse).plus(blankKompetamse)

        val revurderteKompetanser = slåSammenKompetanser(revurderStatus(oppdaterteKompetanser))
        val tilLagring = revurderteKompetanser.minus(eksisterendeKompetanser)
        val tilSletting = eksisterendeKompetanser.minus(revurderteKompetanser)

        if (tilLagring != tilSletting) {
            kompetanseRepository.delete(tilSletting)
            kompetanseRepository.save(tilLagring)
        }

        return hentKompetanser(behandlingId)
    }

    private fun validerOppdatering(oppdatertKompetanse: Kompetanse, gammelKompetanse: Kompetanse) {
        if (oppdatertKompetanse.fom == null)
            throw Feil("Manglende fra-og-med", httpStatus = HttpStatus.BAD_REQUEST)
        if (oppdatertKompetanse.fom > oppdatertKompetanse.tom)
            throw Feil("Fra-og-med er etter til-og-med", httpStatus = HttpStatus.BAD_REQUEST)
        if (oppdatertKompetanse.barnAktørIder.size == 0)
            throw Feil("Mangler barn", httpStatus = HttpStatus.BAD_REQUEST)
        if (oppdatertKompetanse.fom < gammelKompetanse.fom)
            throw Feil("Setter fra-og-med tidligere", httpStatus = HttpStatus.BAD_REQUEST)
        if (!gammelKompetanse.barnAktørIder.containsAll(oppdatertKompetanse.barnAktørIder))
            throw Feil("Oppdaterer barn som ikke er knyttet til kompetansen", httpStatus = HttpStatus.BAD_REQUEST)
    }
}
