package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.kontrakt.Ressurs
import no.nav.familie.sikkerhet.OIDCUtil
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
@ProtectedWithClaims( issuer = "azuread" )
class FagsakController (
        private val oidcUtil: OIDCUtil,
        private val fagsakService: FagsakService
) {
    @GetMapping(path = ["/fagsak/{fagsakId}"])
    fun fagsak(@PathVariable fagsakId: Long): ResponseEntity<Ressurs<RestFagsak>> {
        val saksbehandlerId = oidcUtil.getClaim("preferred_username")

        logger.info("{} henter fagsak med id {}", saksbehandlerId ?: "Ukjent", fagsakId)

        val ressurs: Ressurs<RestFagsak> = Result.runCatching { fagsakService.hentRestFagsak(fagsakId) }
                .fold(
                    onSuccess = { it },
                    onFailure = { e -> Ressurs.failure( "Henting av fagsak med fagsakId $fagsakId feilet: ${e.message}", e) }
                )

        return ResponseEntity.ok(ressurs)
    }

    @GetMapping(path = ["/fagsak/{fagsakId}/vedtak-html"])
    fun hentVedtaksBrevHtml(@PathVariable fagsakId: Long): Ressurs<String> {
        val saksbehandlerId = oidcUtil.getClaim("preferred_username")
        FagsakController.logger.info("{} henter vedtaksbrev", saksbehandlerId ?: "VL")

        //TODO: integration with DokGen

        return Ressurs.success("<H1>Vedtaksbrev</H1><br/>FagsakID= $fagsakId<br /><P>Backend API not implemented yet</P>");
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(BehandlingslagerService::class.java)
    }
}