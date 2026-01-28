package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import io.swagger.v3.oas.annotations.Operation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import no.nav.familie.ba.sak.common.RessursUtils.badRequest
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.kjerne.behandling.HenleggÅrsak
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

const val SATSENDRING = "Satsendring"

@RestController
@RequestMapping("/api/satsendring")
@ProtectedWithClaims(issuer = "azuread")
class SatsendringController(
    private val startSatsendring: StartSatsendring,
    private val tilgangService: TilgangService,
    private val opprettTaskService: OpprettTaskService,
    private val satsendringService: SatsendringService,
    private val satskjøringRepository: SatskjøringRepository,
) {
    @GetMapping(path = ["/kjorsatsendring/{fagsakId}"])
    fun utførSatsendringITaskPåFagsak(
        @PathVariable fagsakId: Long,
    ): ResponseEntity<Ressurs<String>> {
        startSatsendring.opprettSatsendringForFagsak(fagsakId)
        return ResponseEntity.ok(Ressurs.success("Trigget satsendring for fagsak $fagsakId"))
    }

    @PostMapping(path = ["/kjorsatsendring"])
    fun utførSatsendringITaskPåFagsaker(
        @RequestBody fagsaker: Set<Long>,
    ): ResponseEntity<Ressurs<String>> {
        fagsaker.forEach { startSatsendring.opprettSatsendringForFagsak(it) }
        return ResponseEntity.ok(Ressurs.success("Trigget satsendring for fagsakene $fagsaker"))
    }

    @PutMapping(path = ["/{fagsakId}/kjor-satsendring-synkront"])
    fun utførSatsendringSynkrontPåFagsak(
        @PathVariable fagsakId: Long,
    ): ResponseEntity<Ressurs<Unit>> {
        tilgangService.validerTilgangTilHandlingOgFagsak(
            fagsakId = fagsakId,
            handling = "Valider vi kan kjøre satsendring",
            event = AuditLoggerEvent.UPDATE,
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
        )

        startSatsendring.gjennomførSatsendringManuelt(fagsakId)
        return ResponseEntity.ok(Ressurs.success(Unit))
    }

    @GetMapping(path = ["/{fagsakId}/kan-kjore-satsendring"])
    fun kanKjøreSatsendringPåFagsak(
        @PathVariable fagsakId: Long,
    ): ResponseEntity<Ressurs<Boolean>> = ResponseEntity.ok(Ressurs.success(startSatsendring.kanGjennomføreSatsendringManuelt(fagsakId)))

    @PostMapping(path = ["/kjorsatsendringForListeMedIdenter"])
    fun utførSatsendringPåListeIdenter(
        @RequestBody listeMedIdenter: Set<String>,
    ): ResponseEntity<Ressurs<String>> {
        listeMedIdenter.forEach {
            startSatsendring.sjekkOgOpprettSatsendringVedGammelSats(it)
        }
        return ResponseEntity.ok(Ressurs.success("Trigget satsendring for liste med identer ${listeMedIdenter.size}"))
    }

    @PostMapping(path = ["/henleggBehandlingerMedLangFristSenereEnn/{valideringsdato}"])
    fun henleggBehandlingerMedLangLiggetid(
        @RequestBody behandlinger: Set<String>,
        @PathVariable valideringsdato: String,
    ): ResponseEntity<Ressurs<String>> {
        val dato =
            try {
                LocalDate.parse(valideringsdato).also { assert(it.isAfter(LocalDate.now().plusMonths(1))) }
            } catch (e: Exception) {
                return badRequest("Ugyldig dato", e)
            }
        behandlinger.forEach {
            opprettTaskService.opprettHenleggBehandlingTask(
                behandlingId = it.toLong(),
                årsak = HenleggÅrsak.TEKNISK_VEDLIKEHOLD,
                begrunnelse = SATSENDRING,
                validerOppgavefristErEtterDato = dato,
            )
        }
        return ResponseEntity.ok(Ressurs.Companion.success("Trigget henleggelse for ${behandlinger.size} behandlinger"))
    }

    @PostMapping(path = ["/saker-uten-sats"])
    fun finnSakerUtenSisteSats(): ResponseEntity<Pair<String, String>> {
        val callId = UUID.randomUUID().toString()
        val scope = CoroutineScope(SupervisorJob())
        scope.launch {
            satsendringService.finnLøpendeFagsakerUtenSisteSats(callId)
        }
        return ResponseEntity.ok(Pair("callId", callId))
    }

    @PostMapping(path = ["/uferdige-satskjoringer"])
    fun finnFeiledeSatskjøringer(
        @RequestBody finnUferdigeSatskjøringerRequest: FinnUferdigeSatskjøringerRequest,
    ): ResponseEntity<Ressurs<List<Long>>> {
        tilgangService.verifiserHarTilgangTilHandling(BehandlerRolle.FORVALTER, "Se satskjøringer som er forsøkt kjørt men som ikke er ferdige")

        val feiledeSatskjøringer =
            satsendringService.finnUferdigeSatskjøringer(
                feiltyper = finnUferdigeSatskjøringerRequest.feiltype,
                satsTidspunkt = finnUferdigeSatskjøringerRequest.satsTid,
            )

        return ResponseEntity.ok(Ressurs.success(feiledeSatskjøringer))
    }

    @DeleteMapping(path = ["/satskjoringer"])
    fun slettSatskjøringer(
        @RequestBody slettSatskjøringerRequest: SlettSatskjøringerRequest,
    ): ResponseEntity<Ressurs<String>> {
        tilgangService.verifiserHarTilgangTilHandling(BehandlerRolle.FORVALTER, "Slett satskjøringer som ikke er ferdigkjørt.")

        satsendringService.slettSatskjøringer(slettSatskjøringerRequest.fagsakIder, slettSatskjøringerRequest.satsTid)
        return ResponseEntity.ok(Ressurs.success("Slettet satskjøringer for fagsakIder: ${slettSatskjøringerRequest.fagsakIder}"))
    }

    @PostMapping("/satsendringer/{satstid}/feiltype/{feiltype}/rekjør")
    @Operation(
        summary = "Rekjør satsendringer med feiltype lik feiltypen som er sendt inn",
        description =
            "Dette endepunktet sletter alle rader fra Satskjøring der ferdigtid ikke er satt og med feiltypen som er sendt inn. " +
                "Det gjør at satsendringen kjøres på nytt på fagsaken.",
    )
    fun rekjørSatsendringMedFeiltype(
        @PathVariable satstid: YearMonth,
        @PathVariable feiltype: String,
    ): ResponseEntity<String> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Rekjør satsendring med feiltype",
        )

        val satskjøringerSomSkalRekjøres = satskjøringRepository.finnPåFeilTypeOgFerdigTidNull(feiltype, satstid)
        satskjøringRepository.deleteAll(satskjøringerSomSkalRekjøres)
        return ResponseEntity.ok("Ok")
    }
}

data class FinnUferdigeSatskjøringerRequest(
    val feiltype: List<SatsendringSvar> = SatsendringSvar.entries,
    val satsTid: YearMonth = StartSatsendring.hentAktivSatsendringstidspunkt(),
)

data class SlettSatskjøringerRequest(
    val fagsakIder: Set<Long>,
    val satsTid: YearMonth = StartSatsendring.hentAktivSatsendringstidspunkt(),
)
