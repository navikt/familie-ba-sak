package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.Beregning
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import org.springframework.stereotype.Service
import java.lang.Error
import java.math.BigDecimal

@Service
class ØkonomiService(
        private val økonomiKlient: ØkonomiKlient,
        private val beregning: Beregning,
        private val behandlingService: BehandlingService
) {
    fun iverksettVedtak(behandlingsId: Long, vedtakId: Long, saksbehandlerId: String) {
        val vedtak = behandlingService.hentVedtak(vedtakId)
                ?: throw Error("Fant ikke vedtak med id $vedtakId i forbindelse med iverksetting mot oppdrag")

        val barnBeregning = behandlingService.hentBarnBeregningForVedtak(vedtak.id)
        val tidslinje = beregning.beregnUtbetalingsperioder(barnBeregning)
        val utbetalingsperioder = tidslinje.toSegments().map {
            Utbetalingsperiode(
                    erEndringPåEksisterendePeriode = false,
                    datoForVedtak = vedtak.vedtaksdato,
                    klassifisering = "BATR",
                    vedtakdatoFom = it.fom,
                    vedtakdatoTom = it.tom,
                    sats = BigDecimal(it.value),
                    satsType = Utbetalingsperiode.SatsType.MND,
                    utbetalesTil = vedtak.behandling.fagsak.personIdent.ident.toString(),
                    behandlingId = vedtak.behandling.id!!
            )
        }

        val utbetalingsoppdrag = Utbetalingsoppdrag(
                saksbehandlerId = saksbehandlerId,
                kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
                fagSystem = FAGSYSTEM,
                saksnummer = vedtak.behandling.fagsak.id.toString(),
                aktoer = vedtak.behandling.fagsak.personIdent.ident.toString(),
                utbetalingsperiode = utbetalingsperioder
        )

        Result.runCatching { økonomiKlient.iverksettOppdrag(utbetalingsoppdrag) }
                .fold(
                        onSuccess = {
                            behandlingService.oppdaterStatusPåBehandling(behandlingsId, BehandlingStatus.SENDT_TIL_IVERKSETTING)
                        },
                        onFailure = {
                            throw Exception("Iverksetting mot oppdrag feilet", it)
                        }
                )
    }

    fun hentStatus(statusFraOppdragDTO: StatusFraOppdragDTO): OppdragProtokollStatus {
        Result.runCatching { økonomiKlient.hentStatus(statusFraOppdragDTO) }
                .fold(
                        onSuccess = {
                            return objectMapper.convertValue(it.body?.data, OppdragProtokollStatus::class.java)
                        },
                        onFailure = {
                            throw Exception("Henting av status mot oppdrag feilet", it)
                        }
                )
    }
}