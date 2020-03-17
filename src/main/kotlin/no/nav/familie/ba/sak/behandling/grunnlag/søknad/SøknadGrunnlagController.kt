package no.nav.familie.ba.sak.behandling.grunnlag.søknad

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.steg.RegistrerPersongrunnlagDTO
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.common.RessursResponse
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/behandlinger")
@ProtectedWithClaims(issuer = "azuread")
class RegistrereSøknadController(
        private val fagsakService: FagsakService,
        private val behandlingService: BehandlingService,
        private val stegService: StegService
) {

    @PostMapping(path = ["/{behandlingId}/registrere-søknad-og-hent-persongrunnlag"])
    fun registrereSøknadOgHentPersongrunnlag(@PathVariable behandlingId: Long,
                         @RequestBody søknadDTO: SøknadDTO): ResponseEntity<Ressurs<RestFagsak>> {
        val behandling = behandlingService.hent(behandlingId = behandlingId)

        return Result.runCatching {
                    stegService.håndterSøknad(behandling, søknadDTO)
                    stegService.håndterPersongrunnlag(
                            behandling,
                            RegistrerPersongrunnlagDTO(ident = søknadDTO.søkerMedOpplysninger.ident,
                                                       barnasIdenter = søknadDTO.barnaMedOpplysninger.map { it.ident }))
                }
                .fold(
                        onSuccess = { ResponseEntity.ok(fagsakService.hentRestFagsak(behandling.fagsak.id)) },
                        onFailure = {
                            return RessursResponse.badRequest((it.cause?.message ?: it.message).toString())
                        }
                )
    }
}