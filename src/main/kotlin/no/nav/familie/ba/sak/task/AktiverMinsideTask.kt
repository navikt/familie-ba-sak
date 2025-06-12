package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.kjerne.minside.MinsideAktiveringKafkaProducer
import no.nav.familie.ba.sak.kjerne.minside.MinsideAktiveringService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.task.dto.AktiverMinsideDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = AktiverMinsideTask.TASK_STEP_TYPE,
    beskrivelse = "Aktiverer minside for ident",
    maxAntallFeil = 3,
)
class AktiverMinsideTask(
    private val minsideAktiveringKafkaProducer: MinsideAktiveringKafkaProducer,
    private val minsideAktiveringService: MinsideAktiveringService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val aktiverMinsideDTO =
            objectMapper.readValue(task.payload, AktiverMinsideDTO::class.java)

        if (minsideAktiveringService.harAktivertMinsideAktivering(aktiverMinsideDTO.aktør)) {
            logger.info("Minside er allerede aktivert for aktør: ${aktiverMinsideDTO.aktør.aktørId}")
            return
        }
        logger.info("Aktiverer minside for aktør: ${aktiverMinsideDTO.aktør.aktørId}")
        minsideAktiveringService.aktiverMinsideAktivering(aktiverMinsideDTO.aktør)
        minsideAktiveringKafkaProducer.aktiver(aktiverMinsideDTO.aktør.aktivFødselsnummer())
    }

    companion object {
        const val TASK_STEP_TYPE = "aktiverMinside"
        private val logger = LoggerFactory.getLogger(AktiverMinsideTask::class.java)

        fun opprettTask(aktør: Aktør): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(AktiverMinsideDTO(aktør)),
            )
    }
}
