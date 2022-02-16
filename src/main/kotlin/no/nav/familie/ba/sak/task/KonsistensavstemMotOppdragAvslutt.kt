package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.integrasjoner.økonomi.AvstemmingService
import no.nav.familie.ba.sak.integrasjoner.økonomi.DataChunkRepository
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingAvsluttTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.error.RekjørSenereException
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
@TaskStepBeskrivelse(
    taskStepType = KonsistensavstemMotOppdrag.TASK_STEP_TYPE,
    beskrivelse = "Konsistensavstemming mot oppdrag",
    maxAntallFeil = 3
)
class KonsistensavstemMotOppdragAvslutt(
    val avstemmingService: AvstemmingService,
    val dataChunkRepository: DataChunkRepository,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val konsistensavstemmingAvsluttTask =
            objectMapper.readValue(task.payload, KonsistensavstemmingAvsluttTaskDTO::class.java)

        val dataChunks = dataChunkRepository.findByTransaksjonsId(konsistensavstemmingAvsluttTask.transaksjonsId)
        if (dataChunks.any { !it.erSendt }) {
            throw RekjørSenereException(
                årsak = "Alle datatasks for konsistensavstemming med id ${konsistensavstemmingAvsluttTask.transaksjonsId} er ikke kjørt.",
                triggerTid = LocalDateTime.now().plusMinutes(15)
            )
        }

        avstemmingService.konsistensavstemOppdragAvslutt(
            avstemmingsdato = konsistensavstemmingAvsluttTask.avstemmingsdato,
            transaksjonsId = konsistensavstemmingAvsluttTask.transaksjonsId
        )
    }

    companion object {
        const val TASK_STEP_TYPE = "konsistensavstemMotOppdragAvslutt"
    }
}
