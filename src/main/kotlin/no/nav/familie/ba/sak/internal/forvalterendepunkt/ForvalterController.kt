package no.nav.familie.ba.sak.internal.forvalterendepunkt

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.økonomi.UtbetalingsTidslinjeService
import no.nav.familie.ba.sak.integrasjoner.økonomi.UtbetalingsperiodeDto
import no.nav.familie.ba.sak.internal.forvalter.ForvalterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatusScheduler
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import no.nav.familie.ba.sak.statistikk.stønadsstatistikk.StønadsstatistikkService
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.ba.sak.task.HentAlleIdenterTilPsysTask
import no.nav.familie.ba.sak.task.MaskineltUnderkjennVedtakTask
import no.nav.familie.ba.sak.task.OppdaterLøpendeFlagg
import no.nav.familie.ba.sak.task.dto.FerdigstillBehandlingDTO
import no.nav.familie.ba.sak.task.dto.HenleggAutovedtakOgSettBehandlingTilbakeTilVentVedSmåbarnstilleggTask
import no.nav.familie.ba.sak.task.internkonsistensavstemming.OpprettInternKonsistensavstemmingTaskerTask
import no.nav.familie.eksterne.kontrakter.UtbetalingsperiodeDVHV2
import no.nav.familie.kontrakter.ba.finnmarkstillegg.kommuneErIFinnmarkEllerNordTroms
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/forvalter")
class ForvalterController(
    private val integrasjonKlient: IntegrasjonKlient,
    private val forvalterService: ForvalterService,
    private val tilgangService: TilgangService,
    private val taskService: TaskService,
    private val fagsakStatusScheduler: FagsakStatusScheduler,
    private val fagsakService: FagsakService,
    private val taskRepository: TaskRepositoryWrapper,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val behandlingService: BehandlingService,
    private val stønadsstatistikkService: StønadsstatistikkService,
    private val persongrunnlagService: PersongrunnlagService,
    private val hentAlleIdenterTilPsysTask: HentAlleIdenterTilPsysTask,
    private val utbetalingsTidslinjeService: UtbetalingsTidslinjeService,
    private val saksstatistikkEventPublisher: SaksstatistikkEventPublisher,
) {
    private val logger: Logger = LoggerFactory.getLogger(ForvalterController::class.java)

    @GetMapping(path = ["/kjor-intern-konsistensavstemming/{maksAntallTasker}"])
    fun kjørInternKonsistensavstemming(
        @PathVariable maksAntallTasker: Int = Int.MAX_VALUE,
    ): ResponseEntity<Ressurs<String>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Kjør intern konsistensavstemming",
        )

        taskService.save(OpprettInternKonsistensavstemmingTaskerTask.opprettTask(maksAntallTasker))
        return ResponseEntity.ok(Ressurs.success("Intern konsistensavstemming startet"))
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
        return ResponseEntity.ok(Ressurs.success("Task for oppdatering av løpende flagg startet"))
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
        return ResponseEntity.ok(Ressurs.success("Task for henleggelse av autovedtak startet"))
    }

    @GetMapping("/stonadstatistikk-utbetalingsperioder/{behandlingId}")
    fun hentStønadstatistikkUtbetalingsperioder(
        @PathVariable behandlingId: Long,
    ): ResponseEntity<List<UtbetalingsperiodeDVHV2>> {
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.ACCESS)
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Hente utbetalingsperioder til datavarehus for behandling",
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
            handling = "Hente alle identer som sendes til pensjon",
        )

        return ResponseEntity.ok(hentAlleIdenterTilPsysTask.hentAlleIdenterMedBarnetrygd(aar.toInt(), UUID.randomUUID()))
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
                    val organisasjon = integrasjonKlient.hentOrganisasjon(orgNummer)
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

    @PostMapping("/send-behandlingsstatistikk-til-dvh")
    fun sendBehandlingsstatistikkTilDvh(
        @RequestBody behandlingId: Long,
    ): ResponseEntity<String> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Sende behandlingsstatistikk for behandling til Datavarehus",
        )
        saksstatistikkEventPublisher.publiserBehandlingsstatistikk(behandlingId)
        return ResponseEntity.ok("Sendt behandlingsstatistikk for behandling $behandlingId til Datavarehus")
    }

    @PatchMapping("/fagsak/{fagsakId}/endre-status-til-opprettet")
    @Operation(
        summary = " Endrer fagsakstatus fra løpende til opprettet om det kun finnes henlagte behandlinger.",
        description = "En fagsak som har status løpende uten vedtatte behandlinger feiler opprettelse av revurdering. I en fødselshendelse med tvillinger hvor første task gir henlagt behandling, og andre task skal vedta behandling, må man endre status på fagsak til opprettet for å kunne kjøre automatisk behandling.",
    )
    fun endreStatusPåFagsak(
        @PathVariable fagsakId: Long,
    ): ResponseEntity<String> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Endre status på fagsak",
        )

        forvalterService.endreFagsakStatusFraLøpendeTilOpprettet(fagsakId)
        return ResponseEntity.ok("Endret status på fagsak $fagsakId fra løpende til opprettet.")
    }

    @PostMapping("/opprett-ferdigstill-behandling-task")
    @Operation(
        summary = "Oppretter en ferdigstill behandling task for en behandling",
        description = """
            Kan brukes hvis distribuer dokument har blitt avvikshåndtert og behandlingen ikke har blitt ferdigstilt.

            Hvis man ikke fikk distribuert brevet gjennom dokdist og saksbehandler manuelt har sendt printet og sendt, så vil 
            aldri behandlingen gå viderere til ferdigstill behandling-steget. I de tilfellene kan man bruke dette endepunktet
        """,
    )
    fun opprettFerdigstillBehandlingTask(
        @RequestBody dto: FerdigstillBehandlingDTO,
    ): ResponseEntity<String> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Opprett task for å ferdigstille behandling for ident",
        )

        val behandling = behandlingHentOgPersisterService.hent(dto.behandlingsId)
        if (behandling.status != BehandlingStatus.IVERKSETTER_VEDTAK) {
            return ResponseEntity.badRequest().body("Kan bare opprette ferdigstill behandling task for behandlinger som er i status IVERKSETTER_VEDTAK")
        }

        if (behandling.steg != StegType.DISTRIBUER_VEDTAKSBREV) {
            return ResponseEntity.badRequest().body("Kan bare opprette ferdigstill behandling task for behandlinger som er i steg DISTRIBUER_VEDTAKSBREV")
        }

        behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(
            behandlingId = dto.behandlingsId,
            steg = StegType.FERDIGSTILLE_BEHANDLING,
        )

        val task =
            FerdigstillBehandlingTask.opprettTask(
                søkerIdent = dto.personIdent,
                behandlingsId = dto.behandlingsId,
            )
        taskRepository.save(task)

        logger.info("Opprettet ferdigstill behandling task for behandling ${dto.behandlingsId} gjennom forvalter-endepunktet")

        return ResponseEntity.ok("Task opprettet ${task.id}")
    }

    @PatchMapping("/deaktiverHenlagtBehandlingOgSettSisteVedtatteBehandlingTilAktiv")
    @Operation(
        summary = "Deaktiverer en behandling og setter siste vedtatte behandling til aktiv",
        description = "Dette endepunktet deaktiverer en behandling og setter siste vedtatte behandling til aktiv. Dette for å fikse en sak som ble feil etter en bug i henlegging",
    )
    @Transactional
    fun deaktiverHenlagtBehandlingOgSettSisteVedtatteBehandlingTilAktiv(
        @RequestBody behandlingId: BehandlingId,
    ) {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Deaktiverer en behandling og setter siste vedtatte behandling til aktiv",
        )

        val behandling = behandlingHentOgPersisterService.hent(behandlingId.id)
        val aktivBehandling = behandlingHentOgPersisterService.finnAktivForFagsak(behandling.fagsak.id) ?: error("Finnes ingen aktiv behandling")
        if (aktivBehandling.id != behandling.id) {
            error("Aktiv behandling er ${aktivBehandling.id} og ikke ${behandling.id}")
        }

        if (!aktivBehandling.erHenlagt()) {
            error("Aktiv behandling er ikke henlagt")
        }

        aktivBehandling.aktiv = false
        behandlingHentOgPersisterService.lagreEllerOppdater(aktivBehandling, sendTilDvh = false)
        behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id)?.apply {
            aktiv = true
            behandlingHentOgPersisterService.lagreEllerOppdater(this, sendTilDvh = false)
            secureLogger.info("Patchet ${behandling.fagsak.id} ved å deaktivere behandling ${behandling.id} og setter siste vedtatte behandling ($id) til aktiv")
        }
    }

    @PostMapping("/fagsaklåsing/start-batch")
    @Operation(summary = "Start batch for å låse fagsaker iht. arkivloven. maksAntall begrenser hvor mange fagsaker som låses i denne kjøringen.")
    fun startFagsakLåsingBatch(
        @RequestParam maksAntall: Int,
    ): ResponseEntity<Ressurs<String>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Start fagsaklåsing-batch",
        )
        val startet = fagsakStatusScheduler.startFagsakLåsing(maksAntall = maksAntall)
        val melding = if (startet) "Fagsaklåsing-batch startet med maks $maksAntall fagsaker" else "Fagsaklåsing-batch ble ikke startet: toggle er av"
        return ResponseEntity.ok(Ressurs.success(melding))
    }

    @PostMapping("/fagsaklåsing/lås-fagsak/{fagsakId}")
    @Operation(summary = "Lås én fagsak iht. arkivloven")
    fun låsFagsak(
        @PathVariable fagsakId: Long,
    ): ResponseEntity<Ressurs<String>> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Lås fagsak",
        )
        fagsakService.låsFagsak(fagsakId)
        return ResponseEntity.ok(Ressurs.success("Fagsak $fagsakId er låst"))
    }
}
