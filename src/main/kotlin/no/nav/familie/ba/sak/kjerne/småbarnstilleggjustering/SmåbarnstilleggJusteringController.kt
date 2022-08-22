package no.nav.familie.ba.sak.kjerne.småbarnstilleggjustering

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
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
@RequestMapping("/api/småbarnstillegg")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class SmåbarnstilleggController(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val småbarnstilleggJusteringService: SmåbarnstilleggJusteringService,
    private val utvidetBehandlingService: UtvidetBehandlingService
) {
    @PostMapping(path = ["/behandling/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun leggTilSmåBarnstilleggPåBehandling(
        @PathVariable behandlingId: Long,
        @RequestBody småbarnstilleggRequest: SmåbarnstilleggRequest
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)

        småbarnstilleggJusteringService.leggTilSmåbarnstilleggPåBehandling(småbarnstilleggRequest.måned, behandling)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId)))
    }

    @DeleteMapping(path = ["/behandling/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun fjernSmåbarnstilleggFraMåned(
        @PathVariable behandlingId: Long,
        @RequestBody småBarnstilleggRequest: SmåbarnstilleggRequest
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)

        småbarnstilleggJusteringService.fjernSmåbarnstilleggPåBehandling(småBarnstilleggRequest.måned, behandling)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId)))
    }
}

data class SmåbarnstilleggRequest(
    @Schema(
        implementation = String::class,
        example = "2020-12"
    ) val måned: YearMonth
)
