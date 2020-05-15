package no.nav.familie.ba.sak.behandling.grunnlag.søknad

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.behandling.restDomene.SøknadDTO
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.common.RessursUtils.illegalState
import no.nav.familie.ba.sak.common.RessursUtils.notFound
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/behandlinger")
@ProtectedWithClaims(issuer = "azuread")
class SøknadGrunnlagController(
        private val fagsakService: FagsakService,
        private val behandlingService: BehandlingService,
        private val stegService: StegService,
        private val søknadGrunnlagService: SøknadGrunnlagService
) {

    @PostMapping(path = ["/{behandlingId}/registrere-søknad-og-hent-persongrunnlag"],
                 produces = [MediaType.APPLICATION_JSON_VALUE])
    fun registrereSøknadOgHentPersongrunnlag(@PathVariable behandlingId: Long,
                                             @RequestBody søknadDTO: SøknadDTO): ResponseEntity<Ressurs<RestFagsak>> {
        val behandling = behandlingService.hent(behandlingId = behandlingId)

        return Result.runCatching {
                    stegService.håndterSøknad(behandling, RestRegistrerSøknad(søknad = søknadDTO, bekreftEndringerViaFrontend = true))
        }
                .fold(
                        onSuccess = { ResponseEntity.ok(fagsakService.hentRestFagsak(behandling.fagsak.id)) },
                        onFailure = {
                            throw it
                        }
                )
    }

    @PostMapping(path = ["/{behandlingId}/registrere-søknad-og-hent-persongrunnlag/v2"],
                 produces = [MediaType.APPLICATION_JSON_VALUE])
    fun registrereSøknadOgHentPersongrunnlagV2(@PathVariable behandlingId: Long,
                                             @RequestBody restRegistrerSøknad: RestRegistrerSøknad): ResponseEntity<Ressurs<RestFagsak>> {
        val behandling = behandlingService.hent(behandlingId = behandlingId)

        return Result.runCatching {
            stegService.håndterSøknad(behandling, restRegistrerSøknad)
        }
                .fold(
                        onSuccess = { ResponseEntity.ok(fagsakService.hentRestFagsak(behandling.fagsak.id)) },
                        onFailure = {
                            throw it
                        }
                )
    }

    @GetMapping(path = ["/{behandlingId}/søknad"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentSøknad(@PathVariable behandlingId: Long): ResponseEntity<Ressurs<SøknadDTO>> {
        return Result.runCatching { søknadGrunnlagService.hentAktiv(behandlingId) }
                .fold(
                        onSuccess = {
                            when (it) {
                                null -> return notFound("Fant ikke søknadsgrunnlag å behandling")
                                else -> ResponseEntity.ok(Ressurs.success(it.hentSøknadDto()))
                            }
                        },
                        onFailure = {
                            return illegalState((it.cause?.message ?: it.message).toString(), it)
                        }
                )
    }
}