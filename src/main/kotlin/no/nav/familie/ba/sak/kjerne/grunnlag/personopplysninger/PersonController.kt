package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import jakarta.validation.Valid
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.ekstern.restDomene.ManuellDødsfallDto
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonInfo
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPersonInfo
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPersonInfoMedNavnOgAdresse
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Fødselsnummer
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
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
    private val familieIntegrasjonerTilgangskontrollService: FamilieIntegrasjonerTilgangskontrollService,
    private val utvidetBehandlingService: UtvidetBehandlingService,
    private val tilgangService: TilgangService,
) {
    @PostMapping
    fun hentPerson(
        @Valid @RequestBody personIdentDto: PersonIdent,
    ): ResponseEntity<Ressurs<RestPersonInfo>> {
        val personIdent = personIdentDto.ident

        // Valider personIdent
        Fødselsnummer(personIdent)

        val aktør = personidentService.hentAktør(personIdent)
        val personinfo =
            familieIntegrasjonerTilgangskontrollService.hentMaskertPersonInfoVedManglendeTilgang(aktør)
                ?: personopplysningerService
                    .hentPdlPersoninfoMedRelasjonerOgRegisterinformasjon(aktør)
                    .tilRestPersonInfo(personIdent)
        return ResponseEntity.ok(Ressurs.success(personinfo))
    }

    @PostMapping(path = ["/enkel"])
    fun hentPersonEnkel(
        @Valid @RequestBody personIdentDto: PersonIdent,
    ): ResponseEntity<Ressurs<RestPersonInfo>> {
        val personIdent = personIdentDto.ident

        // Valider personIdent
        Fødselsnummer(personIdent)

        val aktør = personidentService.hentAktør(personIdent)
        val personinfo =
            familieIntegrasjonerTilgangskontrollService.hentMaskertPersonInfoVedManglendeTilgang(aktør)
                ?: personopplysningerService
                    .hentPersoninfoEnkel(aktør)
                    .tilRestPersonInfo(personIdent)
        return ResponseEntity.ok(Ressurs.success(personinfo))
    }

    @PostMapping(path = ["/adresse"])
    fun hentPersonAdresse(
        @Valid @RequestBody personIdentDto: PersonIdent,
    ): ResponseEntity<Ressurs<RestPersonInfo>> {
        val personIdent = personIdentDto.ident

        // For validering
        Fødselsnummer(personIdent)

        val aktør = personidentService.hentAktør(personIdent)
        val personinfo =
            familieIntegrasjonerTilgangskontrollService.hentMaskertPersonInfoVedManglendeTilgang(aktør)
                ?: personopplysningerService
                    .hentPersoninfoNavnOgAdresse(aktør)
                    .tilRestPersonInfoMedNavnOgAdresse(personIdent)
        return ResponseEntity.ok(Ressurs.success(personinfo))
    }

    @GetMapping(path = ["/oppdater-registeropplysninger/{behandlingId}"])
    fun hentOgOppdaterRegisteropplysninger(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.ACCESS)

        val personopplysningGrunnlag = persongrunnlagService.oppdaterRegisteropplysninger(behandlingId)
        return ResponseEntity.ok(
            Ressurs.success(
                utvidetBehandlingService
                    .lagRestUtvidetBehandling(behandlingId = personopplysningGrunnlag.behandlingId),
            ),
        )
    }

    @PostMapping(path = ["/registrer-manuell-dodsfall/{behandlingId}"])
    fun registrerManuellDødsfallPåPerson(
        @PathVariable behandlingId: Long,
        @RequestBody manuellDødsfallDto: ManuellDødsfallDto,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.ACCESS)

        persongrunnlagService.registrerManuellDødsfallPåPerson(
            behandlingId = BehandlingId(behandlingId),
            personIdent = PersonIdent(manuellDødsfallDto.personIdent),
            dødsfallDato = manuellDødsfallDto.dødsfallDato,
            begrunnelse = manuellDødsfallDto.begrunnelse,
        )

        return ResponseEntity.ok(
            Ressurs.success(
                utvidetBehandlingService
                    .lagRestUtvidetBehandling(behandlingId = behandlingId),
            ),
        )
    }
}
