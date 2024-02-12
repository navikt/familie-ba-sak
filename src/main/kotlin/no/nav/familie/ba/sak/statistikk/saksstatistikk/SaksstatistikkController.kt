package no.nav.familie.ba.sak.statistikk.saksstatistikk

import no.nav.familie.eksterne.kontrakter.saksstatistikk.BehandlingDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.SakDVH
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/saksstatistikk")
@ProtectedWithClaims(issuer = "azuread")
class SaksstatistikkController(
    private val saksstatistikkService: SaksstatistikkService,
) {
    private val logger = LoggerFactory.getLogger(SaksstatistikkController::class.java)

    @GetMapping(path = ["/behandling/{behandlingId}"])
    fun hentBehandlingDvh(
        @PathVariable(name = "behandlingId", required = true) behandlingId: Long,
    ): BehandlingDVH {
        try {
            return saksstatistikkService.mapTilBehandlingDVH(behandlingId)!!
        } catch (e: Exception) {
            logger.warn("Feil ved henting av sakstatistikk behandling", e)
            throw e
        }
    }

    @GetMapping(path = ["/sak/{fagsakId}"])
    fun hentSakDvh(
        @PathVariable(name = "fagsakId", required = true) fagsakId: Long,
    ): SakDVH {
        try {
            return saksstatistikkService.mapTilSakDvh(fagsakId)!!
        } catch (e: Exception) {
            logger.warn("Feil ved henting av sakstatistikk sak", e)
            throw e
        }
    }
}
