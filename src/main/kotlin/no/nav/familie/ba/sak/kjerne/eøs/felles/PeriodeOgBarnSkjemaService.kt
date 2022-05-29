package no.nav.familie.ba.sak.kjerne.eøs.felles

import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.oppdaterSkjemaerRekursivt
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.slåSammen
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.somInversOppdateringEllersNull

class PeriodeOgBarnSkjemaService<S : PeriodeOgBarnSkjemaEntitet<S>>(
    val periodeOgBarnSkjemaRepository: PeriodeOgBarnSkjemaRepository<S>,
    val endringsabonnenter: Collection<PeriodeOgBarnSkjemaEndringAbonnent<S>>
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
    }

    fun slettSkjema(skjemaId: Long) {
        val skjemaTilSletting = periodeOgBarnSkjemaRepository.getById(skjemaId)
        val behandlingId = skjemaTilSletting.behandlingId
        val gjeldendeSkjemaer = hentMedBehandlingId(behandlingId)
        val blanktSkjema = skjemaTilSletting.utenInnhold()

        val oppdaterteKompetanser = gjeldendeSkjemaer.minus(skjemaTilSletting).plus(blanktSkjema)
            .slåSammen().medBehandlingId(behandlingId)

        lagreSkjemaDifferanse(gjeldendeSkjemaer, oppdaterteKompetanser)
    }

    fun kopierOgErstattSkjemaer(fraBehandlingId: Long, tilBehandlingId: Long) {
        val gjeldendeTilSkjemaer = hentMedBehandlingId(tilBehandlingId)
        val kopiAvFraSkjemaer = hentMedBehandlingId(fraBehandlingId)
            .map { it.kopier() }
            .medBehandlingId(tilBehandlingId)

        lagreSkjemaDifferanse(gjeldendeTilSkjemaer, kopiAvFraSkjemaer)
    }

    fun lagreSkjemaDifferanse(gjeldende: Collection<S>, oppdaterte: Collection<S>) {
        val skalSlettes = gjeldende - oppdaterte
        val skalLagres = oppdaterte - gjeldende

        periodeOgBarnSkjemaRepository.deleteAll(skalSlettes)
        periodeOgBarnSkjemaRepository.saveAll(skalLagres)

        val endringer = skalSlettes + skalLagres
        if (endringer.isNotEmpty()) {
            val behandlingId = endringer.first().behandlingId
            endringsabonnenter.forEach { it.skjemaerEndret(behandlingId, oppdaterte) }
        }
    }
}

fun <T : PeriodeOgBarnSkjemaEntitet<T>> Collection<T>.medBehandlingId(behandlingId: Long): Collection<T> {
    this.forEach { it.behandlingId = behandlingId }
    return this
}
