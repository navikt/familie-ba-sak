package no.nav.familie.ba.sak.kjerne.fagsak

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.RessursUtils
import no.nav.familie.ba.sak.common.RessursUtils.illegalState
import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsakDeltager
import no.nav.familie.ba.sak.ekstern.restDomene.RestHentFagsakForPerson
import no.nav.familie.ba.sak.ekstern.restDomene.RestSøkParam
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
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
@RequestMapping("/api/fagsaker")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class FagsakController(
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService,
    private val fagsakService: FagsakService,
    private val stegService: StegService,
    private val tilgangService: TilgangService,
    private val tilbakekrevingService: TilbakekrevingService,
) {

    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentEllerOpprettFagsak(@RequestBody fagsakRequest: FagsakRequest): ResponseEntity<Ressurs<RestFagsak>> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} henter eller oppretter ny fagsak")

        return Result.runCatching { fagsakService.hentEllerOpprettFagsak(fagsakRequest) }
            .fold(
                onSuccess = { ResponseEntity.status(HttpStatus.CREATED).body(it) },
                onFailure = { throw it }
            )
    }

    @GetMapping(path = ["/{fagsakId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentFagsak(@PathVariable fagsakId: Long): ResponseEntity<Ressurs<RestFagsak>> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} henter fagsak med id $fagsakId")

        val fagsak = fagsakService.hentRestFagsak(fagsakId)
        return ResponseEntity.ok().body(fagsak)
    }

    @PostMapping(path = ["/sok"])
    fun søkFagsak(@RequestBody søkParam: RestSøkParam): ResponseEntity<Ressurs<List<RestFagsakDeltager>>> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} søker fagsak")

        val fagsakDeltagere = fagsakService.hentFagsakDeltager(søkParam.personIdent)
        return ResponseEntity.ok().body(Ressurs.success(fagsakDeltagere))
    }

    @PostMapping(path = ["/sok/fagsakdeltagere"])
    fun oppgiFagsakdeltagere(@RequestBody restSøkParam: RestSøkParam): ResponseEntity<Ressurs<List<RestFagsakDeltager>>> {
        return Result.runCatching {
            fagsakService.oppgiFagsakdeltagere(
                restSøkParam.personIdent,
                restSøkParam.barnasIdenter
            )
        }
            .fold(
                onSuccess = { ResponseEntity.ok(Ressurs.success(it)) },
                onFailure = {
                    logger.info("Henting av fagsakdeltagere feilet.")
                    secureLogger.info("Henting av fagsakdeltagere feilet: ${it.message}", it)
                    ResponseEntity
                        .status(if (it is Feil) it.httpStatus else HttpStatus.OK)
                        .body(
                            Ressurs.failure(
                                error = it,
                                errorMessage = "Henting av fagsakdeltagere feilet: ${it.message}"
                            )
                        )
                }
            )
    }

    @PostMapping(path = ["/hent-fagsak-paa-person"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentRestFagsak(@RequestBody request: RestHentFagsakForPerson): ResponseEntity<Ressurs<RestFagsak>> {

        return Result.runCatching {
            fagsakService.hentRestFagsakForPerson(PersonIdent(request.personIdent))
        }.fold(
            onSuccess = { return ResponseEntity.ok().body(it) },
            onFailure = { illegalState("Ukjent feil ved henting data for manuell journalføring.", it) }
        )
    }

    @PostMapping(path = ["/{fagsakId}/send-til-beslutter"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun sendBehandlingTilBeslutter(
        @PathVariable fagsakId: Long,
        @RequestParam behandlendeEnhet: String
    ): ResponseEntity<Ressurs<RestFagsak>> {
        val behandling = behandlingService.hentAktivForFagsak(fagsakId)
            ?: return RessursUtils.notFound("Fant ikke behandling på fagsak $fagsakId")

        stegService.håndterSendTilBeslutter(behandling, behandlendeEnhet)
        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId))
    }

    @PostMapping(path = ["/{fagsakId}/iverksett-vedtak"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun iverksettVedtak(
        @PathVariable fagsakId: Long,
        @RequestBody restBeslutningPåVedtak: RestBeslutningPåVedtak
    ): ResponseEntity<Ressurs<RestFagsak>> {
        val behandling = behandlingService.hentAktivForFagsak(fagsakId)
            ?: return RessursUtils.notFound("Fant ikke behandling på fagsak $fagsakId")

        stegService.håndterBeslutningForVedtak(behandling, restBeslutningPåVedtak)
        return ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId))
    }

    @GetMapping(path = ["/{fagsakId}/har-apen-tilbakekreving"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun harÅpenTilbakekreving(@PathVariable fagsakId: Long): ResponseEntity<Ressurs<Boolean>> {
        return ResponseEntity.ok(
            Ressurs.success(tilbakekrevingService.søkerHarÅpenTilbakekreving(fagsakId))
        )
    }

    @GetMapping(path = ["/{fagsakId}/opprett-tilbakekreving"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun opprettTilbakekrevingsbehandling(@PathVariable fagsakId: Long): Ressurs<String> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "opprette tilbakekrevingbehandling"
        )

        return tilbakekrevingService.opprettTilbakekrevingsbehandlingManuelt(fagsakId)
    }

    @PostMapping(path = ["/annullerFoedsel"])
    fun behandleAnnullertFødsel(@RequestBody barnasIdenter: List<String>): Ressurs<String>{
        fagsakService.behandleAnnullertFødsel(barnasIdenter)
        return Ressurs.success("Ok", "Ok")
    }

    companion object {

        private val logger: Logger = LoggerFactory.getLogger(FagsakController::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}

data class FagsakRequest(
    val personIdent: String?,
    val aktørId: String? = null
)

data class RestBeslutningPåVedtak(
    val beslutning: Beslutning,
    val begrunnelse: String? = null,
    val kontrollerteSider: List<String> = emptyList()
)

enum class Beslutning {
    GODKJENT,
    UNDERKJENT;

    fun erGodkjent() = this == GODKJENT
}
