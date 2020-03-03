package no.nav.familie.ba.sak.dokument

import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.validering.BehandlingstilgangConstraint
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// TODO: endre til dokument eller flytt til vedtak
@RestController
@RequestMapping("/api")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class DokumentController(
        private val dokumentService: DokumentService
) {

    @GetMapping(path = ["behandlinger/{behandlingId}/vedtak-html"])
    fun hentHtmlVedtak(@PathVariable @BehandlingstilgangConstraint behandlingId: Long): Ressurs<String> {
        val saksbehandlerId = SikkerhetContext.hentSaksbehandler()

        LOG.info("{} henter vedtaksbrev", saksbehandlerId)

        return dokumentService.hentHtmlVedtakForBehandling(behandlingId)
    }

    companion object {
        val LOG = LoggerFactory.getLogger(DokumentController::class.java)
    }
}