package no.nav.familie.ba.sak.kjerne.vedtak

import no.nav.familie.ba.sak.kjerne.brev.DokumentGenereringService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class VedtakService(
    private val vedtakRepository: VedtakRepository,
    private val dokumentGenereringService: DokumentGenereringService,
) {
    fun hent(vedtakId: Long): Vedtak = vedtakRepository.getReferenceById(vedtakId)

    fun hentAktivForBehandling(behandlingId: Long): Vedtak? = vedtakRepository.findByBehandlingAndAktivOptional(behandlingId)

    fun hentAktivForBehandlingThrows(behandlingId: Long): Vedtak = vedtakRepository.findByBehandlingAndAktiv(behandlingId)

    fun hentVedtaksdatoForBehandlingThrows(behandlingId: Long): LocalDateTime =
        vedtakRepository.finnVedtaksdatoForBehandling(behandlingId)
            ?: error("Finner ikke vedtaksato for behandling=$behandlingId")

    fun oppdater(vedtak: Vedtak): Vedtak =
        if (vedtakRepository.findByIdOrNull(vedtak.id) != null) {
            vedtakRepository.saveAndFlush(vedtak)
        } else {
            error("Forsøker å oppdatere et vedtak som ikke er lagret")
        }

    fun oppdaterVedtakMedStønadsbrev(vedtak: Vedtak): Vedtak =
        if (vedtak.behandling.erBehandlingMedVedtaksbrevutsending()) {
            val brev = dokumentGenereringService.genererBrevForVedtak(vedtak)
            vedtakRepository.save(vedtak.also { it.stønadBrevPdF = brev })
        } else {
            vedtak
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
