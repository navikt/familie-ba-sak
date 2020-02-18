package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.økonomi.AvstemmingService
import no.nav.familie.ba.sak.task.dto.GrensesnittavstemmingTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.MonthDay

@Service
@TaskStepBeskrivelse(taskStepType = GrensesnittavstemMotOppdrag.TASK_STEP_TYPE,
                     beskrivelse = "Grensesnittavstemming mot oppdrag",
                     maxAntallFeil = 3)
class GrensesnittavstemMotOppdrag(val avstemmingService: AvstemmingService, val taskRepository: TaskRepository) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val avstemmingTask = objectMapper.readValue(task.payload, GrensesnittavstemmingTaskDTO::class.java)
        LOG.info("Gjør avstemming mot oppdrag fra og med ${avstemmingTask.fomDato} til og med ${avstemmingTask.tomDato}")

        avstemmingService.grensesnittavstemOppdrag(avstemmingTask.fomDato, avstemmingTask.tomDato)
    }

    override fun onCompletion(task: Task) {
        val nesteAvstemmingTaskDTO = nesteAvstemmingDTO(task.triggerTid!!.toLocalDate().plusDays(1), 1)

        val nesteAvstemmingTask = Task.nyTaskMedTriggerTid(
                TASK_STEP_TYPE,
                objectMapper.writeValueAsString(nesteAvstemmingTaskDTO),
                nesteAvstemmingTaskDTO.tomDato.toLocalDate().atTime(8, 0)
        )

        taskRepository.save(nesteAvstemmingTask)
    }

    fun nesteAvstemmingDTO(nesteDag: LocalDate, antallDager: Int): GrensesnittavstemmingTaskDTO {
        return if (erHelgEllerHelligdag(nesteDag)) nesteAvstemmingDTO(nesteDag.plusDays(1), antallDager + 1)
        else GrensesnittavstemmingTaskDTO(nesteDag.minusDays(antallDager.toLong()).atStartOfDay(),
                                                                                            nesteDag.atStartOfDay())
    }

    private fun erHelgEllerHelligdag(dato: LocalDate): Boolean {
        return dato.dayOfWeek == DayOfWeek.SATURDAY
               || dato.dayOfWeek == DayOfWeek.SUNDAY
               || FASTE_HELLIGDAGER.contains(MonthDay.from(dato))
    }

    companion object {
        const val TASK_STEP_TYPE = "avstemMotOppdrag"
        val FASTE_HELLIGDAGER = setOf(
                MonthDay.of(1, 1),
                MonthDay.of(5, 1),
                MonthDay.of(5, 17),
                MonthDay.of(12, 25),
                MonthDay.of(12, 26)
        )
        val LOG: Logger = LoggerFactory.getLogger(GrensesnittavstemMotOppdrag::class.java)
    }
}
