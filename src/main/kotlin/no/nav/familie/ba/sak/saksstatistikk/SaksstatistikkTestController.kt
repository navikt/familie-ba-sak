package no.nav.familie.ba.sak.saksstatistikk

import no.nav.familie.eksterne.kontrakter.saksstatistikk.BehandlingDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.SakDVH
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.lang.Exception

@RestController
@RequestMapping("/api/saksstatistikk")
@ProtectedWithClaims(issuer = "azuread")
@Profile("!prod")
class SaksstatistikkTestController(
        private val saksstatistikkService: SaksstatistikkService
) {

    val LOG = LoggerFactory.getLogger(SaksstatistikkTestController::class.java)

    @GetMapping(path = ["/behandling/{behandlingId}"])
    @Unprotected
    fun hentBehandlingDvh(@PathVariable(name = "behandlingId", required = true) behandlingId: Long): BehandlingDVH {
        try {
            return saksstatistikkService.mapTilBehandlingDVH(behandlingId, null)!!
        } catch (e: Exception) {
            LOG.warn("Feil ved henting av sakstatistikk behandling", e)
            throw e
        }
    }

    @GetMapping(path = ["/sak/{fagsakId}"])
    @Unprotected
    fun hentSakDvh(@PathVariable(name = "fagsakId", required = true) fagsakId: Long): SakDVH {
        try {
            return saksstatistikkService.mapTilSakDvh(fagsakId)!!
        } catch (e: Exception) {
            LOG.warn("Feil ved henting av sakstatistikk sak", e)
            throw e
        }
    }
}