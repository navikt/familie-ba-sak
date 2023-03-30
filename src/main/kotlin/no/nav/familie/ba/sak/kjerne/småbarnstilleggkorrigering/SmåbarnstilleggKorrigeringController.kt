package no.nav.familie.ba.sak.kjerne.småbarnstilleggkorrigering

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingId
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth

@RestController
@RequestMapping("/api/småbarnstilleggkorrigering")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class SmåbarnstilleggController(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val småbarnstilleggKorrigeringService: SmåbarnstilleggKorrigeringService,
    private val utvidetBehandlingService: UtvidetBehandlingService
) {
    @PostMapping(path = ["/behandling/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun leggTilSmåBarnstilleggPåBehandling(
        @PathVariable behandlingId: Long,
        @RequestBody småbarnstilleggKorrigeringRequest: SmåbarnstilleggKorrigeringRequest
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        val behandling = behandlingHentOgPersisterService.hent(BehandlingId(behandlingId))

        småbarnstilleggKorrigeringService.leggTilSmåbarnstilleggPåBehandling(
            småbarnstilleggKorrigeringRequest.årMåned,
            behandling
        )

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandling.behandlingId)))
    }

    @DeleteMapping(path = ["/behandling/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun fjernSmåbarnstilleggFraMåned(
        @PathVariable behandlingId: Long,
        @RequestBody småBarnstilleggKorrigeringRequest: SmåbarnstilleggKorrigeringRequest
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        val behandling = behandlingHentOgPersisterService.hent(BehandlingId(behandlingId))

        småbarnstilleggKorrigeringService.fjernSmåbarnstilleggPåBehandling(
            småBarnstilleggKorrigeringRequest.årMåned,
            behandling
        )

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandling.behandlingId)))
    }
}

data class SmåbarnstilleggKorrigeringRequest(
    @Schema(
        implementation = String::class,
        example = "2020-12"
    ) val årMåned: YearMonth
)
