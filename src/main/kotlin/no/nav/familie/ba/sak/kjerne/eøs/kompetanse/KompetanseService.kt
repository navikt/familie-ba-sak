package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.blankUt
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.util.slåSammen
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.util.tilpassKompetanserTilRegelverk
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.util.trekkFra
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer.TidslinjeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KompetanseService(
    val tidslinjeService: TidslinjeService,
    val kompetanseRepository: KompetanseRepository,
    val tilbakestillBehandlingService: TilbakestillBehandlingService,
) {

    fun hentKompetanser(behandlingId: Long): Collection<Kompetanse> {
        return kompetanseRepository.findByBehandlingId(behandlingId)
    }

    fun hentKompetanse(kompetanseId: Long): Kompetanse {
        return kompetanseRepository.getById(kompetanseId)
    }

    @Transactional
    fun oppdaterKompetanse(kompetanseId: Long, oppdatertKompetanse: Kompetanse): Collection<Kompetanse> {
        val gjeldendeKompetanse = kompetanseRepository.getById(kompetanseId)

        val kompetanserFratrukketOppdatering = gjeldendeKompetanse.trekkFra(oppdatertKompetanse)
        val oppdaterteKompetanser = (kompetanserFratrukketOppdatering + oppdatertKompetanse)
            .slåSammen().medBehandlingId(gjeldendeKompetanse.behandlingId)

        lagreKompetanseDifferanse(listOf(gjeldendeKompetanse), oppdaterteKompetanser)

        tilbakestillBehandlingService.resettStegVedEndringPåBehandlingsresultatSteg(gjeldendeKompetanse.behandlingId)

        // Denne brukes ikke av controlleren, og bør nok endres til ikke å returnere noe
        return hentKompetanser(oppdatertKompetanse.behandlingId)
    }

    @Transactional
    fun slettKompetanse(kompetanseId: Long): Collection<Kompetanse> {
        val kompetanseTilSletting = kompetanseRepository.getById(kompetanseId)
        val behandlingId = kompetanseTilSletting.behandlingId
        val gjeldendeKompetanser = hentKompetanser(behandlingId)
        val blankKompetanse = kompetanseTilSletting.blankUt()

        val oppdaterteKompetanser = gjeldendeKompetanser.minus(kompetanseTilSletting).plus(blankKompetanse)
            .slåSammen().medBehandlingId(behandlingId)

        lagreKompetanseDifferanse(gjeldendeKompetanser, oppdaterteKompetanser)

        tilbakestillBehandlingService.resettStegVedEndringPåBehandlingsresultatSteg(behandlingId)

        // Denne brukes ikke av controlleren, og bør nok endres til ikke å returnere noe
        return hentKompetanser(behandlingId)
    }

    @Transactional
    fun tilpassKompetanserTilRegelverk(behandlingId: Long): Collection<Kompetanse> {
        val gjeldendeKompetanser = hentKompetanser(behandlingId)
        val barnasRegelverkTidslinjer = tidslinjeService.hentBarnasRegelverkTidslinjer(behandlingId)

        val oppdaterteKompetanser = tilpassKompetanserTilRegelverk(gjeldendeKompetanser, barnasRegelverkTidslinjer)
            .medBehandlingId(behandlingId)

        lagreKompetanseDifferanse(gjeldendeKompetanser, oppdaterteKompetanser)

        // Denne brukes ikke av klienten, og bør nok endres til ikke å returnere noe
        return hentKompetanser(behandlingId)
    }

    private fun lagreKompetanseDifferanse(gjeldende: Collection<Kompetanse>, oppdaterte: Collection<Kompetanse>) {
        kompetanseRepository.deleteAll(gjeldende - oppdaterte)
        kompetanseRepository.saveAll(oppdaterte - gjeldende)
    }

    private fun Collection<Kompetanse>.medBehandlingId(behandlingId: Long): Collection<Kompetanse> {
        this.forEach { it.behandlingId = behandlingId }
        return this
    }

    private fun TidslinjeService.hentBarnasRegelverkTidslinjer(behandlingId: Long): Map<Aktør, Tidslinje<Regelverk, Måned>> =
        this.hentTidslinjer(behandlingId).barnasTidslinjer()
            .mapValues { (_, tidslinjer) -> tidslinjer.regelverkTidslinje }
            .mapKeys { (aktør, _) -> aktør }
}
