package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaService
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSeparateTidslinjerForBarna
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSkjemaer
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilpassTil
import no.nav.familie.ba.sak.kjerne.eøs.felles.medBehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.beregning.hentBarnasRegelverkResultatTidslinjer
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.beregning.tilBarnasEøsRegelverkTidslinjer
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.RegelverkResultat
import no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.TidslinjeService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KompetanseService(
    private val tidslinjeService: TidslinjeService,
    kompetanseRepository: PeriodeOgBarnSkjemaRepository<Kompetanse>,
    val tilbakestillBehandlingService: TilbakestillBehandlingService,
) {
    val serviceDelegate = PeriodeOgBarnSkjemaService(kompetanseRepository)

    fun hentKompetanser(behandlingId: Long) =
        serviceDelegate.hentMedBehandlingId(behandlingId)

    fun hentKompetanse(kompetanseId: Long) =
        serviceDelegate.hentMedId(kompetanseId)

    @Transactional
    fun endreKompetanse(behandlingId: Long, oppdatering: Kompetanse) =
        serviceDelegate.endreSkjemaer(behandlingId, oppdatering) {
            tilbakestillBehandlingService.tilbakestillBehandlingTilBehandlingsresultat(behandlingId)
        }

    @Transactional
    fun slettKompetanse(kompetanseId: Long) =
        serviceDelegate.slettSkjema(kompetanseId) {
            tilbakestillBehandlingService.tilbakestillBehandlingTilBehandlingsresultat(it)
        }

    @Transactional
    fun tilpassKompetanserTilRegelverk(behandlingId: Long) {
        val gjeldendeKompetanser = hentKompetanser(behandlingId)

        val barnasRegelverkResultatTidslinjer = tidslinjeService.hentBarnasRegelverkResultatTidslinjer(behandlingId)

        val oppdaterteKompetanser =
            tilpassKompetanserTilRegelverk(gjeldendeKompetanser, barnasRegelverkResultatTidslinjer)
                .medBehandlingId(behandlingId)

        serviceDelegate.lagreSkjemaDifferanse(gjeldendeKompetanser, oppdaterteKompetanser)
    }
}

internal fun tilpassKompetanserTilRegelverk(
    gjeldendeKompetanser: Collection<Kompetanse>,
    barnaRegelverkTidslinjer: Map<Aktør, Tidslinje<RegelverkResultat, Måned>>
): Collection<Kompetanse> {
    val barnasEøsRegelverkTidslinjer = barnaRegelverkTidslinjer.tilBarnasEøsRegelverkTidslinjer()
    return gjeldendeKompetanser.tilSeparateTidslinjerForBarna()
        .tilpassTil(barnasEøsRegelverkTidslinjer) { kompetanse, _ -> kompetanse ?: Kompetanse.NULL }
        .tilSkjemaer()
}
