package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger

import no.nav.familie.ba.sak.behandling.restDomene.RestPersonInfo
import no.nav.familie.ba.sak.behandling.restDomene.toRestPersonInfo
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.validering.PersontilgangConstraint
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/person")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class PersonController(private val personopplysningerService: PersonopplysningerService) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping
    @PersontilgangConstraint
    fun hentPerson(@RequestHeader personIdent: String): ResponseEntity<Ressurs<RestPersonInfo>> {
        return Result.runCatching {
            personopplysningerService.hentPersoninfoMedRelasjoner(personIdent)
        }
                .fold(
                        onFailure = {
                            throw Feil(message = "Hent person feilet: ${it.message}",
                                       frontendFeilmelding = "Henting av person med ident '$personIdent' feilet.")
                        },
                        onSuccess = {
                            ResponseEntity.ok(Ressurs.success(it.toRestPersonInfo(personIdent)))
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
                                                   frontendFeilmelding = "Henting av person med ident '$personIdent' feilet.")
                            }
                        },
                        onSuccess = {
                            ResponseEntity.ok(Ressurs.success(it.toRestPersonInfo(personIdent)))
                        }
                )
    }
}
