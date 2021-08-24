package no.nav.familie.ba.sak.kjerne.simulering

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringMottaker
import no.nav.familie.ba.sak.kjerne.simulering.domene.RestSimulering
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØknomiSimuleringMottakerRepository
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiKlient
import no.nav.familie.ba.sak.økonomi.ØkonomiService
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.kontrakter.felles.simulering.SimuleringMottaker
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import javax.transaction.Transactional

@Service
class SimuleringService(
        private val økonomiKlient: ØkonomiKlient,
        private val økonomiService: ØkonomiService,
        private val øknomiSimuleringMottakerRepository: ØknomiSimuleringMottakerRepository,
        private val tilgangService: TilgangService,
        private val vedtakRepository: VedtakRepository,
        private val behandlingRepository: BehandlingRepository,
) {

    fun hentSimuleringFraFamilieOppdrag(vedtak: Vedtak): DetaljertSimuleringResultat? {
        try {
            /**
             * SOAP integrasjonen støtter ikke full epost som MQ,
             * så vi bruker bare første 8 tegn av saksbehandlers epost for simulering.
             * Denne verdien brukes ikke til noe i simulering.
             */
            val utbetalingsoppdrag = økonomiService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                    vedtak = vedtak,
                    saksbehandlerId = SikkerhetContext.hentSaksbehandler().take(8),
                    erSimulering = true,
            )

            if (utbetalingsoppdrag.utbetalingsperiode.isEmpty()) {
                return null
            }

            return økonomiKlient.hentSimulering(utbetalingsoppdrag)?.data
        } catch (feil: Throwable) {
            throw Feil("Henting av simuleringsresultat feilet: ${feil.message}")
        }
    }

    @Transactional
    fun lagreSimuleringPåBehandling(simuleringMottakere: List<SimuleringMottaker>,
                                    beahndling: Behandling): List<ØkonomiSimuleringMottaker> {
        val vedtakSimuleringMottakere = simuleringMottakere.map { it.tilBehandlingSimuleringMottaker(beahndling) }
        return øknomiSimuleringMottakerRepository.saveAll(vedtakSimuleringMottakere)
    }

    @Transactional
    fun slettSimuleringPåBehandling(behandlingId: Long) = øknomiSimuleringMottakerRepository.deleteByBehandlingId(behandlingId)

    fun hentSimuleringPåBehandling(behandlingId: Long): List<ØkonomiSimuleringMottaker> {
        return øknomiSimuleringMottakerRepository.findByBehandlingId(behandlingId)
    }

    fun oppdaterSimuleringPåBehandlingVedBehov(behandlingId: Long): List<ØkonomiSimuleringMottaker> {
        val behandling = behandlingRepository.finnBehandling(behandlingId)
        val behandlingErFerdigBesluttet =
                behandling.status == BehandlingStatus.IVERKSETTER_VEDTAK ||
                behandling.status == BehandlingStatus.AVSLUTTET

        val simulering = hentSimuleringPåBehandling(behandlingId)
        val restSimulering = vedtakSimuleringMottakereTilRestSimulering(simulering)

        return if (!behandlingErFerdigBesluttet && simuleringErUtdatert(restSimulering)) {
            oppdaterSimuleringPåBehandling(behandling)
        } else simulering
    }

    private fun simuleringErUtdatert(simulering: RestSimulering) =
            simulering.tidSimuleringHentet == null
            || (simulering.forfallsdatoNestePeriode != null
                && simulering.tidSimuleringHentet < simulering.forfallsdatoNestePeriode
                && LocalDate.now() > simulering.forfallsdatoNestePeriode)

    @Transactional
    fun oppdaterSimuleringPåBehandling(behandling: Behandling): List<ØkonomiSimuleringMottaker> {
        val aktivtVedtak = vedtakRepository.findByBehandlingAndAktiv(behandling.id)
                           ?: throw Feil("Fant ikke aktivt vedtak på behandling${behandling.id}")
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                                                      handling = "opprette simulering")

        val simulering: List<SimuleringMottaker> =
                hentSimuleringFraFamilieOppdrag(vedtak = aktivtVedtak)?.simuleringMottaker ?: emptyList()

        slettSimuleringPåBehandling(behandling.id)
        return lagreSimuleringPåBehandling(simulering, behandling)
    }

    fun hentEtterbetaling(behandlingId: Long): BigDecimal {
        val vedtakSimuleringMottakere = hentSimuleringPåBehandling(behandlingId)
        return vedtakSimuleringMottakereTilRestSimulering(vedtakSimuleringMottakere).etterbetaling
    }

    fun hentFeilutbetaling(behandlingId: Long): BigDecimal {
        val vedtakSimuleringMottakere = hentSimuleringPåBehandling(behandlingId)
        return vedtakSimuleringMottakereTilRestSimulering(vedtakSimuleringMottakere).feilutbetaling
    }
}