package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.vedtak.AndelTilkjentYtelse
import no.nav.familie.ba.sak.behandling.vedtak.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.beregning.domene.BeregningResultat
import no.nav.familie.ba.sak.beregning.domene.BeregningResultatRepository
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BeregningService(
        private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
        private val beregningResultatRepository: BeregningResultatRepository
) {
    fun hentAndelerTilkjentYtelseForBehandling(behandlingId: Long): List<AndelTilkjentYtelse> {
        return andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId)
    }

    fun hentBeregningsresultatForBehandling(behandlingId: Long): BeregningResultat {
        return beregningResultatRepository.findByBehandling(behandlingId)
    }

    fun lagreBeregningsresultat(behandling: Behandling, utbetalingsoppdrag: Utbetalingsoppdrag) {

        val nyttBeregningsResultat = populerBeregningsresultat(behandling, utbetalingsoppdrag)
        beregningResultatRepository.save(nyttBeregningsResultat)
    }

    private fun populerBeregningsresultat(behandling: Behandling, utbetalingsoppdrag: Utbetalingsoppdrag): BeregningResultat {
        val erRentOpphør = utbetalingsoppdrag.utbetalingsperiode.size == 1 && utbetalingsoppdrag.utbetalingsperiode[0].opphør != null
        var opphørsdato: LocalDate? = null
        if (utbetalingsoppdrag.utbetalingsperiode[0].opphør != null) {
            opphørsdato = utbetalingsoppdrag.utbetalingsperiode[0].opphør!!.opphørDatoFom
        }

        return BeregningResultat(
                behandling = behandling,
                utbetalingsoppdrag = objectMapper.writeValueAsString(utbetalingsoppdrag),
                opprettetDato = LocalDate.now(),
                stønadFom = if (erRentOpphør) null else utbetalingsoppdrag.utbetalingsperiode
                        .filter { !it.erEndringPåEksisterendePeriode }
                        .minBy { it.vedtakdatoFom }!!.vedtakdatoFom,
                stønadTom = utbetalingsoppdrag.utbetalingsperiode.maxBy { it.vedtakdatoTom }!!.vedtakdatoTom,
                opphørFom = opphørsdato
        )
    }
}