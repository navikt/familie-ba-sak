package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.kjerne.minside.MinsideAktiveringKafkaProducer
import no.nav.familie.ba.sak.kjerne.minside.MinsideAktiveringService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
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
    private val minsideAktiveringKafkaProducer: MinsideAktiveringKafkaProducer,
    private val minsideAktiveringService: MinsideAktiveringService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val deaktiverMinsideDTO =
            objectMapper.readValue(task.payload, DeaktiverMinsideDTO::class.java)
        if (!minsideAktiveringService.harAktivertMinsideAktivering(deaktiverMinsideDTO.aktør)) {
            logger.info("Minside er ikke aktivert for aktør: ${deaktiverMinsideDTO.aktør.aktørId}")
            return
        }

        logger.info("Deaktiverer minside for aktør: ${deaktiverMinsideDTO.aktør.aktørId}")
        minsideAktiveringService.deaktiverMinsideAktivering(deaktiverMinsideDTO.aktør)
        minsideAktiveringKafkaProducer.deaktiver(deaktiverMinsideDTO.aktør.aktivFødselsnummer())
    }

    companion object {
        const val TASK_STEP_TYPE = "deaktiverMinside"
        private val logger = LoggerFactory.getLogger(DeaktiverMinsideTask::class.java)

        fun opprettTask(aktør: Aktør): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(DeaktiverMinsideDTO(aktør)),
            )
    }
}
