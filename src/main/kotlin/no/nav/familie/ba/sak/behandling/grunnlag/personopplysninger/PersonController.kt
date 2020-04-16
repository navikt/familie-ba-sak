package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger

import no.nav.familie.ba.sak.common.RessursResponse.badRequest
import no.nav.familie.ba.sak.common.RessursResponse.illegalState
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
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
class PersonController(private val integrasjonClient: IntegrasjonClient) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping
    @PersontilgangConstraint
    fun hentPerson(@RequestHeader personIdent: String?,
                   @RequestHeader aktoerId: String?): ResponseEntity<Ressurs<RestPersonInfo>> {
        if (personIdent == null && aktoerId == null) {
            return badRequest("Finner ikke personident eller aktørId på request", null)
        }

        val hentPersonIdent = personIdent
                              ?: (integrasjonClient.hentPersonIdent(aktoerId)?.ident
                                  ?: error("Fant ikke person ident for aktør id"))

        return Result.runCatching {
            integrasjonClient.hentPersoninfoFor(hentPersonIdent)
        }
                .fold(
                        onFailure = {
                            illegalState("Hent person feilet: ${it.message}", it)
                        },
                        onSuccess = {
                            ResponseEntity.ok(Ressurs.success(it.toRestPersonInfo(hentPersonIdent)))
                        }
                )
    }
}
