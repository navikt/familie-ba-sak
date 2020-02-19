package no.nav.familie.ba.sak.mottak

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.FagsakController
import no.nav.familie.ba.sak.behandling.FagsakService
import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.sikkerhet.OIDCUtil
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.exceptions.JwtTokenValidatorException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/behandling")
@ProtectedWithClaims(issuer = "azuread")
class BehandlingController(private val oidcUtil: OIDCUtil,
                       private val behandlingService: BehandlingService,
                       private val fagsakService: FagsakService) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping(path = ["/{behandlingId}/vedtak-html"])
    fun hentHtmlVedtak(@PathVariable behandlingId: Long): Ressurs<String> {
        val saksbehandlerId = oidcUtil.getClaim("preferred_username")
        FagsakController.logger.info("{} henter vedtaksbrev", saksbehandlerId ?: "VL")
        val html = behandlingService.hentHtmlVedtakForBehandling(behandlingId)
        FagsakController.logger.debug(html.data)

        return html
    }

    @PostMapping(path = ["/opprett"])
    fun opprettBehandling(@RequestBody nyBehandling: NyBehandling): ResponseEntity<Ressurs<RestFagsak>> {
        val saksbehandlerId = try {
            oidcUtil.getClaim("preferred_username") ?: "VL"
        } catch (e: JwtTokenValidatorException) {
            "VL"
        }

        logger.info("{} oppretter ny behandling", saksbehandlerId)

        if (nyBehandling.ident.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Ressurs.failure("Søkers ident kan ikke være blank"))
        }

        if (nyBehandling.barnasIdenter.filter { it.isBlank() }.isNotEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Ressurs.failure("Minst et av barna mangler ident"))
        }

        return Result.runCatching { behandlingService.opprettBehandling(nyBehandling) }
                .fold(
                        onFailure = {
                            logger.info("Opprettelse av behandling feilet", it)
                            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body(Ressurs.failure(it.cause?.message ?: it.message, it))
                        },
                        onSuccess = { ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = it.id)) }
                )
    }

    @PostMapping(path = ["/opprettfrahendelse"])
    fun opprettEllerOppdaterBehandlingFraHendelse(@RequestBody nyBehandling: NyBehandlingHendelse): ResponseEntity<Ressurs<RestFagsak>> {
        val saksbehandlerId = "VL"

        logger.info("{} oppretter ny behandling fra hendelse", saksbehandlerId)

        return Result.runCatching { behandlingService.opprettEllerOppdaterBehandlingFraHendelse(nyBehandling) }
                .fold(
                        onFailure = {
                            logger.info("Opprettelse av behandling fra hendelse feilet", it)
                            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .body(Ressurs.failure(it.message, it))
                        },
                        onSuccess = { ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = it.id)) }
                )
    }

}

class NyBehandling(
        val kategori: BehandlingKategori,
        val underkategori: BehandlingUnderkategori,
        val ident: String,
        val barnasIdenter: Array<String>,
        val behandlingType: BehandlingType,
        val journalpostID: String?)

class NyBehandlingHendelse(
        val fødselsnummer: String,
        val barnasFødselsnummer: Array<String>
)