package no.nav.familie.ba.sak.internal.forvalterendepunkt

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonInfoQuery
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.IdentInformasjon
import no.nav.familie.ba.sak.internal.ForvalterPersonInfoDto
import no.nav.familie.ba.sak.internal.HentPersonFraPdlRequest
import no.nav.familie.ba.sak.internal.tilForvalterPersonInfoDto
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.ba.sak.task.PatchFomPåVilkårTilFødselsdato
import no.nav.familie.ba.sak.task.PatchMergetAktørDto
import no.nav.familie.ba.sak.task.PatchMergetIdentDto
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/forvalter/person")
class ForvalterPatchIdent(
    private val opprettTaskService: OpprettTaskService,
    private val personidentService: PersonidentService,
    private val pdlRestKlient: PdlRestKlient,
    private val tilgangService: TilgangService,
) {
    @PostMapping("/hent-alle-identer")
    fun hentAlleIdenter(
        @RequestBody ident: String,
    ): ResponseEntity<List<IdentInformasjon>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Hent alle identer for ident",
        )
        return ResponseEntity.ok(personidentService.hentIdenter(ident, true))
    }

    @PostMapping("/hent-person-fra-pdl")
    @Operation(
        summary = "Henter personinfo fra PDL",
        description =
            "Henter ut detaljer om en person fra PDL. " +
                "Sett de ulike vis-flaggene til true for å inkludere ønsket informasjon i responsen. " +
                "Uthenting av person info logges til tilgangsloggen (audit) med begrunnelsen, " +
                "og til securelogger med hvilke flagg som ble satt.",
    )
    fun hentPersonFraPdl(
        @RequestBody hentPersonFraPdlRequest: HentPersonFraPdlRequest,
    ): ResponseEntity<Ressurs<ForvalterPersonInfoDto>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Hent person fra PDL",
        )

        tilgangService.validerTilgangTilPersoner(
            personIdenter = listOf(hentPersonFraPdlRequest.ident),
            event = AuditLoggerEvent.ACCESS,
            begrunnelse = hentPersonFraPdlRequest.begrunnelse,
        )

        secureLogger.info(
            "${SikkerhetContext.hentSaksbehandlerNavn()} (${SikkerhetContext.hentSaksbehandler()}) " +
                "henter person fra PDL via forvalter-endepunktet for ident=${hentPersonFraPdlRequest.ident} " +
                "med begrunnelse=\"${hentPersonFraPdlRequest.begrunnelse}\" og flagg: $hentPersonFraPdlRequest",
        )

        val personInfo = pdlRestKlient.hentPerson(hentPersonFraPdlRequest.ident, PersonInfoQuery.MED_RELASJONER_OG_REGISTERINFORMASJON)

        return ResponseEntity.ok(Ressurs.success(personInfo.tilForvalterPersonInfoDto(hentPersonFraPdlRequest)))
    }

    @PatchMapping("/patch-fagsak-med-ny-ident")
    fun patchMergetIdent(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description =
                "skalSjekkeAtGammelIdentErHistoriskAvNyIdent - Sjekker at " +
                    "gammel ident er historisk av ny. Hvis man ønsker å patche med en ident hvor den gamle ikke er historisk av ny, så settes " +
                    "denne til false. OBS: Du må da være sikker på at identen man ønsker å patche til er samme person. Dette kan skje hvis " +
                    "identen ikke er merget av folketrygden.",
        )
        @RequestBody
        @Valid
        patchMergetIdentDto: PatchMergetIdentDto,
    ): ResponseEntity<String> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Patch merget ident",
        )

        opprettTaskService.opprettTaskForÅPatcheMergetIdent(patchMergetIdentDto)
        return ResponseEntity.ok("ok")
    }

    @PatchMapping("/patch-fagsak-med-ny-aktoer")
    fun patchAktørIdent(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description =
                "skalSjekkeAtGammelIdentErHistoriskAvNyIdent - Sjekker at " +
                    "gammel aktørId er historisk av ny. Hvis man ønsker å patche med en aktørId hvor den gamle ikke er historisk av ny, så settes " +
                    "denne til false. OBS: Du må da være sikker på at identen man ønsker å patche til er samme person.",
        )
        @RequestBody
        @Valid
        patchMergetAktørDto: PatchMergetAktørDto,
    ): ResponseEntity<String> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Patch merget ident",
        )

        opprettTaskService.opprettTaskForÅPatcheAktørIdent(patchMergetAktørDto)
        return ResponseEntity.ok("ok")
    }

    @PatchMapping("/flytt-vilkaar-fom-dato-til-foedselsdato")
    @Operation(
        summary = "Sett periodeFom på vilkårresultater i behandling som er tidligere enn personens fødselsdato til å være fødselsdato. ",
        description =
            "Dette endepunktet henter alle vilkårresultater og setter periodefom = fødselsdato til personen vilkårresultatet tilhører dersom " +
                "vilkårresultatet sin periodeFom < personens fødselsdato.",
    )
    fun flyttVilkårFomDatoTilFødselsdato(
        @RequestBody behandlinger: Set<Long>,
    ): ResponseEntity<String> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Flytt vilkår fom dato på person til fødselsdato",
        )

        behandlinger.forEach {
            opprettTaskService.opprettTaskForÅPatcheVilkårFom(PatchFomPåVilkårTilFødselsdato(it))
        }
        return ResponseEntity.ok("Ok")
    }
}
