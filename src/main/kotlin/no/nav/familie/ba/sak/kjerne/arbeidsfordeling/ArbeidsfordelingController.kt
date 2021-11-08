package no.nav.familie.ba.sak.kjerne.arbeidsfordeling

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/arbeidsfordeling")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class ArbeidsfordelingController(
    private val behandlingService: BehandlingService,
    private val utvidetBehandlingService: UtvidetBehandlingService,
    private val arbeidsfordelingService: ArbeidsfordelingService
) {

    @PutMapping(path = ["{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun endreBehandlendeEnhet(
        @PathVariable behandlingId: Long,
        @RequestBody
        endreBehandlendeEnhet: RestEndreBehandlendeEnhet
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        if (endreBehandlendeEnhet.begrunnelse.isBlank()) throw FunksjonellFeil(
            melding = "Begrunnelse kan ikke være tom",
            frontendFeilmelding = "Du må skrive en begrunnelse for endring av enhet"
        )

        val behandling = behandlingService.hent(behandlingId)
        arbeidsfordelingService.manueltOppdaterBehandlendeEnhet(
            behandling = behandling,
            endreBehandlendeEnhet = endreBehandlendeEnhet
        )

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandling.id)))
    }
}

data class RestEndreBehandlendeEnhet(
    val enhetId: String,
    val begrunnelse: String
)
