package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.task.StatusFraOppdrag.Companion.TASK_STEP_TYPE
import no.nav.familie.ba.sak.økonomi.OppdragId
import no.nav.familie.ba.sak.økonomi.OppdragProtokollStatus
import no.nav.familie.ba.sak.økonomi.ØkonomiService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskFeil
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
@TaskStepBeskrivelse(taskStepType = TASK_STEP_TYPE, beskrivelse = "Iverksett vedtak mot oppdrag", maxAntallFeil = 100)
class StatusFraOppdrag(
        private val økonomiService: ØkonomiService,
        private val taskRepository: TaskRepository
) : AsyncTaskStep {

    /**
     * Metoden prøver å hente kvittering i ét døgn.
     * Får tasken kvittering som ikke er OK feiler vi tasken.
     */
    override fun doTask(task: Task) {
        val oppdragId = objectMapper.readValue(task.payload, OppdragId::class.java)
        Result.runCatching { økonomiService.hentStatus(oppdragId) }
                .fold(
                        onFailure = {
                            task.triggerTid = LocalDateTime.now().plusMinutes(15)
                            taskRepository.save(task)
                            throw it
                        },
                        onSuccess = {
                            LOG.debug("Mottok status '$it' fra oppdrag")
                            if (it != OppdragProtokollStatus.KVITTERT_OK) {
                                task.triggerTid = LocalDateTime.now().plusMinutes(15)
                                taskRepository.save(task)

                                LOG.error("Fant kvittering, men den var ikke OK")
                                throw Exception("Fant kvittering, men den var ikke OK")
                            }
                        }
                )
    }

    companion object {
        const val TASK_STEP_TYPE = "kvitteringFraOppdrag"
        val LOG = LoggerFactory.getLogger(IverksettMotOppdrag::class.java)
    }
}