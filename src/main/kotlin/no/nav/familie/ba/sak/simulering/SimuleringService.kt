package no.nav.familie.ba.sak.simulering

import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.steg.BehandlerRolle
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.common.assertGenerelleSuksessKriterier
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.simulering.domene.VedtakSimuleringMottaker
import no.nav.familie.ba.sak.simulering.domene.VedtakSimuleringMottakerRepository
import no.nav.familie.ba.sak.simulering.domene.VedtakSimuleringPosteringRepository
import no.nav.familie.ba.sak.økonomi.ØkonomiService
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.kontrakter.felles.simulering.SimuleringMottaker
import org.springframework.stereotype.Service
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
            val simuleringResponse = simuleringKlient.hentSimulering(økonomiService.genererUtbetalingsoppdrag(vedtak = vedtak,
                                                                                                              saksbehandlerId = SikkerhetContext.hentSaksbehandler()
                                                                                                                      .take(8)))
            assertGenerelleSuksessKriterier(simuleringResponse.body)
            return simuleringResponse.body?.data!!
        } catch (feil: Throwable) {
            throw Exception("Henting av etterbetalingsbeløp fra simulering feilet", feil)
        }
    }

    @Transactional
    fun lagreSimuleringPåVedtak(simuleringMottakere: List<SimuleringMottaker>,
                                vedtak: Vedtak): List<VedtakSimuleringMottaker> {
        var vedtakSimuleringMottakere = simuleringMottakere.map { it.tilVedtakSimuleringMottaker(vedtak) }
        vedtakSimuleringMottakere = vedtakSimuleringMottakerRepository.saveAll(vedtakSimuleringMottakere)
        return vedtakSimuleringMottakere
    }

    @Transactional
    fun slettSimuleringPåVedtak(vedtakId: Long) {
        val simuleringMottakere = hentSimuleringPåVedtak(vedtakId)
        simuleringMottakere.forEach {
            vedtakSimuleringPosteringRepository.deleteByVedtakSimuleringMottakerId(it.id)
        }
        return vedtakSimuleringMottakerRepository.deleteByVedtakId(vedtakId)
    }

    fun hentSimuleringPåVedtak(vedtakId: Long): List<VedtakSimuleringMottaker> {
        return vedtakSimuleringMottakerRepository.findByVedtakId(vedtakId)
    }

    fun hentSimulering(vedtakId: Long): List<VedtakSimuleringMottaker> {
        val vedtak = vedtakService.hent(vedtakId)
        val erÅpenBehandling =
                vedtak.behandling.status == BehandlingStatus.OPPRETTET ||
                vedtak.behandling.status == BehandlingStatus.UTREDES

        return if (erÅpenBehandling) {
            oppdaterSimuleringPåVedtak(vedtak)

        } else hentSimuleringPåVedtak(vedtakId)
    }

    @Transactional
    fun oppdaterSimuleringPåVedtak(vedtak: Vedtak): List<VedtakSimuleringMottaker> {
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                                                      handling = "opprette simulering")
        val simulering: List<SimuleringMottaker> = hentSimuleringFraFamilieOppdrag(vedtak = vedtak).simuleringMottaker
        slettSimuleringPåVedtak(vedtak.id)
        return lagreSimuleringPåVedtak(simulering, vedtak)
    }
}