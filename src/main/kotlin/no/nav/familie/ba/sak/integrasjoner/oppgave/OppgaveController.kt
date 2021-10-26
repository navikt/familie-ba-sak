package no.nav.familie.ba.sak.integrasjoner.oppgave

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.RessursUtils.illegalState
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPersonInfo
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.oppgave.domene.DataForManuellJournalføring
import no.nav.familie.ba.sak.integrasjoner.oppgave.domene.RestFinnOppgaveRequest
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/oppgave")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class OppgaveController(
    private val oppgaveService: OppgaveService,
    private val fagsakService: FagsakService,
    private val integrasjonClient: IntegrasjonClient,
    private val personopplysningerService: PersonopplysningerService,
    private val tilgangService: TilgangService
) {

    @PostMapping(
        path = ["/hent-oppgaver"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun hentOppgaver(@RequestBody restFinnOppgaveRequest: RestFinnOppgaveRequest): ResponseEntity<Ressurs<FinnOppgaveResponseDto>> =
        try {
            val oppgaver: FinnOppgaveResponseDto =
                oppgaveService.hentOppgaver(restFinnOppgaveRequest.tilFinnOppgaveRequest())
            ResponseEntity.ok().body(Ressurs.success(oppgaver, "Finn oppgaver OK"))
        } catch (e: Throwable) {
            illegalState("Henting av oppgaver feilet", e)
        }

    @PostMapping(path = ["/{oppgaveId}/fordel"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun fordelOppgave(
        @PathVariable(name = "oppgaveId") oppgaveId: Long,
        @RequestParam("saksbehandler") saksbehandler: String
    ): ResponseEntity<Ressurs<String>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "fordele oppgave"
        )

        val oppgaveId =
            oppgaveService.fordelOppgave(
                oppgaveId = oppgaveId,
                saksbehandler = saksbehandler,
                overstyrFordeling = false
            )

        return ResponseEntity.ok().body(Ressurs.success(oppgaveId))
    }

    @PostMapping(path = ["/{oppgaveId}/tilbakestill"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun tilbakestillFordelingPåOppgave(@PathVariable(name = "oppgaveId") oppgaveId: Long): ResponseEntity<Ressurs<Oppgave>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "tilbakestille fordeling på oppgave"
        )

        Result.runCatching {
            oppgaveService.tilbakestillFordelingPåOppgave(oppgaveId)
        }.fold(
            onSuccess = { return ResponseEntity.ok().body(Ressurs.Companion.success(it)) },
            onFailure = { return illegalState("Feil ved tilbakestilling av tildeling på oppgave", it) }
        )
    }

    @GetMapping(path = ["/{oppgaveId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentDataForManuellJournalføring(@PathVariable(name = "oppgaveId") oppgaveId: Long): ResponseEntity<Ressurs<DataForManuellJournalføring>> {

        val oppgave = oppgaveService.hentOppgave(oppgaveId)

        val personIdent = if (oppgave.aktoerId == null) null else {
            integrasjonClient.hentPersonIdent(oppgave.aktoerId) ?: error("Fant ikke personident for aktør id")
        }
        val minimalFagsak = if (personIdent == null) null else fagsakService.hentRestFagsakForPerson(personIdent).data

        val dataForManuellJournalføring = DataForManuellJournalføring(
            oppgave = oppgave,
            journalpost = null,
            person = personIdent?.ident?.let {
                personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(it)
                    .tilRestPersonInfo(it)
            },
            minimalFagsak = minimalFagsak
        )

        val journalpost: Ressurs<Journalpost>? =
            if (oppgave.journalpostId == null) null else integrasjonClient.hentJournalpost(oppgave.journalpostId!!)

        return when {
            journalpost == null -> {
                ResponseEntity.ok(Ressurs.success(dataForManuellJournalføring))
            }
            journalpost.status == Ressurs.Status.SUKSESS -> {
                ResponseEntity.ok(
                    Ressurs.success(
                        dataForManuellJournalføring.copy(
                            journalpost = journalpost.getDataOrThrow()
                        )
                    )
                )
            }
            journalpost.status == Ressurs.Status.FEILET -> ResponseEntity.ok(Ressurs.failure(journalpost.melding))
            journalpost.status == Ressurs.Status.FUNKSJONELL_FEIL -> ResponseEntity.ok(
                Ressurs.funksjonellFeil(
                    journalpost.melding
                )
            )
            journalpost.status == Ressurs.Status.IKKE_TILGANG -> ResponseEntity.ok(
                Ressurs(
                    data = null,
                    stacktrace = null,
                    status = Ressurs.Status.IKKE_TILGANG,
                    melding = journalpost.melding,
                    frontendFeilmelding = "Ikke tilgang til journalpost"
                )
            )
            else -> throw Feil("Ukjent status fra journalpost: ${journalpost.status}")
        }
    }
}
