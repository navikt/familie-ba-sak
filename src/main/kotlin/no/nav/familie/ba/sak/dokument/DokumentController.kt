package no.nav.familie.ba.sak.dokument

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
    fun hentHtmlVedtak(@PathVariable @VedtaktilgangConstraint vedtakId: Long): Ressurs<String> {
        val saksbehandlerId = SikkerhetContext.hentSaksbehandler()

        LOG.info("{} henter vedtaksbrev", saksbehandlerId)

        return dokumentService.hentHtmlForVedtak(vedtak = vedtakService.hent(vedtakId))
    }

    companion object {
        val LOG = LoggerFactory.getLogger(DokumentController::class.java)
    }
}