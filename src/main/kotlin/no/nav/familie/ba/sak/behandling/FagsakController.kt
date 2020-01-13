package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.vedtak.NyttVedtak
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.økonomi.ØkonomiService
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
        private val fagsakService: FagsakService,
        private val behandlingService: BehandlingService,
        private val økonomiService: ØkonomiService
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
    fun nyttVedtak(@PathVariable fagsakId: Long, @RequestBody nyttVedtak: NyttVedtak): ResponseEntity<Ressurs<RestFagsak>> {
        val saksbehandlerId = oidcUtil.getClaim("preferred_username")

        logger.info("{} lager nytt vedtak for fagsak med id {}", saksbehandlerId ?: "Ukjent", fagsakId)

        val fagsak: Ressurs<RestFagsak> = Result.runCatching { behandlingService.nyttVedtakForAktivBehandling(fagsakId, nyttVedtak, ansvarligSaksbehandler = saksbehandlerId) }
                .fold(
                        onSuccess = { it },
                        onFailure = { e ->
                            Ressurs.failure("Klarte ikke å opprette nytt vedtak", e)
                        }
                )

        return ResponseEntity.ok(fagsak)
    }

    @PostMapping(path = ["/fagsak/{fagsakId}/iverksett-vedtak"])
    fun iverksettVedtak(@PathVariable fagsakId: Long): ResponseEntity<Ressurs<RestFagsak>> {
        val saksbehandlerId = oidcUtil.getClaim("preferred_username")

        logger.info("{} iverksetter vedtak for fagsak med id {}", saksbehandlerId ?: "Ukjent", fagsakId)

        val behandling = behandlingService.hentBehandlingHvisEksisterer(fagsakId)
                ?: throw Error("Fant ikke behandling på fagsak $fagsakId")

        val behandlingVedtak = behandlingService.hentBehandlingVedtakHvisEksisterer(behandlingId = behandling.id)
                ?: throw Error("Fant ikke aktivt vedtak på behandling ${behandling.id}")

        val fagsak: Ressurs<RestFagsak> = Result.runCatching { økonomiService.iverksettVedtak(behandlingVedtak, saksbehandlerId) }
                .fold(
                        onSuccess = { it },
                        onFailure = { e ->
                            Ressurs.failure("Klarte ikke å iverksette vedtak", e)
                        }
                )

        return ResponseEntity.ok(fagsak)
    }

    @GetMapping(path = ["/behandling/{behandlingId}/vedtak-html"])
    fun hentHtmlVedtak(@PathVariable behandlingId: Long): Ressurs<String> {
        val saksbehandlerId = oidcUtil.getClaim("preferred_username")
        logger.info("{} henter vedtaksbrev", saksbehandlerId ?: "VL")
        val html = behandlingService.hentHtmlVedtakForBehandling(behandlingId)
        logger.debug(html.data)

        return html
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(BehandlingService::class.java)
    }
}