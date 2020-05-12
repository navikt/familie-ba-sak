package no.nav.familie.ba.sak.dokument

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.validering.VedtaktilgangConstraint
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/dokument")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class DokumentController(
        private val dokumentService: DokumentService,
        private val vedtakService: VedtakService
) {

    @GetMapping(path = ["vedtak-html/{vedtakId}"])
    fun hentHtmlVedtak(@PathVariable @VedtaktilgangConstraint vedtakId: Long): Ressurs<RestDokument> {
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} henter vedtaksbrev")

        val vedtak = vedtakService.hent(vedtakId)

        return dokumentService.hentBrevForVedtak(vedtak)
    }

    companion object {
        val LOG = LoggerFactory.getLogger(DokumentController::class.java)
    }
}

class RestDokument(@JsonProperty("pdfBase64")
                   val pdf: ByteArray,
                   val html: String?)