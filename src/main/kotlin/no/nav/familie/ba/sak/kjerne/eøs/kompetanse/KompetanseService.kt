package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.beregning.AktørId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.blankUt
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.util.revurderStatus
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.util.slåSammen
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.util.tilpassKompetanserTilRegelverk
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.util.trekkFra
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer.TidslinjeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KompetanseService(
    val tidslinjeService: TidslinjeService,
    val kompetanseRepository: MockKompetanseRepository = MockKompetanseRepository()
) {

    fun hentKompetanser(behandlingId: Long): Collection<Kompetanse> {
        return kompetanseRepository.hentKompetanser(behandlingId)
    }

    @Transactional
    fun oppdaterKompetanse(oppdatertKompetanse: Kompetanse): Collection<Kompetanse> {
        val gammelKompetanse = kompetanseRepository.hentKompetanse(oppdatertKompetanse.id)

        validerOppdatering(oppdatertKompetanse, gammelKompetanse)

        val restKompetanser = gammelKompetanse.trekkFra(oppdatertKompetanse)
        val revurderteKompetanser =
            (restKompetanser + oppdatertKompetanse)
                .slåSammen().vurderStatus().medBehandlingId(gammelKompetanse.behandlingId)

        val tilOppretting = revurderteKompetanser - gammelKompetanse
        val tilSletting = listOf(gammelKompetanse) - revurderteKompetanser

        if (tilOppretting != tilSletting) {
            kompetanseRepository.delete(tilSletting)
            kompetanseRepository.save(tilOppretting)
        }
        return hentKompetanser(oppdatertKompetanse.behandlingId)
    }

    @Transactional
    fun slettKompetanse(kompetanseId: Long): Collection<Kompetanse> {
        val gammelKompetanse = kompetanseRepository.hentKompetanse(kompetanseId)
        val behandlingId = gammelKompetanse.behandlingId
        val eksisterendeKompetanser = hentKompetanser(behandlingId)
        val blankKompetamse = gammelKompetanse.blankUt()

        val revurderteKompetanser =
            eksisterendeKompetanser.minus(gammelKompetanse).plus(blankKompetamse)
                .slåSammen().vurderStatus().medBehandlingId(behandlingId)

        val tilOppretting = revurderteKompetanser.trekkFra(eksisterendeKompetanser)
        val tilSletting = eksisterendeKompetanser.trekkFra(revurderteKompetanser)

        if (tilOppretting != tilSletting) {
            kompetanseRepository.delete(tilSletting)
            kompetanseRepository.save(tilOppretting)
        }

        return hentKompetanser(behandlingId)
    }

    @Transactional
    fun tilpassKompetanserTilEøsPerioder(behandlingId: Long): Collection<Kompetanse> {
        val kompetanser = hentKompetanser(behandlingId)
        val barnasRegelverkTidslinjer = tidslinjeService.hentBarnasRegelverkTidslinjer(behandlingId)

        val tilpassedeKompetanser = tilpassKompetanserTilRegelverk(kompetanser, barnasRegelverkTidslinjer)

        val tilOppretting = tilpassedeKompetanser.trekkFra(kompetanser)
            .medBehandlingId(behandlingId).vurderStatus()
        val tilSletting = kompetanser.trekkFra(tilpassedeKompetanser)

        if (tilOppretting != tilSletting) {
            kompetanseRepository.delete(tilSletting)
            kompetanseRepository.save(tilOppretting)
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

    private fun Collection<Kompetanse>.medBehandlingId(behandlingId: Long): Collection<Kompetanse> {
        this.forEach { it.behandlingId = behandlingId }
        return this
    }

    private fun Collection<Kompetanse>.vurderStatus(): Collection<Kompetanse> =
        this.map { revurderStatus(it) }

    private fun TidslinjeService.hentBarnasRegelverkTidslinjer(behandlingId: Long): Map<AktørId, Tidslinje<Regelverk, Måned>> =
        this.hentTidslinjerThrows(behandlingId).barnasTidslinjer()
            .mapValues { (_, tidslinjer) -> tidslinjer.regelverkTidslinje }
            .mapKeys { (aktør, _) -> aktør.aktørId }
}
