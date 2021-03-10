package no.nav.familie.ba.sak.simulering

import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.common.assertGenerelleSuksessKriterier
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.simulering.domene.VedtakSimuleringMottaker
import no.nav.familie.ba.sak.simulering.domene.VedtakSimuleringMottakerRepository
import no.nav.familie.ba.sak.simulering.domene.VedtakSimuleringPostering
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
) {

    fun hentSimulering(vedtak: Vedtak): DetaljertSimuleringResultat {
        Result.runCatching {
            simuleringKlient.hentSimulering(økonomiService.genererUtbetalingsoppdrag(vedtak = vedtak,
                                                                                     saksbehandlerId = SikkerhetContext.hentSaksbehandler()
                                                                                             .take(8)))
        }.fold(
                onSuccess = {
                    assertGenerelleSuksessKriterier(it.body)
                    return it.body?.data!!
                },
                onFailure = {
                    throw Exception("Henting av etterbetalingsbeløp fra simulering feilet", it)
                }
        )
    }

    @Transactional
    fun lagreSimulering(simuleringMottakere: List<SimuleringMottaker>,
                        vedtak: Vedtak): Pair<List<VedtakSimuleringMottaker>, List<VedtakSimuleringPostering>> {
        var (vedtakSimuleringMottakere, vedtakSimuleringPosteringer) = opprettSimuleringsobjekter(simuleringMottakere, vedtak)

        vedtakSimuleringMottakere = vedtakSimuleringMottakerRepository.saveAll(vedtakSimuleringMottakere)
        vedtakSimuleringPosteringer = vedtakSimuleringPosteringRepository.saveAll(vedtakSimuleringPosteringer)

        return Pair(vedtakSimuleringMottakere, vedtakSimuleringPosteringer)
    }

    fun hentSimuleringLagretPåVedtak(vedtakId: Long) {
        vedtakSimuleringMottakerRepository.findByVedtakId(vedtakId)
    }


}