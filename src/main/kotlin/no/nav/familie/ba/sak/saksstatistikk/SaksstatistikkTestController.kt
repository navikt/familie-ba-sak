package no.nav.familie.ba.sak.saksstatistikk

import no.nav.familie.eksterne.kontrakter.saksstatistikk.BehandlingDVH
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/saksstatistikk")
@ProtectedWithClaims(issuer = "azuread")
@Profile("!prod")
class SaksstatistikkTestController(
        private val saksstatistikkService: SaksstatistikkService
) {

    @GetMapping(path = ["/behandling/{behandlingId}"])
    @Unprotected
    fun hentBehandlingDvh(@PathVariable(name = "behandlingId", required = true) behandlingId: Long) : BehandlingDVH  {
        return saksstatistikkService.loggBehandlingStatus(behandlingId, null)
    }
}