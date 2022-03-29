package no.nav.familie.ba.sak.kjerne.vedtak

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.brev.DokumentService
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class VedtakService(
    private val behandlingService: BehandlingService,
    private val vedtakRepository: VedtakRepository,
    private val dokumentService: DokumentService,
    private val tilbakekrevingService: TilbakekrevingService,
    private val vedtaksperiodeService: VedtaksperiodeService
) {

    fun hent(vedtakId: Long): Vedtak {
        return vedtakRepository.getById(vedtakId)
    }

    fun hentAktivForBehandling(behandlingId: Long): Vedtak? {
        return vedtakRepository.findByBehandlingAndAktivOptional(behandlingId)
    }

    fun hentAktivForBehandlingThrows(behandlingId: Long): Vedtak {
        return vedtakRepository.findByBehandlingAndAktiv(behandlingId)
    }

    fun oppdater(vedtak: Vedtak): Vedtak {

        return if (vedtakRepository.findByIdOrNull(vedtak.id) != null) {
            vedtakRepository.saveAndFlush(vedtak)
        } else {
            error("Forsøker å oppdatere et vedtak som ikke er lagret")
        }
    }

    fun oppdaterVedtakMedStønadsbrev(vedtak: Vedtak): Vedtak {

        return if (vedtak.behandling.erBehandlingMedVedtaksbrevutsending()) {
            val brev = dokumentService.genererBrevForVedtak(vedtak)
            vedtakRepository.save(vedtak.also { it.stønadBrevPdF = brev })
        } else {
            vedtak
        }
    }

    /**
     * Oppdater vedtaksdato og brev.
     * Vi oppdaterer brevet for å garantere å få riktig beslutter og vedtaksdato.
     */
    fun oppdaterVedtaksdatoOgBrev(vedtak: Vedtak) {
        vedtak.vedtaksdato = LocalDateTime.now()
        oppdaterVedtakMedStønadsbrev(vedtak)

        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} beslutter vedtak $vedtak")
    }

    companion object {

        private val logger = LoggerFactory.getLogger(VedtakService::class.java)
    }
}
