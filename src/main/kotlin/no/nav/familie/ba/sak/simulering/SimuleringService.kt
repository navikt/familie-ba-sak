package no.nav.familie.ba.sak.simulering

import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.steg.BehandlerRolle
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.common.assertGenerelleSuksessKriterier
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.simulering.domene.RestVedtakSimulering
import no.nav.familie.ba.sak.simulering.domene.VedtakSimuleringMottaker
import no.nav.familie.ba.sak.simulering.domene.VedtakSimuleringMottakerRepository
import no.nav.familie.ba.sak.simulering.domene.VedtakSimuleringPosteringRepository
import no.nav.familie.ba.sak.økonomi.ØkonomiService
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.kontrakter.felles.simulering.SimuleringMottaker
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import javax.transaction.Transactional

@Service
class SimuleringService(
        private val simuleringKlient: SimuleringKlient,
        private val økonomiService: ØkonomiService,
        private val vedtakSimuleringPosteringRepository: VedtakSimuleringPosteringRepository,
        private val vedtakSimuleringMottakerRepository: VedtakSimuleringMottakerRepository,
        private val tilgangService: TilgangService,
        private val vedtakService: VedtakService,
) {

    fun hentSimuleringFraFamilieOppdrag(vedtak: Vedtak): DetaljertSimuleringResultat {
        try {
            val simuleringResponse = simuleringKlient.hentSimulering(
                    økonomiService.genererUtbetalingsoppdrag(
                            vedtak = vedtak,
                            saksbehandlerId = SikkerhetContext.hentSaksbehandler()
                                    .take(8)
                    )
            )
            assertGenerelleSuksessKriterier(simuleringResponse.body)
            return simuleringResponse.body?.data!!
        } catch (feil: Throwable) {
            throw Exception("Henting av simuleringsresultat feilet", feil)
        }
    }

    @Transactional
    fun lagreSimuleringPåVedtak(simuleringMottakere: List<SimuleringMottaker>,
                                vedtak: Vedtak): List<VedtakSimuleringMottaker> {
        val vedtakSimuleringMottakere = simuleringMottakere.map { it.tilVedtakSimuleringMottaker(vedtak) }
        return vedtakSimuleringMottakerRepository.saveAll(vedtakSimuleringMottakere)
    }

    @Transactional
    fun slettSimuleringPåVedtak(vedtakId: Long) {
        val simuleringMottakere = hentSimuleringPåVedtak(vedtakId)
        simuleringMottakere.forEach {
            vedtakSimuleringPosteringRepository.deleteByVedtakSimuleringMottakerId(it.id)
        }
        vedtakSimuleringMottakerRepository.deleteByVedtakId(vedtakId)
    }

    fun hentSimuleringPåVedtak(vedtakId: Long): List<VedtakSimuleringMottaker> {
        return vedtakSimuleringMottakerRepository.findByVedtakId(vedtakId)
    }

    fun hentEllerOppdaterSimuleringPåVedtak(vedtakId: Long): List<VedtakSimuleringMottaker> {
        val vedtak = vedtakService.hent(vedtakId)
        val behandlingErFerdigBesluttet =
                vedtak.behandling.status == BehandlingStatus.IVERKSETTER_VEDTAK ||
                vedtak.behandling.status == BehandlingStatus.AVSLUTTET

        val simulering = hentSimuleringPåVedtak(vedtakId)
        val restSimulering = vedtakSimuleringMottakereTilRestSimulering(simulering)

        return if (!behandlingErFerdigBesluttet && simuleringErUtdatert(restSimulering)) {
            oppdaterSimuleringPåVedtak(vedtak)
        } else simulering
    }

    private fun simuleringErUtdatert(simulering: RestVedtakSimulering) =
            simulering.forfallsdatoNestePeriode != null &&
            simulering.tidSimuleringHentet < simulering.forfallsdatoNestePeriode &&
            LocalDate.now() > simulering.forfallsdatoNestePeriode

    @Transactional
    fun oppdaterSimuleringPåVedtak(vedtak: Vedtak): List<VedtakSimuleringMottaker> {
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                                                      handling = "opprette simulering")
        val simulering: List<SimuleringMottaker> = hentSimuleringFraFamilieOppdrag(vedtak = vedtak).simuleringMottaker
        slettSimuleringPåVedtak(vedtak.id)
        return lagreSimuleringPåVedtak(simulering, vedtak)
    }

    fun hentEtterbetaling(vedtakId: Long): BigDecimal {
        val vedtakSimuleringMottakere = hentSimuleringPåVedtak(vedtakId)
        return vedtakSimuleringMottakereTilRestSimulering(vedtakSimuleringMottakere).etterbetaling
    }
}