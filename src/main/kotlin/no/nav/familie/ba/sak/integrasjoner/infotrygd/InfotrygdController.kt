package no.nav.familie.ba.sak.integrasjoner.infotrygd

import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import no.nav.familie.ba.sak.common.PERSONIDENT_IKKE_GYLDIG_FEILMELDING
import no.nav.familie.ba.sak.common.PERSONIDENT_REGEX
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.kontrakter.ba.infotrygd.Sak
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/infotrygd")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class InfotrygdController(
    private val infotrygdBarnetrygdKlient: InfotrygdBarnetrygdKlient,
    private val personidentService: PersonidentService,
    private val infotrygdService: InfotrygdService,
) {
    @PostMapping(path = ["/hent-infotrygdsaker-for-soker"])
    fun hentInfotrygdsakerForSøker(
        @Valid
        @RequestBody
        personIdent: Personident,
    ): ResponseEntity<Ressurs<RestInfotrygdsaker>> {
        val aktør = personidentService.hentAktør(personIdent.ident)
        val infotrygdsaker =
            infotrygdService.hentMaskertRestInfotrygdsakerVedManglendeTilgang(aktør)
                ?: RestInfotrygdsaker(infotrygdService.hentInfotrygdsakerForSøker(aktør).bruker)

        return ResponseEntity.ok(Ressurs.success(infotrygdsaker))
    }
}

class Personident(
    @field:Pattern(regexp = PERSONIDENT_REGEX, message = PERSONIDENT_IKKE_GYLDIG_FEILMELDING)
    val ident: String,
)

class RestInfotrygdsaker(
    val saker: List<Sak> = emptyList(),
    val adressebeskyttelsegradering: ADRESSEBESKYTTELSEGRADERING? = null,
    val harTilgang: Boolean = true,
)

class RestInfotrygdstønader(
    val adressebeskyttelsegradering: ADRESSEBESKYTTELSEGRADERING? = null,
    val harTilgang: Boolean = true,
)
