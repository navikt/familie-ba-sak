package no.nav.familie.ba.sak.internal.forvalterendepunkter

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiService
import no.nav.familie.ba.sak.internal.ForvalterService
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringMottaker
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.task.GrensesnittavstemMotOppdrag
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.ba.sak.task.internkonsistensavstemming.OpprettInternKonsistensavstemmingTaskerTask
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import kotlin.concurrent.thread

@RestController
@RequestMapping("/api/forvalter")
class ForvaltØkonomiController(
    private val tilgangService: TilgangService,
    private val forvalterService: ForvalterService,
    private val økonomiService: ØkonomiService,
    private val opprettTaskService: OpprettTaskService,
    private val taskService: TaskService,
    private val simuleringService: SimuleringService,
) {
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
    @Operation(
        summary = "Identifiser utbetalinger over 100 prosent",
        description =
            "Ofte lurt å kjøre etter satsendring for å finne feil.",
    )
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

    @GetMapping(path = ["/behandling/{behandlingId}/simulering"])
    fun hentSimuleringPåBehandling(
        @PathVariable behandlingId: Long,
    ): List<ØkonomiSimuleringMottaker> {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.FORVALTER,
            handling = "Hent simulering på behandling",
        )
        tilgangService.validerTilgangTilBehandling(behandlingId = behandlingId, event = AuditLoggerEvent.ACCESS)

        return simuleringService.hentSimuleringPåBehandling(behandlingId)
    }
}
