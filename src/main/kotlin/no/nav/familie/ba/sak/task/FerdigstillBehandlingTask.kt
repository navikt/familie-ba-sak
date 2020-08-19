package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.task.dto.FerdigstillBehandlingDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
@TaskStepBeskrivelse(taskStepType = FerdigstillBehandlingTask.TASK_STEP_TYPE,
                     beskrivelse = "Ferdigstill behandling",
                     maxAntallFeil = 3)
class FerdigstillBehandlingTask(
        val behandlingService: BehandlingService,
        val stegService: StegService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val ferdigstillBehandling = objectMapper.readValue(task.payload, FerdigstillBehandlingDTO::class.java)
        stegService.håndterFerdigstillBehandling(behandling = behandlingService.hent(ferdigstillBehandling.behandlingsId))
    }

    companion object {
        const val TASK_STEP_TYPE = "ferdigstillBehandling"
        val LOG = LoggerFactory.getLogger(FerdigstillBehandlingTask::class.java)


        fun opprettTask(personIdent: String, behandlingsId: Long): Task {
            return Task.nyTask(type = TASK_STEP_TYPE,
                               payload = objectMapper.writeValueAsString(FerdigstillBehandlingDTO(
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