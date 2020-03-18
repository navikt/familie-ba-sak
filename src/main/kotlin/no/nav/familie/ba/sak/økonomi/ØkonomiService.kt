package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BrevType
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.task.dto.StatusFraOppdragDTO
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.springframework.stereotype.Service

@Service
class ØkonomiService(
        private val økonomiKlient: ØkonomiKlient,
        private val behandlingService: BehandlingService,
        private val vedtakService: VedtakService,
        private val beregningService: BeregningService
) {

    fun iverksettVedtak(behandlingsId: Long, vedtakId: Long, saksbehandlerId: String) {
        val vedtak = vedtakService.hent(vedtakId)

        val personberegninger = if (vedtak.behandling.brev == BrevType.OPPHØRT)
            beregningService.hentPersonerForVedtak(vedtak.forrigeVedtakId!!)
        else beregningService.hentPersonerForVedtak(vedtak.id)

        val utbetalingsoppdrag = lagUtbetalingsoppdrag(saksbehandlerId, vedtak, personberegninger)

        iverksettOppdrag(vedtak.behandling.id, utbetalingsoppdrag)
    }


    private fun iverksettOppdrag(behandlingsId: Long,
                                 utbetalingsoppdrag: Utbetalingsoppdrag) {
        Result.runCatching { økonomiKlient.iverksettOppdrag(utbetalingsoppdrag) }
                .fold(
                        onSuccess = {
                            checkNotNull(it.body) { "Finner ikke ressurs" }
                            checkNotNull(it.body?.data) { "Ressurs mangler data" }

                            check(it.body?.status == Ressurs.Status.SUKSESS) {
                                "Ressurs returnerer ${it.body?.status} men har http status kode ${it.statusCode}"
                            }

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
                            checkNotNull(it.body) { "Finner ikke ressurs" }
                            checkNotNull(it.body?.data) { "Ressurs mangler data" }
                            check(it.body?.status == Ressurs.Status.SUKSESS) {
                                "Ressurs returnerer ${it.body?.status} men har http status kode ${it.statusCode}"
                            }

                            return it.body?.data!!
                        },
                        onFailure = {
                            throw Exception("Henting av status mot oppdrag feilet", it)
                        }
                )
    }

}
