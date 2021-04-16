package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.task.dto.FerdigstillBehandlingDTO
import no.nav.familie.ba.sak.task.dto.IverksettMotFamilieTilbakeDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
@TaskStepBeskrivelse(taskStepType = IverksettMotFamilieTilbakeTask.TASK_STEP_TYPE,
                     beskrivelse = "Iverksett mot Familie tilbake",
                     maxAntallFeil = 3)
class IverksettMotFamilieTilbakeTask(
        val behandlingService: BehandlingService,
        val stegService: StegService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val iverksettMotFamilieTilbake = objectMapper.readValue(task.payload, IverksettMotFamilieTilbakeDTO::class.java)
        stegService.håndterIverksettMotFamilieTilbake(behandling = behandlingService.hent(iverksettMotFamilieTilbake.behandlingsId))
    }

    companion object {
        const val TASK_STEP_TYPE = "iverksettMotFamilieTilbake"
        fun opprettTask(personIdent: String, behandlingsId: Long): Task {
            return Task.nyTask(type = TASK_STEP_TYPE,
                               payload = objectMapper.writeValueAsString(IverksettMotFamilieTilbakeDTO(
                                       personIdent = personIdent,
                                       behandlingsId = behandlingsId
                               )),
                               properties = Properties().apply {
                                   this["personIdent"] = personIdent
                                   this["behandlingsId"] = behandlingsId.toString()
                               }
            )
        }
    }
}