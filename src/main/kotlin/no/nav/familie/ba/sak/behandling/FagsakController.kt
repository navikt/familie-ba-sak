package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.vedtak.BehandlingVedtak
import no.nav.familie.ba.sak.behandling.domene.vedtak.NyttVedtak
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.vedtak.DokGenKlient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.sikkerhet.OIDCUtil
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
@ProtectedWithClaims(issuer = "azuread")
class FagsakController(
        private val oidcUtil: OIDCUtil,
        private val docgenKlient: DokGenKlient,
        private val fagsakService: FagsakService,
        private val behandlingslagerService: BehandlingslagerService
) {
    @GetMapping(path = ["/fagsak/{fagsakId}"])
    fun hentFagsak(@PathVariable fagsakId: Long): ResponseEntity<Ressurs<RestFagsak>> {
        val saksbehandlerId = oidcUtil.getClaim("preferred_username")

        logger.info("{} henter fagsak med id {}", saksbehandlerId ?: "Ukjent", fagsakId)

        val ressurs = Result.runCatching { fagsakService.hentRestFagsak(fagsakId) }
                .fold(
                        onSuccess = { it },
                        onFailure = { e -> Ressurs.failure("Henting av fagsak med fagsakId $fagsakId feilet", e) }
                )

        return ResponseEntity.ok(ressurs)
    }

    @PostMapping(path = ["/fagsak/{fagsakId}/nytt-vedtak"])
    fun nyttVedtak(@PathVariable fagsakId: Long, @RequestBody nyttVedtak: NyttVedtak): ResponseEntity<Ressurs<BehandlingVedtak>> {
        val saksbehandlerId = oidcUtil.getClaim("preferred_username")

        logger.info("{} lager nytt vedtak for fagsak med id {}", saksbehandlerId ?: "Ukjent", fagsakId)

        val behandlingVedtakRessurs: Ressurs<BehandlingVedtak> = Result.runCatching { behandlingslagerService.nyttVedtakForAktivBehandling(fagsakId, nyttVedtak, ansvarligSaksbehandler = saksbehandlerId) }
                .fold(
                        onSuccess = { it },
                        onFailure = { e ->
                            Ressurs.failure("Klarte ikke å opprette nytt vedtak", e)
                        }
                )

        return ResponseEntity.ok(behandlingVedtakRessurs)
    }

    @GetMapping(path = ["/behandling/{behandlingId}/vedtak-html"])
    fun hentHtmlVedtak(@PathVariable behandlingId: Long): Ressurs<String> {
        val saksbehandlerId = oidcUtil.getClaim("preferred_username")
        logger.info("{} henter vedtaksbrev", saksbehandlerId ?: "VL")
        return behandlingslagerService.hentHtmlVedtakForBehandling(behandlingId);
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(BehandlingslagerService::class.java)
    }
}