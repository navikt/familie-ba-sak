package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonInfo
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPersonInfo
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.sikkerhet.validering.PersontilgangConstraint
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/person")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class PersonController(
    private val personopplysningerService: PersonopplysningerService,
    private val persongrunnlagService: PersongrunnlagService,
    private val integrasjonClient: IntegrasjonClient,
    private val utvidetBehandlingService: UtvidetBehandlingService,
    private val personidentService: PersonidentService
) {

    @PostMapping(path = ["/ny-ident"])
    fun håndterNyIdent(@RequestBody nyIdent: PersonIdent): ResponseEntity<Ressurs<String>> {
        personidentService.håndterNyIdent(nyIdent)
        return ResponseEntity.ok(Ressurs.success("Håndtert ny ident"))
    }

    @GetMapping
    fun hentPerson(@RequestHeader personIdent: String): ResponseEntity<Ressurs<RestPersonInfo>> {
        val personinfo = integrasjonClient.hentMaskertPersonInfoVedManglendeTilgang(personIdent)
            ?: personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(personIdent)
                .tilRestPersonInfo(personIdent)
        return ResponseEntity.ok(Ressurs.success(personinfo))
    }

    @GetMapping(path = ["/enkel"])
    @PersontilgangConstraint
    fun hentPersonEnkel(@RequestHeader personIdent: String): ResponseEntity<Ressurs<RestPersonInfo>> {
        val personinfo = personopplysningerService.hentPersoninfoEnkel(personIdent)
        return ResponseEntity.ok(Ressurs.success(personinfo.tilRestPersonInfo(personIdent)))
    }

    @GetMapping(path = ["/oppdater-registeropplysninger/{behandlingId}"])
    fun hentOgOppdaterRegisteropplysninger(@PathVariable behandlingId: Long): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        val personopplysningGrunnlag = persongrunnlagService.oppdaterRegisteropplysninger(behandlingId)
        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = personopplysningGrunnlag.behandlingId)))
    }
}
