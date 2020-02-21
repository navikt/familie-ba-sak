package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.vedtak.VedtakResultat
import no.nav.familie.ba.sak.task.dto.StatusFraOppdragDTO
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.springframework.stereotype.Service
import org.springframework.util.Assert

@Service
class ØkonomiService(
        private val økonomiKlient: ØkonomiKlient,
        private val behandlingService: BehandlingService
) {

    fun iverksettVedtak(behandlingsId: Long, vedtakId: Long, saksbehandlerId: String) {
        val vedtak = behandlingService.hentVedtak(vedtakId)
                     ?: throw Error("Fant ikke vedtak med id $vedtakId i forbindelse med iverksetting mot oppdrag")

        val personberegninger = if(vedtak.resultat==VedtakResultat.OPPHØRT)
                behandlingService.hentPersonerForVedtak(vedtak.forrigeVedtakId!!)
                else behandlingService.hentPersonerForVedtak(vedtak.id)

        val utbetalingsoppdrag = lagUtbetalingsoppdrag(saksbehandlerId, vedtak, personberegninger)

        iverksettOppdrag(vedtak.behandling.id, utbetalingsoppdrag)
    }


    private fun iverksettOppdrag(behandlingsId: Long,
                                 utbetalingsoppdrag: Utbetalingsoppdrag) {
        Result.runCatching { økonomiKlient.iverksettOppdrag(utbetalingsoppdrag) }
                .fold(
                        onSuccess = {
                            Assert.notNull(it.body, "Finner ikke ressurs")
                            Assert.notNull(it.body?.data, "Ressurs mangler data")
                            Assert.isTrue(it.body?.status == Ressurs.Status.SUKSESS,
                                          String.format("Ressurs returnerer %s men har http status kode %s",
                                                        it.body?.status,
                                                        it.statusCode))

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
                            Assert.notNull(it.body, "Finner ikke ressurs")
                            Assert.notNull(it.body?.data, "Ressurs mangler data")
                            Assert.isTrue(it.body?.status == Ressurs.Status.SUKSESS,
                                          String.format("Ressurs returnerer %s men har http status kode %s",
                                                        it.body?.status,
                                                        it.statusCode))

                            return objectMapper.convertValue(it.body?.data, OppdragProtokollStatus::class.java)
                        },
                        onFailure = {
                            throw Exception("Henting av status mot oppdrag feilet", it)
                        }
                )
    }

}
