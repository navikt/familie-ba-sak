package no.nav.familie.ba.sak.sikkerhet

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.NyBehandling
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.core.env.Environment
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/rolletilgang")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class RolletilgangDatabaseTestController(
        private val behandlingService: BehandlingService,
        private val environment: Environment
) {

    @PostMapping(path = ["test-behandlinger"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun opprettBehandling(@RequestBody nyBehandling: NyBehandling): Ressurs<Behandling> {
        if (environment.activeProfiles.any {
                    listOf("prod", "preprod")
                            .contains(it.trim(' '))
                }) error("Controller feilaktig aktivert i miljø")

        return Ressurs.success(behandlingService.opprettBehandling(nyBehandling))
    }
}