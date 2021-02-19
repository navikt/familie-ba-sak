package no.nav.familie.ba.sak.infotrygd

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.pdl.internal.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.Ressurs
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
class InfotrygdController(private val infotrygdService: InfotrygdService) {

    @PostMapping(path = ["/hent-infotrygdsaker-for-soker"])
    fun hentInfotrygdsakerForSøker(@RequestBody personIdent: Personident): ResponseEntity<Ressurs<RestInfotrygdsaker>> {
        try {
            val infotrygdsaker = infotrygdService.hentMaskertRestInfotrygdsakerVedManglendeTilgang(personIdent.ident) ?:
                    RestInfotrygdsaker(infotrygdService.hentInfotrygdsakerForSøker(personIdent.ident).bruker)

            return ResponseEntity.ok(Ressurs.success(infotrygdsaker))
        } catch(ex: Throwable) {
            throw Feil(message = "Hent person feilet: ${ex.message}",
                    frontendFeilmelding = "Henting av person med ident '$personIdent' feilet.",
                    throwable = ex)
        }
    }
}

class Personident(val ident: String)

class RestInfotrygdsaker(val saker: List<Sak> = emptyList(),
                         val adressebeskyttelsegradering: ADRESSEBESKYTTELSEGRADERING? = null,
                         val harTilgang: Boolean = true)