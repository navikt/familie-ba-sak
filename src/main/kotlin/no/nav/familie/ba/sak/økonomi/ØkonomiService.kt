package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.Beregning
import no.nav.familie.ba.sak.behandling.domene.vedtak.BehandlingVedtakStatus
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import org.springframework.stereotype.Service
import org.springframework.util.Assert
import java.lang.Error
import java.math.BigDecimal

@Service
class ØkonomiService(
        private val økonomiKlient: ØkonomiKlient,
        private val beregning: Beregning,
        private val behandlingService: BehandlingService
) {
    fun iverksettVedtak(behandlingVedtakId: Long, saksbehandlerId: String) {
        val behandlingVedtak = behandlingService.hentBehandlingVedtak(behandlingVedtakId)
                ?: throw Error("Fant ikke vedtak med id $behandlingVedtakId i forbindelse med iverksetting mot oppdrag")

        val barnBeregning = behandlingService.hentBarnBeregningForVedtak(behandlingVedtak.id)
        val tidslinje = beregning.beregnUtbetalingsperioder(barnBeregning)
        val utbetalingsperioder = tidslinje.toSegments().map {
            Utbetalingsperiode(
                    erEndringPåEksisterendePeriode = false,
                    datoForVedtak = behandlingVedtak.vedtaksdato,
                    klassifisering = "BATR",
                    vedtakdatoFom = it.fom,
                    vedtakdatoTom = it.tom,
                    sats = BigDecimal(it.value),
                    satsType = Utbetalingsperiode.SatsType.MND,
                    utbetalesTil = behandlingVedtak.behandling.fagsak.personIdent?.ident.toString(),
                    behandlingId = behandlingVedtak.behandling.id!!
            )
        }

        val utbetalingsoppdrag = Utbetalingsoppdrag(
                saksbehandlerId = saksbehandlerId,
                kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
                fagSystem = FAGSYSTEM,
                saksnummer = behandlingVedtak.behandling.fagsak.id.toString(),
                aktoer = behandlingVedtak.behandling.fagsak.personIdent?.ident.toString(),
                utbetalingsperiode = utbetalingsperioder
        )

        Result.runCatching { økonomiKlient.iverksettOppdrag(utbetalingsoppdrag) }
                .fold(
                        onSuccess = {
                            Assert.notNull(it.body, "Finner ikke ressurs")
                            Assert.notNull(it.body?.data, "Ressurs mangler data")
                            Assert.isTrue(it.body?.status == Ressurs.Status.SUKSESS, String.format("Ressurs returnerer %s men har http status kode %s",
                                    it.body?.status,
                                    it.statusCode))

                            behandlingService.oppdatertStatusPåBehandlingVedtak(behandlingVedtak, BehandlingVedtakStatus.SENDT_TIL_IVERKSETTING)
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
                            Assert.isTrue(it.body?.status == Ressurs.Status.SUKSESS, String.format("Ressurs returnerer %s men har http status kode %s",
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