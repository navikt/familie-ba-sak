package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.restDomene.TilgangDTO
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
@ProtectedWithClaims(issuer = "azuread")
class TilgangController(private val personopplysningerService: PersonopplysningerService,
                        private val integrasjonClient: IntegrasjonClient) {

    @PostMapping(path = ["tilgang"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentTilgangOgDiskresjonskode(@RequestBody personIdent: String): ResponseEntity<Ressurs<TilgangDTO>> {
        val adressebeskyttelse = personopplysningerService.hentAdressebeskyttelseSomSystembruker(personIdent)
        val tilgang = integrasjonClient.sjekkTilgangTilPersoner(listOf(personIdent)).first().harTilgang
        return ResponseEntity.ok(Ressurs.success(data = TilgangDTO(
                saksbehandlerHarTilgang = tilgang,
                adressebeskyttelsegradering = adressebeskyttelse)))

    }
}