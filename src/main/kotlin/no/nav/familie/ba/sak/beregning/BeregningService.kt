package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.vedtak.VedtakPerson
import no.nav.familie.ba.sak.behandling.vedtak.VedtakPersonRepository
import no.nav.familie.ba.sak.beregning.domene.BeregningResultat
import no.nav.familie.ba.sak.beregning.domene.BeregningResultatRepository
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BeregningService(
        private val vedtakPersonRepository: VedtakPersonRepository,
        private val beregningResultatRepository: BeregningResultatRepository
) {

    fun hentPersonerForVedtak(vedtakId: Long): List<VedtakPerson> {
        return vedtakPersonRepository.finnPersonBeregningForVedtak(vedtakId)
    }

    fun lagreBeregningsresultat(behandling: Behandling, utbetalingsoppdrag: Utbetalingsoppdrag) {

        var erOpphør = false
        var opphørsdato: LocalDate = LocalDate.now()
        if (utbetalingsoppdrag.utbetalingsperiode.size == 1 && utbetalingsoppdrag.utbetalingsperiode[0].opphør != null) {
            erOpphør = true
            opphørsdato = utbetalingsoppdrag.utbetalingsperiode[0].opphør!!.opphørDatoFom
        }

        val nyttBeregningsResultat = BeregningResultat(
                behandling = behandling,
                utbetalingsoppdrag = utbetalingsoppdrag,
                opprettetDato = LocalDate.now(),
                stønadFom = if (erOpphør) opphørsdato else utbetalingsoppdrag.utbetalingsperiode
                        .filter { !it.erEndringPåEksisterendePeriode }
                        .minBy { it.vedtakdatoFom }!!.vedtakdatoFom,
                stønadTom = utbetalingsoppdrag.utbetalingsperiode.maxBy { it.vedtakdatoTom }!!.vedtakdatoTom,
                erOpphør = erOpphør
        )

        beregningResultatRepository.save(nyttBeregningsResultat)
    }
}