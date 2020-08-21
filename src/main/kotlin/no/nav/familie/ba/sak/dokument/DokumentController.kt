package no.nav.familie.ba.sak.dokument

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.validering.VedtaktilgangConstraint
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/dokument")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class DokumentController(
        private val dokumentService: DokumentService,
        private val vedtakService: VedtakService,
        private val behandlingService: BehandlingService
) {
    @PostMapping(path = ["vedtaksbrev/{vedtakId}"])
    fun genererVedtaksbrev(@PathVariable @VedtaktilgangConstraint vedtakId: Long): Ressurs<ByteArray> {
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} henter vedtaksbrev")

        val vedtak = vedtakService.hent(vedtakId)

        return dokumentService.genererBrevForVedtak(vedtak).let {
            vedtak.stønadBrevPdF = it
            vedtakService.lagreEllerOppdater(vedtak)
            Ressurs.success(it)
        }
    }

    @GetMapping(path = ["vedtaksbrev/{vedtakId}"])
    fun hentVedtaksbrev(@PathVariable @VedtaktilgangConstraint vedtakId: Long): Ressurs<ByteArray> {
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} henter vedtaksbrev")

        val vedtak = vedtakService.hent(vedtakId)

        return dokumentService.hentBrevForVedtak(vedtak)
    }

    @PostMapping(path = ["manuellebrev/{brevMal}/{behandlingId}"])
    fun genererBrev(
            @PathVariable brevMal: String,
            @PathVariable behandlingId: Long,
            @RequestBody manuelleBrevRequest: ManuelleBrevRequest)
            : Ressurs<ByteArray> {
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} henter brev: $brevMal")

        if (manuelleBrevRequest.fritekst.isEmpty()) {
            return Ressurs.failure("Friteksten kan ikke være tom", "Friteksten kan ikke være tom")
        }

        val behandling = behandlingService.hent(behandlingId)

        return if (brevMal === ManuelleBrev.INNHENTE_OPPLYSNINGER.malId) {
            dokumentService.genererBrevForInnhenteOpplysninger(behandling, manuelleBrevRequest).let {
                Ressurs.success(it)
            }
        } else {
            error("Finnes ingen støttet brevmal for type $brevMal")
        }
    }

    enum class ManuelleBrev(val malId: String) {
        INNHENTE_OPPLYSNINGER("innhente-opplysninger")
    }

    class ManuelleBrevRequest(val fritekst: String)

    companion object {
        val LOG = LoggerFactory.getLogger(DokumentController::class.java)
    }
}
