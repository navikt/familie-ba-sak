package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.Fagsak
import no.nav.familie.ba.sak.behandling.domene.vedtak.BehandlingVedtak
import no.nav.familie.ba.sak.behandling.restDomene.RestBehandling
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.vedtak.DokGenKlient
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
import java.time.LocalDate
import java.time.LocalDateTime

@RestController
@RequestMapping("/api")
@ProtectedWithClaims( issuer = "azuread" )
class FagsakController (
        private val oidcUtil: OIDCUtil,
        private val fagsakService: FagsakService,
        private val docgenKlient: DokGenKlient
) {
    @GetMapping(path = ["/fagsak/{fagsakId}"])
    fun fagsak(@PathVariable fagsakId: Long): ResponseEntity<Ressurs<RestFagsak>> {
        val saksbehandlerId = oidcUtil.getClaim("preferred_username")

        logger.info("{} henter fagsak med id {}", saksbehandlerId ?: "Ukjent", fagsakId)

        /*
        if(fagsakId< 0){
            return ResponseEntity.ok(Ressurs.success(RestFagsak(LocalDateTime.now(), fagsakId, "na", emptyList())));
        }
        */

        val ressurs: Ressurs<RestFagsak> = Result.runCatching { fagsakService.hentRestFagsak(fagsakId) }
                .fold(
                    onSuccess = { it },
                    onFailure = { e -> Ressurs.failure( "Henting av fagsak med fagsakId $fagsakId feilet: ${e.message}", e) }
                )

        return ResponseEntity.ok(ressurs)
    }

    @GetMapping(path = ["/behandling/{behandlingId}/vedtak-html"])
    fun hentVedtakBrevHtml(@PathVariable behandlingId: Long): Ressurs<String> {
        val saksbehandlerId = oidcUtil.getClaim("preferred_username")
        FagsakController.logger.info("{} henter vedtaksbrev", saksbehandlerId ?: "VL")

        val behandlingVedtak= fagsakService.hentVedtakForBehandling(behandlingId);
        if(behandlingVedtak== null){
            return Ressurs.failure("Vedtak ikke funnet");
        }

        /*
        val behandlingVedtak= BehandlingVedtak(-1, -1, "na", LocalDate.MAX, LocalDate.MIN, LocalDate.MAX,
                "# Vedtaksbrev");
        */

        val html= docgenKlient.lagHtmlFraMarkdown(behandlingVedtak.stønadBrevMarkdown);
        FagsakController.logger.debug("HTML preview generated: "+ html);

        return Ressurs.success(html);
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(BehandlingslagerService::class.java)
    }
}