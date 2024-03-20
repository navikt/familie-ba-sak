package no.nav.familie.ba.sak.internal

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.integrasjoner.ecb.ECBService
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.oppgave.domene.OppgaveRepository
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiService
import no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering.AutovedtakMånedligValutajusteringService
import no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering.MånedligValutajusteringScheduler
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg.RestartAvSmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.task.GrensesnittavstemMotOppdrag
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.ba.sak.task.PatchFomPåVilkårTilFødselsdato
import no.nav.familie.ba.sak.task.PatchMergetIdentDto
import no.nav.familie.ba.sak.task.internkonsistensavstemming.OpprettInternKonsistensavstemmingTaskerTask
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.internal.TaskService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.concurrent.thread

@RestController
@RequestMapping("/api/forvalter")
@ProtectedWithClaims(issuer = "azuread")
class ForvalterController(
    private val oppgaveRepository: OppgaveRepository,
    private val integrasjonClient: IntegrasjonClient,
    private val restartAvSmåbarnstilleggService: RestartAvSmåbarnstilleggService,
    private val forvalterService: ForvalterService,
    private val ecbService: ECBService,
    private val testVerktøyService: TestVerktøyService,
    private val tilgangService: TilgangService,
    private val økonomiService: ØkonomiService,
    private val opprettTaskService: OpprettTaskService,
    private val taskService: TaskService,
    private val satskjøringRepository: SatskjøringRepository,
    private val autovedtakMånedligValutajusteringService: AutovedtakMånedligValutajusteringService,
    private val envService: EnvService,
    private val månedligValutajusteringScheduler: MånedligValutajusteringScheduler,
) {
    private val logger: Logger = LoggerFactory.getLogger(ForvalterController::class.java)

    @PostMapping(
        path = ["/ferdigstill-oppgaver"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun ferdigstillListeMedOppgaver(
        @RequestBody oppgaveListe: List<Long>,
    ): ResponseEntity<String> {
        var antallFeil = 0
        oppgaveListe.forEach { oppgaveId ->
            Result.runCatching {
                ferdigstillOppgave(oppgaveId)
            }.fold(
                onSuccess = { logger.info("Har ferdigstilt oppgave med oppgaveId=$oppgaveId") },
                onFailure = {
                    logger.warn("Klarte ikke å ferdigstille oppgaveId=$oppgaveId", it)
                    antallFeil = antallFeil.inc()
                },
            )
        }
        return ResponseEntity.ok("Ferdigstill oppgaver kjørt. Antall som ikke ble ferdigstilt: $antallFeil")
    }

    @PostMapping(
        path = ["/start-manuell-restart-av-smaabarnstillegg-jobb/skalOppretteOppgaver/{skalOppretteOppgaver}"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun triggManuellStartAvSmåbarnstillegg(
        @PathVariable skalOppretteOppgaver: Boolean = true,
    ): ResponseEntity<String> {
        restartAvSmåbarnstilleggService.finnOgOpprettetOppgaveForSmåbarnstilleggSomSkalRestartesIDenneMåned(
            skalOppretteOppgaver,
        )
        return ResponseEntity.ok("OK")
    }

    private fun ferdigstillOppgave(oppgaveId: Long) {
        integrasjonClient.ferdigstillOppgave(oppgaveId)
        oppgaveRepository.findByGsakId(oppgaveId.toString()).also {
            if (it != null && !it.erFerdigstilt) {
                it.erFerdigstilt = true
                oppgaveRepository.saveAndFlush(it)
            }
        }
    }

    @PostMapping(path = ["/lag-og-send-utbetalingsoppdrag-til-økonomi"])
    fun lagOgSendUtbetalingsoppdragTilØkonomi(
        @RequestBody behandlinger: Set<Long>,
    ): ResponseEntity<String> {
        behandlinger.forEach {
            try {
                forvalterService.lagOgSendUtbetalingsoppdragTilØkonomiForBehandling(it)
            } catch (exception: Exception) {
                secureLogger.info(
                    "Kunne ikke sende behandling med id $it til økonomi" +
                        "\n$exception",
                )
            }
        }

        return ResponseEntity.ok("OK")
    }

    @PostMapping("/kjor-satsendring-uten-validering")
    @Transactional
    fun kjørSatsendringFor(
        @RequestBody fagsakListe: List<Long>,
    ) {
        fagsakListe.parallelStream().forEach { fagsakId ->
            try {
                logger.info("Kjører satsendring uten validering for $fagsakId")
                forvalterService.kjørForenkletSatsendringFor(fagsakId)
            } catch (e: Exception) {
                logger.warn("Klarte ikke kjøre satsendring for fagsakId=$fagsakId", e)
            }
        }
    }

    @PostMapping("/identifiser-utbetalinger-over-100-prosent")
    fun identifiserUtbetalingerOver100Prosent(): ResponseEntity<Pair<String, String>> {
        val callId = UUID.randomUUID().toString()
        thread {
            forvalterService.identifiserUtbetalingerOver100Prosent(callId)
        }
        return ResponseEntity.ok(Pair("callId", callId))
    }

    @GetMapping("/hentValutakurs/")
    fun finnFagsakerSomSkalAvsluttes(
        @RequestParam valuta: String,
        @RequestParam dato: LocalDate,
    ): ResponseEntity<BigDecimal> {
        if (!valuta.matches(Regex("[A-Z]{3}"))) {
            throw Feil("Valutakode må ha store bokstaver og være tre bokstaver lang")
        }
        return ResponseEntity.ok(ecbService.hentValutakurs(valuta, dato))
    }

    @GetMapping("/finnÅpneFagsakerMedFlereMigreringsbehandlingerOgLøpendeSakIInfotrygd/{fraÅrMåned}")
    fun finnÅpneFagsakerMedFlereMigreringsbehandlingerOgLøpendeSakIInfotrygd(
        @PathVariable fraÅrMåned: YearMonth,
    ): ResponseEntity<List<Pair<Long, String>>> {
        val åpneFagsakerMedFlereMigreringsbehandlingerOgLøpendeSakIInfotrygd =
            forvalterService.finnÅpneFagsakerMedFlereMigreringsbehandlingerOgLøpendeSakIInfotrygd(fraÅrMåned)
        logger.info("Følgende fagsaker har flere migreringsbehandlinger og løpende sak i Infotrygd: $åpneFagsakerMedFlereMigreringsbehandlingerOgLøpendeSakIInfotrygd")
        return ResponseEntity.ok(åpneFagsakerMedFlereMigreringsbehandlingerOgLøpendeSakIInfotrygd)
    }

    @GetMapping("/finnÅpneFagsakerMedFlereMigreringsbehandlinger/{fraÅrMåned}")
    fun finnÅpneFagsakerMedFlereMigreringsbehandlinger(
        @PathVariable fraÅrMåned: YearMonth,
    ): ResponseEntity<List<Pair<Long, String>>> {
        val åpneFagsakerMedFlereMigreringsbehandlinger =
            forvalterService.finnÅpneFagsakerMedFlereMigreringsbehandlinger(fraÅrMåned)
        logger.info("Følgende fagsaker har flere migreringsbehandlinger og løper i ba-sak: $åpneFagsakerMedFlereMigreringsbehandlinger")
        return ResponseEntity.ok(åpneFagsakerMedFlereMigreringsbehandlinger)
    }

    @GetMapping(path = ["/behandling/{behandlingId}/begrunnelsetest"])
    fun hentBegrunnelsetestPåBehandling(
        @PathVariable behandlingId: Long,
    ): String {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.ACCESS)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "hente data til test",
        )

        return testVerktøyService.hentBegrunnelsetest(behandlingId)
            .replace("\n", System.lineSeparator())
    }

    @GetMapping(path = ["/behandling/{behandlingId}/vedtaksperiodertest"])
    fun hentVedtaksperioderTestPåBehandling(
        @PathVariable behandlingId: Long,
    ): String {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.ACCESS)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "hente data til test",
        )

        return testVerktøyService.hentVedtaksperioderTest(behandlingId)
            .replace("\n", System.lineSeparator())
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
        opprettTaskService.opprettTaskForÅPatcheMergetIdent(patchMergetIdentDto)
        return ResponseEntity.ok("ok")
    }

    @PostMapping("/behandling/{behandlingId}/manuell-kvittering")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Manuell kvittering ble opprettet OK"),
            ApiResponse(responseCode = "409", description = "Oppdrag er allerede kvittert ut."),
        ],
    )
    @Operation(
        summary = "Opprett manuell kvittering på oppdrag tilhørende behandling",
        description =
            "Dette endepunktet oppretter kvitteringsmelding på oppdrag og setter status til KVITTERT_OK. " +
                "Endepunktet skal bare taas i bruk når vi ikke har mottatt kvittering på et oppdrag som økonomi bekrefter har gått gjennom. ",
    )
    fun opprettManuellKvitteringPåOppdrag(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<String> {
        økonomiService.opprettManuellKvitteringPåOppdrag(behandlingId = behandlingId)

        return ResponseEntity.ok("ok")
    }

    @PostMapping("/opprett-grensesnittavstemmingtask/sisteTaskId/{taskId}")
    @Operation(
        summary = "Opprett neste grensesnittavstemming basert på taskId av type avstemMotOppdrag ",
        description =
            "Dette endepunktet oppretter en ny task for å trigge grensesnittavstemming. Man må sende " +
                "taskId fra sist avstemMotOppdrag-tasl som har kjørt ok mot oppdrag. Dette endepunktet kan brukes for å opprette neste " +
                "task hvis man må avvikshåndtere en avstemMotOppdrag task",
    )
    fun opprettGrensesnittavstemmingtask(
        @PathVariable taskId: Long,
    ): ResponseEntity<String> {
        val task = taskService.findById(taskId)
        if (task.type != GrensesnittavstemMotOppdrag.TASK_STEP_TYPE) error("sisteTaskId må være av typen ${GrensesnittavstemMotOppdrag.TASK_STEP_TYPE}")
        opprettTaskService.opprettGrensesnittavstemMotOppdragTask(GrensesnittavstemMotOppdrag.nesteAvstemmingDTO(task.triggerTid.toLocalDate()))
        return ResponseEntity.ok("Ok")
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
        behandlinger.forEach {
            opprettTaskService.opprettTaskForÅPatcheVilkårFom(PatchFomPåVilkårTilFødselsdato(it))
        }
        return ResponseEntity.ok("Ok")
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
        val satskjøringerSomSkalRekjøres = satskjøringRepository.finnPåFeilTypeOgFerdigTidIkkeNull(feiltype, satstid)
        satskjøringRepository.deleteAll(satskjøringerSomSkalRekjøres)
        return ResponseEntity.ok("Ok")
    }

    @GetMapping(path = ["/kjor-intern-konsistensavstemming/{maksAntallTasker}"])
    fun kjørInternKonsistensavstemming(
        @PathVariable maksAntallTasker: Int = Int.MAX_VALUE,
    ): ResponseEntity<Ressurs<String>> {
        taskService.save(OpprettInternKonsistensavstemmingTaskerTask.opprettTask(maksAntallTasker))
        return ResponseEntity.ok(Ressurs.success("Kjørt ok"))
    }

    @PostMapping("/valutajustering/{fagsakId}/juster-valuta")
    @Operation(summary = "Start valutajustering på fagsak for gjeldende måned")
    fun justerValuta(
        @PathVariable fagsakId: Long,
    ): ResponseEntity<Ressurs<String>> {
        if (!envService.erProd()) {
            autovedtakMånedligValutajusteringService.utførMånedligValutajusteringPåFagsak(fagsakId = fagsakId, måned = YearMonth.now())
        } else {
            throw Feil("Kan ikke kjøre valutajustering fra forvaltercontroller i prod")
        }
        return ResponseEntity.ok(Ressurs.success("Kjørt ok"))
    }

    @PostMapping("/start-valutajustering-scheduler")
    @Operation(summary = "Start alle valutajusteringer for gjeldende måned")
    fun lagMånedligValuttajusteringTask(
        @PathVariable fagsakId: Long,
    ): ResponseEntity<Ressurs<String>> {
        if (!envService.erProd()) {
            månedligValutajusteringScheduler.lagMånedligValuttajusteringTask()
        } else {
            throw Feil("Kan ikke kjøre valutajustering fra forvaltercontroller i prod")
        }
        return ResponseEntity.ok(Ressurs.success("Kjørt ok"))
    }
}
