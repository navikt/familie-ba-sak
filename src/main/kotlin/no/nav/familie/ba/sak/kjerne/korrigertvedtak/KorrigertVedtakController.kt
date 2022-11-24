package no.nav.familie.ba.sak.kjerne.korrigertvedtak

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/korrigertvedtak")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class KorrigertVedtakController(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val korrigertVedtakService: KorrigertVedtakService,
    private val utvidetBehandlingService: UtvidetBehandlingService,
    private val featureToggleService: FeatureToggleService
) {

    @PostMapping(path = ["/behandling/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun opprettKorrigertVedtakPåBehandling(
        @PathVariable behandlingId: Long,
        @RequestBody korrigerVedtakRequest: KorrigerVedtakRequest
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        if (featureToggleService.isEnabled(FeatureToggleConfig.SKAL_KUNNE_KORRIGERE_VEDTAK, false)) {
            throw Feil(
                message = "Togglen familie-ba-sak.kunne-korrigere-vedtak er ikke slått på, og det er ikke mulig å korrigere vedtak",
                frontendFeilmelding = "Korrigering av vedtak er ikke støttet ennå"
            )
        } else {
            val behandling = behandlingHentOgPersisterService.hent(behandlingId)
            val korrigertVedtak = korrigerVedtakRequest.tilKorrigerVedtak(behandling)

            korrigertVedtakService.lagreKorrigertVedtak(korrigertVedtak)

            return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId)))
        }
    }

    @PatchMapping(path = ["/behandling/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun settKorrigertVedtakTilInaktivPåBehandling(
        @PathVariable behandlingId: Long
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        korrigertVedtakService.settKorrigertVedtakPåBehandlingTilInaktiv(behandling)

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)))
    }
}
