package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaService
import no.nav.familie.ba.sak.kjerne.eøs.felles.medBehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.beregning.hentBarnasRegelverkResultatTidslinjer
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.beregning.tilpassKompetanserTilRegelverk
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjeService
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KompetanseService(
    private val vilkårsvurderingTidslinjeService: VilkårsvurderingTidslinjeService,
    kompetanseRepository: PeriodeOgBarnSkjemaRepository<Kompetanse>,
    val tilbakestillBehandlingService: TilbakestillBehandlingService,
) {
    val skjemaService = PeriodeOgBarnSkjemaService(kompetanseRepository)

    fun hentKompetanser(behandlingId: Long) =
        skjemaService.hentMedBehandlingId(behandlingId)

    fun hentKompetanse(kompetanseId: Long) =
        skjemaService.hentMedId(kompetanseId)

    @Transactional
    fun endreKompetanse(behandlingId: Long, oppdatering: Kompetanse) =
        skjemaService.endreSkjemaer(behandlingId, oppdatering) {
            tilbakestillBehandlingService.tilbakestillBehandlingTilBehandlingsresultat(behandlingId)
        }

    @Transactional
    fun slettKompetanse(kompetanseId: Long) =
        skjemaService.slettSkjema(kompetanseId) {
            tilbakestillBehandlingService.tilbakestillBehandlingTilBehandlingsresultat(it)
        }

    @Transactional
    fun tilpassKompetanserTilRegelverk(behandlingId: Long) {
        val gjeldendeKompetanser = hentKompetanser(behandlingId)
        val barnasRegelverkResultatTidslinjer =
            vilkårsvurderingTidslinjeService.hentBarnasRegelverkResultatTidslinjer(behandlingId)

        val oppdaterteKompetanser = tilpassKompetanserTilRegelverk(
            gjeldendeKompetanser,
            barnasRegelverkResultatTidslinjer
        ).medBehandlingId(behandlingId)

        skjemaService.lagreSkjemaDifferanse(gjeldendeKompetanser, oppdaterteKompetanser)
    }

    @Transactional
    fun kopierOgErstattKompetanser(fraBehandlingId: Long, tilBehandlingId: Long) =
        skjemaService.kopierOgErstattSkjemaer(fraBehandlingId, tilBehandlingId)
}
