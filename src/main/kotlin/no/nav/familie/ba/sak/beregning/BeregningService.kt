package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.vedtak.VedtakPersonYtelsesperiode
import no.nav.familie.ba.sak.behandling.vedtak.VedtakPersonRepository
import no.nav.familie.ba.sak.beregning.domene.BeregningResultat
import no.nav.familie.ba.sak.beregning.domene.BeregningResultatRepository
import no.nav.familie.ba.sak.økonomi.OppdragId
import no.nav.familie.ba.sak.økonomi.ØkonomiKlient
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class BeregningService(
        private val vedtakPersonRepository: VedtakPersonRepository,
        private val behandlingRepository: BehandlingRepository,
        private val beregningResultatRepository: BeregningResultatRepository,
        private val økonomiKlient: ØkonomiKlient
) {

    fun hentPersonerForVedtak(vedtakId: Long): List<VedtakPersonYtelsesperiode> {
        return vedtakPersonRepository.finnPersonBeregningForVedtak(vedtakId)
    }

    fun hentBeregningsresultatForBehandling(behandlingId: Long): BeregningResultat {
        return beregningResultatRepository.findByBehandling(behandlingId)
    }

    fun lagreBeregningsresultat(behandling: Behandling, utbetalingsoppdrag: Utbetalingsoppdrag) {

        val nyttBeregningsResultat = populerBeregningsresultat(behandling, utbetalingsoppdrag)
        beregningResultatRepository.save(nyttBeregningsResultat)
    }

    fun migrerBeregningsresultatForEksisterendeBehandlinger() {
        val alleBehandlinger = behandlingRepository.hentAlleBehandlinger()

        val utbetalingsoppdrag: List<Pair<Behandling, Utbetalingsoppdrag>> = alleBehandlinger.map { behandling ->
            Result.runCatching {
                økonomiKlient.hentUtbetalingsoppdrag(OppdragId(behandling.fagsak.personIdent.ident, behandling.id))
            }
                    .fold(
                            onSuccess = {
                                checkNotNull(it.body) { "Finner ikke ressurs" }
                                checkNotNull(it.body?.data) { "Ressurs mangler data" }
                                behandling to it.body?.data!!
                            },
                            onFailure = {
                                throw Exception("Henting av utbetalingsoppdrag fra familie-oppdrag feilet", it)
                            }
                    )
        }

        lagreAlleBeregningsresultater(utbetalingsoppdrag.toMap())
    }

    @Transactional
    fun lagreAlleBeregningsresultater(behandlingerMedUtbetalingsoppdrag: Map<Behandling, Utbetalingsoppdrag>) {
        val beregningsResultater = behandlingerMedUtbetalingsoppdrag.map {
            populerBeregningsresultat(behandling = it.key, utbetalingsoppdrag = it.value)
        }

        beregningResultatRepository.saveAll(beregningsResultater)
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