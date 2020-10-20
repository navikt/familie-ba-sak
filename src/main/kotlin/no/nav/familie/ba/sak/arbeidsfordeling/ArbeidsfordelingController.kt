package no.nav.familie.ba.sak.arbeidsfordeling

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/arbeidsfordeling")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class ArbeidsfordelingController(private val fagsakService: FagsakService,
                                 private val behandlingService: BehandlingService,
                                 private val arbeidsfordelingService: ArbeidsfordelingService) {


    @PutMapping(path = ["{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun endreBehandlendeEnhet(@PathVariable behandlingId: Long,
                              @RequestBody
                              endreBehandlendeEnhet: RestEndreBehandlendeEnhet): ResponseEntity<Ressurs<RestFagsak>> {
        if (endreBehandlendeEnhet.begrunnelse.isBlank()) throw FunksjonellFeil(melding = "Begrunnelse kan ikke være tom",
                                                                               frontendFeilmelding = "Du må skrive en begrunnelse for endring av enhet")

        val behandling = behandlingService.hent(behandlingId)
        arbeidsfordelingService.manueltOppdaterBehandlendeEnhet(behandling = behandling,
                                                                endreBehandlendeEnhet = endreBehandlendeEnhet)

        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = behandling.fagsak.id))
    }

    companion object {

        val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
}

data class RestEndreBehandlendeEnhet(
        val enhetId: String,
        val begrunnelse: String
)
