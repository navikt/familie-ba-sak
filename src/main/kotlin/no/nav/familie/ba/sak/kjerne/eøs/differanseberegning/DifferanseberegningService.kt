package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpService
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DifferanseberegningService(
    private val valutakursService: ValutakursService,
    private val utenlandskPeriodebeløpService: UtenlandskPeriodebeløpService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository
) {
    @Transactional
    fun beregnDifferanseFraTilkjentYtelse(behandlingId: BehandlingId, tilkjentYtelse: TilkjentYtelse) {
        val valutakurser = valutakursService.hentValutakurser(behandlingId)
        val utenlandskePeriodebeløp = utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(behandlingId)

        val nyTilkjentYtelse = beregnDifferanse(tilkjentYtelse, utenlandskePeriodebeløp, valutakurser)
        tilkjentYtelseRepository.save(nyTilkjentYtelse)
    }

    @Transactional
    fun beregnDifferanseFraUtenlandskePeridebeløp(
        behandlingId: BehandlingId,
        utenlandskePeriodebeløp: Collection<UtenlandskPeriodebeløp>
    ) {
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandlingId.id)
        val valutakurser = valutakursService.hentValutakurser(behandlingId)

        val nyTilkjentYtelse = beregnDifferanse(tilkjentYtelse, utenlandskePeriodebeløp, valutakurser)
        tilkjentYtelseRepository.save(nyTilkjentYtelse)
    }

    @Transactional
    fun beregnDifferanseFraValutakurser(behandlingId: BehandlingId, valutakurser: Collection<Valutakurs>) {
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandlingId.id)
        val utenlandskePeriodebeløp = utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(behandlingId)

        val nyTilkjentYtelse = beregnDifferanse(tilkjentYtelse, utenlandskePeriodebeløp, valutakurser)
        tilkjentYtelseRepository.save(nyTilkjentYtelse)
    }

    private fun beregnDifferanse(
        tilkjentYtelse: TilkjentYtelse,
        utenlandskePeriodebeløp: Collection<UtenlandskPeriodebeløp>,
        valutakurser: Collection<Valutakurs>
    ): TilkjentYtelse {
        TODO()
    }
}
