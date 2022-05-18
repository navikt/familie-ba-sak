package no.nav.familie.ba.sak.kjerne.eøs.felles

import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.oppdaterSkjemaerRekursivt
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.slåSammen
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.somInversOppdateringEllersNull
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilpassSkjemaerTilTidslinjer
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned

class PeriodeOgBarnSkjemaService<S : PeriodeOgBarnSkjemaEntitet<S>>(
    val periodeOgBarnSkjemaRepository: PeriodeOgBarnSkjemaRepository<S>,
    val tilbakestillBehandlingService: TilbakestillBehandlingService,
) {

    fun hentMedBehandlingId(behandlingId: Long): Collection<S> {
        return periodeOgBarnSkjemaRepository.findByBehandlingId(behandlingId)
    }

    fun hentMedId(id: Long): S {
        return periodeOgBarnSkjemaRepository.getById(id)
    }

    fun endreSkjemaer(behandlingId: Long, oppdatering: S) {
        val gjeldendeSkjemaer = hentMedBehandlingId(behandlingId)

        val justertOppdatering = oppdatering.somInversOppdateringEllersNull(gjeldendeSkjemaer) ?: oppdatering
        val oppdaterteKompetanser = oppdaterSkjemaerRekursivt(gjeldendeSkjemaer, justertOppdatering)

        lagreSkjemaDifferanse(gjeldendeSkjemaer, oppdaterteKompetanser.medBehandlingId(behandlingId))
        tilbakestillBehandlingService.tilbakestillBehandlingTilBehandlingsresultat(behandlingId)
    }

    fun slettSkjema(skjemaId: Long) {
        val kompetanseTilSletting = periodeOgBarnSkjemaRepository.getById(skjemaId)
        val behandlingId = kompetanseTilSletting.behandlingId
        val gjeldendeKompetanser = hentMedBehandlingId(behandlingId)
        val blankKompetanse = kompetanseTilSletting.utenSkjema()

        val oppdaterteKompetanser = gjeldendeKompetanser.minus(kompetanseTilSletting).plus(blankKompetanse)
            .slåSammen().medBehandlingId(behandlingId)

        lagreSkjemaDifferanse(gjeldendeKompetanser, oppdaterteKompetanser)

        tilbakestillBehandlingService.tilbakestillBehandlingTilBehandlingsresultat(behandlingId)
    }

    fun kopierOgErstattSkjemaer(fraBehandlingId: Long, tilBehandlingId: Long) {
        val gjeldendeTilSkjemaer = hentMedBehandlingId(tilBehandlingId)
        val kopiAvFraSkjemaer = hentMedBehandlingId(fraBehandlingId)
            .map { it.kopier() }
            .medBehandlingId(tilBehandlingId)

        lagreSkjemaDifferanse(gjeldendeTilSkjemaer, kopiAvFraSkjemaer)
    }

    fun <I> tilpassBarnasSkjemaerTilTidslinjer(
        behandlingId: Long,
        barnasTidslinjer: Map<Aktør, Tidslinje<I, Måned>>,
        tomtSkjemaForBarnFactory: (Aktør) -> S
    ) {
        val gjeldendeSkjemaer = hentMedBehandlingId(behandlingId)
        val oppdaterteSkjemaer =
            tilpassSkjemaerTilTidslinjer(gjeldendeSkjemaer, barnasTidslinjer, tomtSkjemaForBarnFactory)

        lagreSkjemaDifferanse(gjeldendeSkjemaer, oppdaterteSkjemaer.medBehandlingId(behandlingId))
    }

    fun lagreSkjemaDifferanse(gjeldende: Collection<S>, oppdaterte: Collection<S>) {
        periodeOgBarnSkjemaRepository.deleteAll(gjeldende - oppdaterte)
        periodeOgBarnSkjemaRepository.saveAll(oppdaterte - gjeldende)
    }
}

fun <T : PeriodeOgBarnSkjemaEntitet<T>> Collection<T>.medBehandlingId(behandlingId: Long): Collection<T> {
    this.forEach { it.behandlingId = behandlingId }
    return this
}
