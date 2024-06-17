package no.nav.familie.ba.sak.task

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.statistikk.stønadsstatistikk.StønadsstatistikkService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import kotlin.system.measureTimeMillis

@Service
@TaskStepBeskrivelse(taskStepType = FinnBehandlingerMedVedtakEtterDatoTask.TASK_STEP_TYPE, beskrivelse = "Finner behandlinger med vedtak etter dato", maxAntallFeil = 1)
class FinnBehandlingerMedVedtakEtterDatoTask(
    val stønadsstatistikkService: StønadsstatistikkService,
    val taskRepository: TaskRepositoryWrapper,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val dato = objectMapper.readValue<LocalDateTime>(task.payload)
        val tidBrukt =
            measureTimeMillis {
                stønadsstatistikkService.finnVedtakEtterVedtaksdatoOgOpprettTasker(dato)
            }
        logger.info(
            "Fullført kjøring av FinnBehandlingerMedVedtakEtterDatoTask for dato $dato. " +
                "Tid brukt = $tidBrukt millisekunder",
        )
    }

    companion object {
        const val TASK_STEP_TYPE = "finnBehandlingerMedVedtakEtterDatoTask"
        val logger = LoggerFactory.getLogger(FinnBehandlingerMedVedtakEtterDatoTask::class.java)

        fun opprettTask(
            dato: LocalDateTime,
        ): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(dato),
            )
    }
}
