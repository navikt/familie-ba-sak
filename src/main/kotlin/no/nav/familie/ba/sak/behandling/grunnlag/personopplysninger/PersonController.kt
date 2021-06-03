package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger

import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.restDomene.RestPersonInfo
import no.nav.familie.ba.sak.behandling.restDomene.tilRestFagsak
import no.nav.familie.ba.sak.behandling.restDomene.tilRestPersonInfo
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.validering.PersontilgangConstraint
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/person")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class PersonController(private val personopplysningerService: PersonopplysningerService,
                       private val persongrunnlagService: PersongrunnlagService,
                       private val fagsakService: FagsakService) {

    @GetMapping
    fun hentPerson(@RequestHeader personIdent: String): ResponseEntity<Ressurs<RestPersonInfo>> {
        return Result.runCatching {
            personopplysningerService.hentMaskertPersonInfoVedManglendeTilgang(personIdent)
            ?: personopplysningerService.hentPersoninfoMedRelasjoner(personIdent).tilRestPersonInfo(personIdent)
        }
                .fold(
                        onFailure = {
                            throw Feil(message = "Hent person feilet: ${it.message}",
                                       frontendFeilmelding = "Henting av person med ident '$personIdent' feilet.",
                                       throwable = it)
                        },
                        onSuccess = {
                            ResponseEntity.ok(Ressurs.success(it))
                        }
                )
    }

    @GetMapping(path = ["/enkel"])
    @PersontilgangConstraint
    fun hentPersonEnkel(@RequestHeader personIdent: String): ResponseEntity<Ressurs<RestPersonInfo>> {
        return Result.runCatching {
            personopplysningerService.hentPersoninfo(personIdent)
        }
                .fold(
                        onFailure = {
                            when (it) {
                                is Feil -> throw it
                                else -> throw Feil(message = "Hent person feilet: ${it.message}",
                                                   frontendFeilmelding = "Henting av person med ident '$personIdent' feilet.",
                                                   throwable = it)
                            }
                        },
                        onSuccess = {
                            ResponseEntity.ok(Ressurs.success(it.tilRestPersonInfo(personIdent)))
                        }
                )
    }

    @GetMapping(path = ["/oppdater-registeropplysninger/{behandlingId}"])
    fun hentOgOppdaterRegisteropplysninger(@PathVariable behandlingId: Long): ResponseEntity<Ressurs<RestFagsak>> {
        return Result.runCatching {
            val personopplysningGrunnlag = persongrunnlagService.oppdaterRegisteropplysninger(behandlingId)
            fagsakService.hentRestFagsakForPerson(personopplysningGrunnlag.søker.personIdent)
        }
                .fold(
                        onFailure = {
                            when (it) {
                                is Feil -> throw it
                                else -> throw Feil(message = "Feil ved henting og oppdatering av personopplysningsgrunnlag: ${it.message}",
                                                   frontendFeilmelding = "Feilet ved oppdatering av registeropplysninger på behandling '$behandlingId'.",
                                                   throwable = it)
                            }
                        },
                        onSuccess = {
                            ResponseEntity.ok(it)
                        }
                )
    }
}