package no.nav.familie.ba.sak.kjerne.grunnlag.søknad

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/behandlinger")
@ProtectedWithClaims(issuer = "azuread")
class GrunnlagController(
        private val fagsakService: FagsakService,
        private val behandlingService: BehandlingService,
        private val persongrunnlagService: PersongrunnlagService,
        private val stegService: StegService
) {

    @PostMapping(path = ["/{behandlingId}/registrere-søknad-og-hent-persongrunnlag"],
                 produces = [MediaType.APPLICATION_JSON_VALUE])
    fun registrereSøknadOgHentPersongrunnlagV3(@PathVariable behandlingId: Long,
                                               @RequestBody
                                               restRegistrerSøknad: RestRegistrerSøknad): ResponseEntity<Ressurs<RestFagsak>> {
        val behandling = behandlingService.hent(behandlingId = behandlingId)

        return Result.runCatching {
            stegService.håndterSøknad(behandling = behandling, restRegistrerSøknad = restRegistrerSøknad)
        }
                .fold(
                        onSuccess = { ResponseEntity.ok(fagsakService.hentRestFagsak(behandling.fagsak.id)) },
                        onFailure = {
                            throw it
                        }
                )
    }

    @PostMapping(path = ["/{behandlingId}/legg-til-barn"],
                 produces = [MediaType.APPLICATION_JSON_VALUE])
    fun leggTilBarnIPersonopplysningsgrunnlag(@PathVariable behandlingId: Long,
                                              @RequestBody personident: String): ResponseEntity<Ressurs<RestFagsak>> {
        val behandling = behandlingService.hent(behandlingId = behandlingId)

        return Result.runCatching {
            persongrunnlagService.leggTilBarnIPersonopplysningsgrunnlag(behandling = behandling,
                                                                        nyttBarnIdent = personident)
        }
                .fold(
                        onSuccess = { ResponseEntity.ok(fagsakService.hentRestFagsak(behandling.fagsak.id)) },
                        onFailure = {
                            throw it
                        }
                )
    }
}