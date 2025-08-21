package no.nav.familie.ba.sak.internal

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.config.FeatureToggle
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.ekstern.restDomene.RestMinimalFagsak
import no.nav.familie.ba.sak.integrasjoner.ecb.ECBService
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.oppgave.domene.OppgaveRepository
import no.nav.familie.ba.sak.integrasjoner.økonomi.UtbetalingsTidslinjeService
import no.nav.familie.ba.sak.integrasjoner.økonomi.UtbetalingsperiodeDto
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiService
import no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering.AutovedtakMånedligValutajusteringService
import no.nav.familie.ba.sak.kjerne.autovedtak.månedligvalutajustering.MånedligValutajusteringScheduler
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.kjerne.autovedtak.svalbardstillegg.FinnPersonerSomBorIFinnmarkNordTromsEllerPåSvalbardTask
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentRepository
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.statistikk.stønadsstatistikk.StønadsstatistikkService
import no.nav.familie.ba.sak.task.DeaktiverMinsideTask
import no.nav.familie.ba.sak.task.GrensesnittavstemMotOppdrag
import no.nav.familie.ba.sak.task.HentAlleIdenterTilPsysTask
import no.nav.familie.ba.sak.task.LogFagsakIdForJournalpostTask
import no.nav.familie.ba.sak.task.LogJournalpostIdForFagsakTask
import no.nav.familie.ba.sak.task.MaskineltUnderkjennVedtakTask
import no.nav.familie.ba.sak.task.OppdaterLøpendeFlagg
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.ba.sak.task.PatchFomPåVilkårTilFødselsdato
import no.nav.familie.ba.sak.task.PatchMergetIdentDto
import no.nav.familie.ba.sak.task.PorteføljejusteringTask
import no.nav.familie.ba.sak.task.SlettKompetanserTask
import no.nav.familie.ba.sak.task.dto.HenleggAutovedtakOgSettBehandlingTilbakeTilVentVedSmåbarnstilleggTask
import no.nav.familie.ba.sak.task.internkonsistensavstemming.OpprettInternKonsistensavstemmingTaskerTask
import no.nav.familie.eksterne.kontrakter.UtbetalingsperiodeDVHV2
import no.nav.familie.kontrakter.ba.finnmarkstillegg.kommuneErIFinnmarkEllerNordTroms
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import kotlin.concurrent.thread
import kotlin.time.measureTimedValue

