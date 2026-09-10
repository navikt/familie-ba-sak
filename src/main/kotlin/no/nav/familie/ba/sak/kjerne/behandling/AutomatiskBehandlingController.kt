package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.task.BehandleAutomatiskSøknadTask
import no.nav.familie.ba.sak.task.dto.BehandleAutomatiskSøknadTaskDTO
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/behandlinger")
@Validated
class AutomatiskBehandlingController(
    private val tilgangService: TilgangService,
    private val taskRepository: TaskRepositoryWrapper,
) {
    @PostMapping("/automatisk-soknad", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun opprettAutomatiskBehandlingAvSøknad(
        @RequestBody nyBehandling: NyBehandling,
    ) {
        tilgangService.validerTilgangTilFagsak(
            fagsakId = nyBehandling.fagsakId,
            event = AuditLoggerEvent.CREATE,
        )

        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "opprette behandling",
        )

        val task = BehandleAutomatiskSøknadTask.opprettTask(BehandleAutomatiskSøknadTaskDTO(nyBehandling))
        taskRepository.save(task)
    }
}
