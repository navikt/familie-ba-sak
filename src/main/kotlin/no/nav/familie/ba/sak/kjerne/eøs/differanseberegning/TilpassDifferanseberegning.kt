package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseEndretAbonnent
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEndringAbonnent
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import org.springframework.stereotype.Service

@Service
class TilpassDifferanseberegningEtterTilkjentYtelseService(
    private val differanseberegningService: DifferanseberegningService
) : TilkjentYtelseEndretAbonnent {
    override fun endretTilkjentYtelse(tilkjentYtelse: TilkjentYtelse) {
        differanseberegningService.beregnDifferanseFraTilkjentYtelse(
            BehandlingId(tilkjentYtelse.behandling.id),
            tilkjentYtelse
        )
    }
}

@Service
class TilpassDifferanseberegningEtterUtenlandskPeriodebeløpService(
    private val differanseberegningService: DifferanseberegningService
) : PeriodeOgBarnSkjemaEndringAbonnent<UtenlandskPeriodebeløp> {
    override fun skjemaerEndret(behandlingId: BehandlingId, endretTil: Collection<UtenlandskPeriodebeløp>) {
        differanseberegningService.beregnDifferanseFraUtenlandskePeridebeløp(behandlingId, endretTil)
    }
}

@Service
class TilpassDifferanseberegningEtterValutakursService(
    private val differanseberegningService: DifferanseberegningService
) : PeriodeOgBarnSkjemaEndringAbonnent<Valutakurs> {
    override fun skjemaerEndret(behandlingId: BehandlingId, endretTil: Collection<Valutakurs>) {
        differanseberegningService.beregnDifferanseFraValutakurser(behandlingId, endretTil)
    }
}
