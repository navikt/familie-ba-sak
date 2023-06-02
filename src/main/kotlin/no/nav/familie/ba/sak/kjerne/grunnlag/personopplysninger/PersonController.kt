package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonInfo
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPersonInfo
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPersonInfoMedNavnOgAdresse
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
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
    private val personidentService: PersonidentService,
    private val familieIntegrasjonerTilgangskontrollClient: FamilieIntegrasjonerTilgangskontrollClient,
    private val utvidetBehandlingService: UtvidetBehandlingService,
    private val tilgangService: TilgangService,
) {

    @GetMapping
    fun hentPerson(
        @RequestHeader personIdent: String,
        @RequestBody personIdentBody: PersonIdent?,
    ): ResponseEntity<Ressurs<RestPersonInfo>> {
        val aktør = personidentService.hentAktør(personIdent)
        val personinfo = familieIntegrasjonerTilgangskontrollClient.hentMaskertPersonInfoVedManglendeTilgang(aktør)
            ?: personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(aktør)
                .tilRestPersonInfo(personIdent)
        return ResponseEntity.ok(Ressurs.success(personinfo))
    }

    @PostMapping(path = ["/personidenterMedStrengtFortroligGradering"])
    fun returnerPersonIdenterMedStrengtFortroligGradering(
        @RequestBody personIdenter: List<String>,
    ): ResponseEntity<Ressurs<List<String>>> {
        val personIdenterMedStrengtFortroligGradering =
            familieIntegrasjonerTilgangskontrollClient.returnerPersonerMedAdressebeskyttelse(personIdenter)
        return ResponseEntity.ok(Ressurs.success(personIdenterMedStrengtFortroligGradering))
    }

    @GetMapping(path = ["/enkel"])
    fun hentPersonEnkel(
        @RequestHeader personIdent: String,
        @RequestBody personIdentBody: PersonIdent?,
    ): ResponseEntity<Ressurs<RestPersonInfo>> {
        val aktør = personidentService.hentAktør(personIdent)
        val personinfo = familieIntegrasjonerTilgangskontrollClient.hentMaskertPersonInfoVedManglendeTilgang(aktør)
            ?: personopplysningerService.hentPersoninfoEnkel(aktør)
                .tilRestPersonInfo(personIdent)
        return ResponseEntity.ok(Ressurs.success(personinfo))
    }

    @GetMapping(path = ["/adresse"])
    fun hentPersonAdresse(
        @RequestHeader personIdent: String,
    ): ResponseEntity<Ressurs<RestPersonInfo>> {
        val aktør = personidentService.hentAktør(personIdent)
        val personinfo = familieIntegrasjonerTilgangskontrollClient.hentMaskertPersonInfoVedManglendeTilgang(aktør)
            ?: personopplysningerService.hentPersoninfoNavnOgAdresse(aktør)
                .tilRestPersonInfoMedNavnOgAdresse(personIdent)
        return ResponseEntity.ok(Ressurs.success(personinfo))
    }

    @GetMapping(path = ["/oppdater-registeropplysninger/{behandlingId}"])
    fun hentOgOppdaterRegisteropplysninger(@PathVariable behandlingId: Long): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.ACCESS)

        val personopplysningGrunnlag = persongrunnlagService.oppdaterRegisteropplysninger(behandlingId)
        return ResponseEntity.ok(
            Ressurs.success(
                utvidetBehandlingService
                    .lagRestUtvidetBehandling(behandlingId = personopplysningGrunnlag.behandlingId),
            ),
        )
    }
}
