package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger

import no.nav.familie.ba.sak.common.RessursResponse.illegalState
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonOnBehalfClient
import no.nav.familie.ba.sak.integrasjoner.domene.Personinfo
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
class PersonController(private val integrasjonOnBehalfClient: IntegrasjonOnBehalfClient) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping
    fun hentPerson(@RequestHeader personIdent: String): ResponseEntity<Ressurs<Personinfo>> {
        return Result.runCatching {
                    integrasjonOnBehalfClient.hentPersoninfo(personIdent)
                }
                .fold(
                        onFailure = {
                            illegalState("Hent person feilet: ${it.message}", it)
                        },
                        onSuccess = { ResponseEntity.ok(Ressurs.success(it)) }
                )
    }
}
