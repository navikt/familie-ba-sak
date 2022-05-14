package no.nav.familie.ba.sak.kjerne.eøs.felles

import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.oppdaterSkjemaerRekursivt
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.slåSammen
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.somInversOppdateringEllersNull
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService

class PeriodeOgBarnSkjemaService<T : PeriodeOgBarnSkjemaEntitet<T>>(
    val periodeOgBarnSkjemaRepository: PeriodeOgBarnSkjemaRepository<T>,
    val tilbakestillBehandlingService: TilbakestillBehandlingService,
) {

    fun hentMedBehandlingId(behandlingId: Long): Collection<T> {
        return periodeOgBarnSkjemaRepository.findByBehandlingId(behandlingId)
    }

    fun hentMedId(id: Long): T {
        return periodeOgBarnSkjemaRepository.getById(id)
    }

    fun endreSkjemaer(behandlingId: Long, oppdatering: T) {
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

    fun lagreSkjemaDifferanse(gjeldende: Collection<T>, oppdaterte: Collection<T>) {
        periodeOgBarnSkjemaRepository.deleteAll(gjeldende - oppdaterte)
        periodeOgBarnSkjemaRepository.saveAll(oppdaterte - gjeldende)
    }

    private fun <T : PeriodeOgBarnSkjema<T>> Iterable<T>.erLukkingAvEnÅpenPeriode(skjema: T) =
        this.filter {
            it.tom == null && skjema.tom != null && it.kopier(tom = skjema.tom) == skjema
        }
            .singleOrNull() != null
}

fun <T : PeriodeOgBarnSkjemaEntitet<T>> Collection<T>.medBehandlingId(behandlingId: Long): Collection<T> {
    this.forEach { it.behandlingId = behandlingId }
    return this
}