@RestController
@RequestMapping("/api/forvalter")
@ProtectedWithClaims(issuer = "azuread")
class ForvalterController(
    private val oppgaveRepository: OppgaveRepository,
    private val integrasjonClient: IntegrasjonClient,
    private val forvalterService: ForvalterService,
    private val ecbService: ECBService,
    private val testVerktøyService: TestVerktøyService,
    private val tilgangService: TilgangService,
    private val økonomiService: ØkonomiService,
    private val opprettTaskService: OpprettTaskService,
    private val taskService: TaskService,
    private val satskjøringRepository: SatskjøringRepository,
    private val autovedtakMånedligValutajusteringService: AutovedtakMånedligValutajusteringService,
    private val månedligValutajusteringScheduler: MånedligValutajusteringScheduler,
    private val fagsakService: FagsakService,
    private val unleashNextMedContextService: UnleashNextMedContextService,
    private val taskRepository: TaskRepositoryWrapper,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val stønadsstatistikkService: StønadsstatistikkService,
    private val persongrunnlagService: PersongrunnlagService,
    private val hentAlleIdenterTilPsysTask: HentAlleIdenterTilPsysTask,
    private val utbetalingsTidslinjeService: UtbetalingsTidslinjeService,
    private val personidentRepository: PersonidentRepository,
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
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Ferdigstill liste med oppgaver",
        )

        var antallFeil = 0
        oppgaveListe.forEach { oppgaveId ->
            Result
                .runCatching {
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

    private fun ferdigstillOppgave(oppgaveId: Long) {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Ferdigstill oppgave",
        )

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
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Lag og send utbetalingsoppdrag til økonomi",
        )

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

    @PostMapping("/identifiser-utbetalinger-over-100-prosent")
    fun identifiserUtbetalingerOver100Prosent(): ResponseEntity<Pair<String, String>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Identifiser utbetalinger over 100 prosent",
        )

        val callId = UUID.randomUUID().toString()
        thread {
            forvalterService.identifiserUtbetalingerOver100Prosent(callId)
        }
        return ResponseEntity.ok(Pair("callId", callId))
    }

    @GetMapping("/hentValutakurs/")
    fun hentValutakursFraEcb(
        @RequestParam valuta: String,
        @RequestParam dato: LocalDate,
    ): ResponseEntity<BigDecimal> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Hent valutakurs fra ECB",
        )

        if (!valuta.matches(Regex("[A-Z]{3}"))) {
            throw Feil("Valutakode må ha store bokstaver og være tre bokstaver lang")
        }
        return ResponseEntity.ok(ecbService.hentValutakurs(valuta, dato))
    }

    @GetMapping(path = ["/behandling/{behandlingId}/begrunnelsetest"])
    fun hentBegrunnelsetestPåBehandling(
        @PathVariable behandlingId: Long,
    ): String {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.ACCESS)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "hente data til test",
        )

        return testVerktøyService
            .hentBegrunnelsetest(behandlingId)
            .replace("\n", System.lineSeparator())
    }

    @GetMapping(path = ["/behandling/{behandlingId}/vedtaksperiodertest"])
    fun hentVedtaksperioderTestPåBehandling(
        @PathVariable behandlingId: Long,
    ): String {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.ACCESS)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "hente data til test",
        )

        return testVerktøyService
            .hentVedtaksperioderTest(behandlingId)
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
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Patch merget ident",
        )

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
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Opprett manuell kvittering på oppdrag tilhørende behandling",
        )

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
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Opprett neste grensesnittavstemming basert på taskId av type avstemMotOppdrag ",
        )

        val task = taskService.findById(taskId)
        if (task.type != GrensesnittavstemMotOppdrag.TASK_STEP_TYPE) throw Feil("sisteTaskId må være av typen ${GrensesnittavstemMotOppdrag.TASK_STEP_TYPE}")
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
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Flytt vilkår fom dato på person til fødselsdato",
        )

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
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Rekjør satsendring med feiltype",
        )

        val satskjøringerSomSkalRekjøres = satskjøringRepository.finnPåFeilTypeOgFerdigTidIkkeNull(feiltype, satstid)
        satskjøringRepository.deleteAll(satskjøringerSomSkalRekjøres)
        return ResponseEntity.ok("Ok")
    }

    @GetMapping(path = ["/kjor-intern-konsistensavstemming/{maksAntallTasker}"])
    fun kjørInternKonsistensavstemming(
        @PathVariable maksAntallTasker: Int = Int.MAX_VALUE,
    ): ResponseEntity<Ressurs<String>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Kjør intern konsistensavstemming",
        )

        taskService.save(OpprettInternKonsistensavstemmingTaskerTask.opprettTask(maksAntallTasker))
        return ResponseEntity.ok(Ressurs.success("Kjørt ok"))
    }

    @PostMapping("/valutajustering/{fagsakId}/juster-valuta")
    @Operation(summary = "Start valutajustering på fagsak for gjeldende måned")
    fun justerValuta(
        @PathVariable fagsakId: Long,
    ): ResponseEntity<Ressurs<RestMinimalFagsak>> {
        val erPersonMedTilgangTilÅStarteValutajustering = unleashNextMedContextService.isEnabled(FeatureToggle.KAN_KJØRE_AUTOMATISK_VALUTAJUSTERING_FOR_ENKELT_SAK)

        if (erPersonMedTilgangTilÅStarteValutajustering) {
            autovedtakMånedligValutajusteringService.utførMånedligValutajustering(fagsakId = fagsakId, måned = YearMonth.now())
        } else {
            throw Feil("Du har ikke tilgang til å kjøre valutajustering")
        }

        val fagsak = fagsakService.hentRestMinimalFagsak(fagsakId)
        return ResponseEntity.ok(fagsak)
    }

    @PostMapping("/start-valutajustering-scheduler")
    @Operation(summary = "Start valutajustering for alle sekundærlandsaker i gjeldende måned")
    fun lagMånedligValutajusteringTask(): ResponseEntity<Ressurs<String>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Start valutajustering for alle sekundærlandsaker i gjeldende måned",
        )

        månedligValutajusteringScheduler.lagMånedligValutajusteringTask(triggerTid = LocalDateTime.now())
        return ResponseEntity.ok(Ressurs.success("Kjørt ok"))
    }

    @DeleteMapping("/slett-alle-kompetanser-for-behandling/{behandlingId}")
    @Operation(summary = "Slett kompetanser, utenlandsk periodebeløp og valutakurser for en behandling som er på vilkårsvurderingssteget.")
    fun slettKompetanser(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<String>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Slett kompetanser, utenlandsk periodebeløp og valutakurser for en behandling som er på vilkårsvurderingssteget.",
        )

        val task = taskService.save(SlettKompetanserTask.opprettTask(behandlingId))
        return ResponseEntity.ok(Ressurs.success("Kompetanser slettes i task ${task.id}"))
    }

    @PutMapping("/maskinelt-underkjenn-vedtak/{behandlingId}")
    @Operation(summary = "Underkjenner et vedtak på vegne av system")
    fun maskineltUnderkjennVedtak(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<Ressurs<String>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Underkjenner et vedtak på vegne av system",
        )

        val task = taskService.save(MaskineltUnderkjennVedtakTask.opprettTask(behandlingId))
        return ResponseEntity.ok(Ressurs.success("Underkjenner vedtak i behandling $behandlingId i task ${task.id}"))
    }

    @PostMapping("/kjør-oppdater-løpende-flagg-task")
    @Operation(summary = "Kjører oppdaterLøpendeFlagg-tasken slik at man oppdaterer tasker som er løpende til avsluttet ved behov.")
    fun kjørOppdaterLøpendeFlaggTask(): ResponseEntity<Ressurs<String>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Kjører oppdaterLøpendeFlagg-tasken slik at man oppdaterer tasker som er løpende til avsluttet ved behov.",
        )

        val oppdaterLøpendeFlaggTask = Task(type = OppdaterLøpendeFlagg.TASK_STEP_TYPE, payload = "")
        taskRepository.save(oppdaterLøpendeFlaggTask)
        logger.info("Opprettet oppdaterLøpendeFlaggTask")
        return ResponseEntity.ok(Ressurs.success("Kjørt ok"))
    }

    @PostMapping("/henlegg-autovedtak-og-sett-behandling-tilbake-paa-vent")
    @Operation(summary = "Henlegger autovedtak og setter behandling tilbake på vent.")
    @Transactional
    fun henleggAutovedtakOgSettBehandlingTilbakePåVent(
        @RequestBody behandlingList: List<Long>,
    ): ResponseEntity<Ressurs<String>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Henlegger autovedtak og setter behandling tilbake på vent.",
        )

        behandlingList.forEach { behandlingId ->
            logger.info("Opprettet oppdaterLøpendeFlaggTask for behandlingId=$behandlingId")
            val hennleggAutovedtakTask = HenleggAutovedtakOgSettBehandlingTilbakeTilVentVedSmåbarnstilleggTask.opprettTask(behandlingId)
            taskRepository.save(hennleggAutovedtakTask)
        }
        return ResponseEntity.ok(Ressurs.success("Kjørt ok"))
    }

    @GetMapping("/stonadstatistikk-utbetalingsperioder/{behandlingId}")
    fun hentStønadstatistikkUtbetalingsperioder(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<List<UtbetalingsperiodeDVHV2>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.ACCESS)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "hente data til test",
        )
        val behandling = behandlingHentOgPersisterService.hent(behandlingId = behandlingId)
        val persongrunnlag = persongrunnlagService.hentAktivThrows(behandlingId = behandlingId)
        val utbetalingsperioder = stønadsstatistikkService.hentUtbetalingsperioderTilDatavarehus(behandling = behandling, persongrunnlag = persongrunnlag)

        return ResponseEntity.ok(utbetalingsperioder)
    }

    @GetMapping("/identer-barnetrygd-pensjon/{aar}")
    fun hentAlleIdenterSomSendesTilPensjon(
        @PathVariable("aar") aar: Long,
    ): ResponseEntity<List<String>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "hente data til test",
        )

        return ResponseEntity.ok(hentAlleIdenterTilPsysTask.hentAlleIdenterMedBarnetrygd(aar.toInt(), UUID.randomUUID()))
    }

    @PostMapping("/hent-fagsak-id-for-journalpost")
    @Operation(
        summary = "Henter fagsak id som er koblet til journalposten",
        description = "Oppretter task for å logge fagsak id som er koblet til journalpost. Fagsak id'n logges til securelog.",
    )
    fun hentFagsakIdForJournalpost(
        @RequestParam("journalpostId") journalpostId: String,
    ): ResponseEntity<Long> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "hente data til test",
        )

        val opprettetTask = taskRepository.save(LogFagsakIdForJournalpostTask.opprettTask(journalpostId))

        return ResponseEntity.ok(opprettetTask.id)
    }

    @PostMapping("/hent-journalpost-id-for-fagsak")
    @Operation(
        summary = "Henter journalpost ider koblet til fagsaken",
        description = "Oppretter task for å logge journalpost id som er koblet til en fagsak. Journalpost ider logges til securelog.",
    )
    fun hentJournalpostIdForFagsak(
        @RequestParam("fagsakId") fagsakId: String,
    ): ResponseEntity<Long> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "hente data til test",
        )

        val opprettetTask = taskRepository.save(LogJournalpostIdForFagsakTask.opprettTask(fagsakId))

        return ResponseEntity.ok(opprettetTask.id)
    }

    @GetMapping("/hent-utbetalingstidslinjer-for-fagsak/{fagsakId}")
    fun hentUtbetalingsTidslinjerForFagsak(
        @PathVariable("fagsakId") fagsakId: Long,
    ): ResponseEntity<List<List<UtbetalingsperiodeDto>>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Hente gjeldende utbetalings-tidslinjer for fagsak",
        )

        return ResponseEntity.ok(utbetalingsTidslinjeService.genererUtbetalingstidslinjerForFagsak(fagsakId).map { it.tilUtbetalingsperioder() })
    }

    @PostMapping("/finn-og-patch-andeler-tilkjent-ytelse-i-fagsaker-med-avvik")
    @Operation(
        summary = "Finner og patcher andeler tilkjent ytelse i fagsaker med avvik i konsistensavstemming",
        description =
            "Bruker Utbetalingtidslinjer til å sammenligne andelerTilkjentYtelse med faktiske utbetalingsperioder oversendt til Oppdrag." +
                "Finner vi forskjeller mellom en andel og en utbetalingsperiode slettes den originale andelen og erstattes av en korrigert andel.",
    )
    fun finnOgPatchAndelerTilkjentYtelseIFagsakerMedAvvik(
        @RequestBody finnOgPatchAndelerRequestDto: FinnOgPatchAndelerRequestDto,
    ): ResponseEntity<List<Pair<Long, List<AndelTilkjentYtelseKorreksjonDto>?>>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Finne og patche andeler tilkjent ytelse i fagsaker med avvik i konsistensavstemming",
        )
        if (!unleashNextMedContextService.isEnabled(FeatureToggle.SKAL_FINNE_OG_PATCHE_ANDELER_I_FAGAKER_MED_AVVIK, false)) {
            throw FunksjonellFeil("Kan ikke finne og patche andeler. Toggelen ${FeatureToggle.SKAL_FINNE_OG_PATCHE_ANDELER_I_FAGAKER_MED_AVVIK} er skrudd av")
        }

        return ResponseEntity.ok(
            forvalterService.finnOgPatchAndelerTilkjentYtelseIFagsakerMedAvvik(
                fagsaker = finnOgPatchAndelerRequestDto.fagsaker,
                korrigerAndelerFraOgMedDato = finnOgPatchAndelerRequestDto.korrigerAndelerFraOgMedDato,
                dryRun = finnOgPatchAndelerRequestDto.dryRun,
            ),
        )
    }

    @PostMapping("/opprett-minside-task-for-fagsaker-uten-aktivering")
    @Operation(
        summary = "Oppretter task som aktiverer minside for fagsaker som ikke har fått det aktivert enda",
    )
    fun opprettMinsideAktiveringTaskForFagsakerUtenAktivering(
        @RequestBody opprettMinsideAktiveringTaskDto: OpprettMinsideAktiveringTaskDto,
    ): ResponseEntity<String> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Aktiver",
        )

        forvalterService.finnFagsakSomSkalHaMinsideAktivertOgLagTask(
            dryRun = opprettMinsideAktiveringTaskDto.dryRun,
            antallFagsaker = opprettMinsideAktiveringTaskDto.antallFagsaker,
        )

        return ResponseEntity.ok("Kjørt OK")
    }

    @GetMapping("/start-portefoljejustering-task")
    fun startPorteføljejusteringTask(
        @RequestParam("antallOppgaver") antallOppgaver: Long,
    ): ResponseEntity<Long> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Start porteføljejustering",
        )

        val opprettetTask = taskRepository.save(PorteføljejusteringTask.opprettTask(antallOppgaver = antallOppgaver))

        return ResponseEntity.ok(opprettetTask.id)
    }

    @PostMapping("/opprett-tasker-for-autovedtak-finnmarkstillegg")
    @Operation(
        summary = "Oppretter tasker for autovedtak av Finnmarkstillegg",
    )
    fun opprettTaskerForAutovedtakFinnmarkstillegg(
        @RequestBody fagsakIder: List<Long>,
    ): ResponseEntity<String> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Opprett task for autovedtak av Finnmarkstillegg",
        )

        if (!unleashNextMedContextService.isEnabled(FeatureToggle.KAN_KJØRE_AUTOVEDTAK_FINNMARKSTILLEGG)) {
            throw Feil("Toggle for å opprette tasker for autovedtak av Finnmarkstillegg er skrudd av")
        }

        fagsakIder.forEach {
            opprettTaskService.opprettAutovedtakFinnmarkstilleggTask(it)
        }
        return ResponseEntity.ok("Tasker for autovedtak av Finnmarkstillegg opprettet")
    }

    @PostMapping("/aktiver-minside-for-ident")
    @Operation(
        summary = "Sender Kafka-melding om å aktivere MinSide for en ident",
    )
    fun aktiverMinsideForIdent(
        @RequestBody ident: String,
    ): ResponseEntity<String> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Opprett task for å aktivere minside for ident",
        )

        val personIdent = personidentRepository.findByFødselsnummerOrNull(ident) ?: return ResponseEntity.status(404).body("Finner ikke person")

        opprettTaskService.opprettAktiverMinsideTask(personIdent.aktør)

        return ResponseEntity.ok("Task for aktivering av minside for ident opprettet")
    }

    @PostMapping("/deaktiver-minside-for-ident")
    @Operation(
        summary = "Sender Kafka-melding om å deaktivere MinSide for en ident",
    )
    fun deaktiverMinsideForIdent(
        @RequestBody ident: String,
    ): ResponseEntity<String> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Opprett task for å deaktivere minside for ident",
        )

        val personIdent = personidentRepository.findByFødselsnummerOrNull(ident) ?: return ResponseEntity.status(404).body("Finner ikke person")

        DeaktiverMinsideTask.opprettTask(personIdent.aktør)

        return ResponseEntity.ok("Task for deaktivering av minside for ident opprettet")
    }

    @PostMapping("/opprett-tasker-som-finner-personer-som-bor-i-finnmark-nord-troms-eller-paa-svalbard")
    @Operation(
        summary = "Oppretter tasker som finner personer med bostedsadresse eller delt bosted i Finnmark/Nord-Troms eller oppholdsadresse på Svalbard",
    )
    fun opprettTaskerSomFinnerPersonerMedOppholdsadressePåSvalbard(
        @RequestParam dryRun: Boolean = true,
    ): ResponseEntity<String> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Opprett tasker som finner personer med bostedsadresse eller delt bosted i Finnmark/Nord-Troms eller oppholdsadresse på Svalbard",
        )

        val (antallTasker, tid) =
            measureTimedValue {
                val chunksMedIdenter =
                    fagsakService
                        .finnIdenterForLøpendeFagsaker()
                        .also { logger.info("Hentet ${it.size} identer for løpende fagsaker") }
                        .chunked(10000)

                logger.info("Lagde ${chunksMedIdenter.size} chunks á 10 000 identer")

                chunksMedIdenter
                    .onEachIndexed { index, identer ->
                        val task =
                            FinnPersonerSomBorIFinnmarkNordTromsEllerPåSvalbardTask
                                .opprettTask(identer)
                                .medTriggerTid(LocalDateTime.now().plusSeconds(index * 5L))

                        if (!dryRun) taskService.save(task)

                        if (index % 10 == 0) {
                            logger.info("Opprettet og lagret task $index/${chunksMedIdenter.size}")
                        }
                    }.size
            }

        logger.info("Brukte ${tid.inWholeSeconds} sekunder på å opprette $antallTasker tasker for å finne personer som bor i Finnmark, Nord-Troms eller på Svalbard")

        return ResponseEntity.ok("Brukte ${tid.inWholeSeconds} sekunder på å opprette $antallTasker tasker")
    }

    @PostMapping("/sjekk-om-personer-i-fagsak-har-utbetalinger-som-overstiger-100-prosent")
    fun sjekkOmPersonerIFagsakHarUtbetalingerSomOverstiger100Prosent(
        @RequestBody fagsakIder: List<Long>,
    ): ResponseEntity<String> {
//        tilgangService.verifiserHarTilgangTilHandling(
//            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
//            handling = "Sjekk om fagsak har utbetalinger som overstiger 100 prosent",
//        )

        forvalterService.sjekkChunkMedFagsakerOmDeHarUtbetalingerOver100Prosent(fagsakIder)
        return ResponseEntity.ok("Sjekket om fagsaker har utbetalinger som overstiger 100 prosent")
    }

    @PostMapping("/rekjor-feilede-tasker-med-type-finnPersonerSomBorIFinnmarkNordTromsEllerPaaSvalbardTask")
    fun rekjørFeiledeTaskerForÅFinnePersonerSomBorIFinnmarkNordTromsEllerPåSvalbard(): ResponseEntity<String> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Rekjør feilede task med type finnPersonerSomBorIFinnmarkNordTromsEllerPåSvalbardTask",
        )

        val tasker =
            taskRepository
                .findByStatus(Status.FEILET)
                .filter { it.type == FinnPersonerSomBorIFinnmarkNordTromsEllerPåSvalbardTask.TASK_STEP_TYPE }
                .onEachIndexed { index, task ->
                    taskService.save(
                        task
                            .copy(status = Status.KLAR_TIL_PLUKK)
                            .medTriggerTid(LocalDateTime.now().plusMinutes(index.toLong())),
                    )
                }

        return ResponseEntity.ok("Rekjørte ${tasker.size} feilede tasker med type finnPersonerSomBorIFinnmarkNordTromsEllerPåSvalbardTask")
    }

    @GetMapping("/identifiser-institusjoner-med-finnmarkstillegg")
    fun identifiserInstitusjonerMedFinnmarkstillegg(): ResponseEntity<List<Triple<String, String?, String?>>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Identifiser institusjoner med Finnmarkstillegg",
        )

        val institusjonerSomSkalHaFinnmarkstillegg =
            fagsakService
                .finnOrgnummerForLøpendeFagsaker()
                .mapNotNull { orgNummer ->
                    val organisasjon = integrasjonClient.hentOrganisasjon(orgNummer)
                    val kommunenummer = organisasjon.adresse?.kommunenummer
                    if (kommunenummer == null) {
                        logger.info("Kommunenummer er null for orgnummer ${organisasjon.organisasjonsnummer}")
                        null
                    } else if (kommuneErIFinnmarkEllerNordTroms(kommunenummer)) {
                        Triple(organisasjon.organisasjonsnummer, organisasjon.adresse?.type, kommunenummer)
                    } else {
                        null
                    }
                }

        return ResponseEntity.ok(institusjonerSomSkalHaFinnmarkstillegg)
    }
}

data class FinnOgPatchAndelerRequestDto(
    val fagsaker: Set<Long>,
    val korrigerAndelerFraOgMedDato: LocalDate = LocalDate.of(2025, 2, 1),
    val dryRun: Boolean = true,
)

data class OpprettMinsideAktiveringTaskDto(
    val antallFagsaker: Int,
    val dryRun: Boolean = true,
)
