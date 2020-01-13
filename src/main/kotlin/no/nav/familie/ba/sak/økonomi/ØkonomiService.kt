package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.Beregning
import no.nav.familie.ba.sak.behandling.FagsakService
import no.nav.familie.ba.sak.behandling.domene.vedtak.BehandlingVedtak
import no.nav.familie.ba.sak.behandling.domene.vedtak.BehandlingVedtakStatus
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class ØkonomiService(
        private val fagsakService: FagsakService,
        private val økonomiKlient: ØkonomiKlient,
        private val beregning: Beregning,
        private val behandlingService: BehandlingService
) {
    fun iverksettVedtak(behandlingVedtak: BehandlingVedtak, saksbehandlerId: String): Ressurs<RestFagsak> {
        if (behandlingVedtak.status == BehandlingVedtakStatus.SENDT_TIL_IVERKSETTING) {
            return Ressurs.failure("Vedtaket er allerede sendt til oppdrag og venter på kvittering")
        } else if (behandlingVedtak.status == BehandlingVedtakStatus.IVERKSATT) {
            return Ressurs.failure("Vedtaket er allerede iverksatt")
        }

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
                fagSystem = "BA",
                saksnummer = behandlingVedtak.behandling.fagsak.id.toString(),
                aktoer = behandlingVedtak.behandling.fagsak.personIdent?.ident.toString(),
                utbetalingsperiode = utbetalingsperioder
        )

        Result.runCatching { økonomiKlient.iverksettOppdrag(utbetalingsoppdrag) }
                .fold(
                        onSuccess = {
                            behandlingService.oppdatertStatusPåBehandlingVedtak(behandlingVedtak, BehandlingVedtakStatus.SENDT_TIL_IVERKSETTING)
                            return fagsakService.hentRestFagsak(behandlingVedtak.behandling.fagsak.id)
                        },
                        onFailure = {
                            return Ressurs.failure("Iverksetting mot oppdrag feilet", it)
                        }
                )
    }
}