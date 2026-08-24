package no.nav.familie.ba.sak.internal.forvalterendepunkt

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.økonomi.UtbetalingsTidslinjeService
import no.nav.familie.ba.sak.integrasjoner.økonomi.UtbetalingsperiodeDto
import no.nav.familie.ba.sak.internal.forvalter.ForvalterService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.task.HentAlleIdenterTilPsysTask
import no.nav.familie.ba.sak.task.OppdaterLøpendeFlagg
import no.nav.familie.ba.sak.task.internkonsistensavstemming.OpprettInternKonsistensavstemmingTaskerTask
import no.nav.familie.kontrakter.ba.finnmarkstillegg.kommuneErIFinnmarkEllerNordTroms
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/forvalter")
class ForvalterController(
    private val integrasjonKlient: IntegrasjonKlient,
    private val forvalterService: ForvalterService,
    private val tilgangService: TilgangService,
    private val taskService: TaskService,
    private val fagsakService: FagsakService,
    private val taskRepository: TaskRepositoryWrapper,
    private val hentAlleIdenterTilPsysTask: HentAlleIdenterTilPsysTask,
    private val utbetalingsTidslinjeService: UtbetalingsTidslinjeService,
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
}
