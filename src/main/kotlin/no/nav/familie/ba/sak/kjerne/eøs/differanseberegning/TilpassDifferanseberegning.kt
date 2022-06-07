package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseEndretAbonnent
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEndringAbonnent
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import org.springframework.stereotype.Service

@Service
class TilpassDifferanseberegningEtterTilkjentYtelseService(
    private val valutakursRepository: PeriodeOgBarnSkjemaRepository<Valutakurs>,
    private val utenlandskPeriodebeløpRepository: PeriodeOgBarnSkjemaRepository<UtenlandskPeriodebeløp>,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository
) : TilkjentYtelseEndretAbonnent {
    override fun endretTilkjentYtelse(tilkjentYtelse: TilkjentYtelse) {
        val behandlingId = BehandlingId(tilkjentYtelse.behandling.id)
        val valutakurser = valutakursRepository.finnFraBehandlingId(behandlingId.id)
        val utenlandskePeriodebeløp = utenlandskPeriodebeløpRepository.finnFraBehandlingId(behandlingId.id)

        val nyTilkjentYtelse = beregnDifferanse(tilkjentYtelse, utenlandskePeriodebeløp, valutakurser)
        tilkjentYtelseRepository.lagreVedEndring(tilkjentYtelse, nyTilkjentYtelse)
    }
}

@Service
class TilpassDifferanseberegningEtterUtenlandskPeriodebeløpService(
    private val valutakursRepository: PeriodeOgBarnSkjemaRepository<Valutakurs>,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository
) : PeriodeOgBarnSkjemaEndringAbonnent<UtenlandskPeriodebeløp> {
    override fun skjemaerEndret(
        behandlingId: BehandlingId,
        utenlandskePeriodebeløp: Collection<UtenlandskPeriodebeløp>
    ) {
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandlingOptional(behandlingId.id) ?: return
        val valutakurser = valutakursRepository.finnFraBehandlingId(behandlingId.id)

        val nyTilkjentYtelse = beregnDifferanse(tilkjentYtelse, utenlandskePeriodebeløp, valutakurser)
        tilkjentYtelseRepository.lagreVedEndring(tilkjentYtelse, nyTilkjentYtelse)
    }
}

@Service
class TilpassDifferanseberegningEtterValutakursService(
    private val utenlandskPeriodebeløpRepository: PeriodeOgBarnSkjemaRepository<UtenlandskPeriodebeløp>,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository
) : PeriodeOgBarnSkjemaEndringAbonnent<Valutakurs> {
    override fun skjemaerEndret(behandlingId: BehandlingId, valutakurser: Collection<Valutakurs>) {
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandlingOptional(behandlingId.id) ?: return
        val utenlandskePeriodebeløp = utenlandskPeriodebeløpRepository.finnFraBehandlingId(behandlingId.id)

        val nyTilkjentYtelse = beregnDifferanse(tilkjentYtelse, utenlandskePeriodebeløp, valutakurser)
        tilkjentYtelseRepository.lagreVedEndring(tilkjentYtelse, nyTilkjentYtelse)
    }
}

fun TilkjentYtelseRepository.lagreVedEndring(forrigeTilkjentYtelse: TilkjentYtelse, nyTilkjentYtelse: TilkjentYtelse) {
    if (forrigeTilkjentYtelse.andelerTilkjentYtelse.toSet() != nyTilkjentYtelse.andelerTilkjentYtelse.toSet()) {
        this.delete(forrigeTilkjentYtelse)
        this.save(nyTilkjentYtelse)
    }
}
