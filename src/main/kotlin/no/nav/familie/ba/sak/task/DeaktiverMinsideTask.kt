package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.kjerne.minside.MinsideAktiveringService
import no.nav.familie.ba.sak.task.dto.DeaktiverMinsideDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = DeaktiverMinsideTask.TASK_STEP_TYPE,
    beskrivelse = "Aktiverer minside for ident",
    maxAntallFeil = 3,
)
class DeaktiverMinsideTask(
    private val minsideAktiveringService: MinsideAktiveringService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val deaktiverMinsideDTO =
            objectMapper.readValue(task.payload, DeaktiverMinsideDTO::class.java)

        // TODO: Vurdere om vi skal lagre informasjon om ident har aktivert minside i databasen og kun deaktivere hvis vi tidligere har aktivert for identen
        logger.info("Deaktiverer minside for ident: ${deaktiverMinsideDTO.ident}")
        minsideAktiveringService.deaktiver(deaktiverMinsideDTO.ident)
    }

    companion object {
        const val TASK_STEP_TYPE = "deaktiverMinside"
        private val logger = LoggerFactory.getLogger(DeaktiverMinsideTask::class.java)

        fun opprettTask(ident: String): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(DeaktiverMinsideDTO(ident)),
            )
    }
}
