package no.nav.familie.ba.sak.mottak

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.FagsakController
import no.nav.familie.ba.sak.behandling.FagsakService
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.sikkerhet.OIDCUtil
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.exceptions.JwtTokenValidatorException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
@ProtectedWithClaims(issuer = "azuread")
class MottakController(
        private val oidcUtil: OIDCUtil,
        private val behandlingService: BehandlingService,
        private val fagsakService: FagsakService
) {

    @PostMapping(path = ["/behandling/opprett"])
    fun opprettBehandling(@RequestBody nyBehandling: NyBehandling): ResponseEntity<Ressurs<RestFagsak>> {
        val saksbehandlerId = try {
            oidcUtil.getClaim("preferred_username") ?: "VL"
        } catch (e: JwtTokenValidatorException) {
            "VL"
        }

        FagsakController.logger.info("{} oppretter ny behandling", saksbehandlerId)

        return Result.runCatching { behandlingService.opprettBehandling(nyBehandling) }
                .fold(
                        onFailure = {
                            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body(Ressurs.failure("Opprettelse av behandling feilet", it))
                        },
                        onSuccess = { ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = it.id)) }
                )
    }
}

class NyBehandling(val fødselsnummer: String,
                   val barnasFødselsnummer: Array<String>,
                   val behandlingType: BehandlingType,
                   val journalpostID: String?)
