package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.vedtak.VedtakPersonYtelsesperiode
import no.nav.familie.ba.sak.behandling.vedtak.VedtakPersonRepository
import no.nav.familie.ba.sak.beregning.domene.BeregningResultat
import no.nav.familie.ba.sak.beregning.domene.BeregningResultatRepository
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BeregningService(
        private val vedtakPersonRepository: VedtakPersonRepository,
        private val beregningResultatRepository: BeregningResultatRepository
) {

    fun hentPersonerForVedtak(vedtakId: Long): List<VedtakPersonYtelsesperiode> {
        return vedtakPersonRepository.finnPersonBeregningForVedtak(vedtakId)
    }

    fun hentBeregningsresultatForBehandling(behandlingId: Long): BeregningResultat {
        return beregningResultatRepository.findByBehandling(behandlingId)
    }

    fun lagreBeregningsresultat(behandling: Behandling, utbetalingsoppdrag: Utbetalingsoppdrag) {

        val erRentOpphør = utbetalingsoppdrag.utbetalingsperiode.size == 1 && utbetalingsoppdrag.utbetalingsperiode[0].opphør != null
        var opphørsdato: LocalDate? = null
        if (utbetalingsoppdrag.utbetalingsperiode[0].opphør != null) {
            opphørsdato = utbetalingsoppdrag.utbetalingsperiode[0].opphør!!.opphørDatoFom
        }

        val nyttBeregningsResultat = BeregningResultat(
                behandling = behandling,
                utbetalingsoppdrag = objectMapper.writeValueAsString(utbetalingsoppdrag),
                opprettetDato = LocalDate.now(),
                stønadFom = if (erRentOpphør) null else utbetalingsoppdrag.utbetalingsperiode
                        .filter { !it.erEndringPåEksisterendePeriode }
                        .minBy { it.vedtakdatoFom }!!.vedtakdatoFom,
                stønadTom = utbetalingsoppdrag.utbetalingsperiode.maxBy { it.vedtakdatoTom }!!.vedtakdatoTom,
                opphørFom = opphørsdato
        )

        beregningResultatRepository.save(nyttBeregningsResultat)
    }
}