package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.behandling.fødselshendelse.FødselshendelseService
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.gdpr.domene.FødelshendelsePreLanseringRepository
import no.nav.familie.ba.sak.gdpr.domene.FødselshendelsePreLansering
import no.nav.familie.ba.sak.task.dto.BehandleFødselshendelseTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
@TaskStepBeskrivelse(taskStepType = BehandleFødselshendelseTask.TASK_STEP_TYPE,
                     beskrivelse = "Setter i gang behandlingsløp for fødselshendelse",
                     maxAntallFeil = 3)
class BehandleFødselshendelseTask(
        private val fødselshendelseService: FødselshendelseService,
        private val fødselshendelsePreLanseringRepository: FødelshendelsePreLanseringRepository) :
        AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandleFødselshendelseTaskDTO = objectMapper.readValue(task.payload, BehandleFødselshendelseTaskDTO::class.java)
        LOG.info("Kjører BehandleFødselshendelseTask")

        try {
            fødselshendelseService.opprettBehandlingOgKjørReglerForFødselshendelse(behandleFødselshendelseTaskDTO.nyBehandling)
        } catch (e: KontrollertRollbackException) {
            when (e.fødselshendelsePreLansering) {
                null -> LOG.error("Rollback har blitt trigget, men data fra fødselshendelse mangler")
                else -> fødselshendelsePreLanseringRepository.save(e.fødselshendelsePreLansering.copy(id = 0))
            }

            LOG.info("Rollback utført. Data ikke persistert.")
        } catch (e: Throwable) {
            LOG.info("FødselshendelseTask kjørte med Feil=${e.message}")

            if (e is Feil) {
                secureLogger.info("FødselshendelseTask kjørte med Feil=${e.frontendFeilmelding}", e)
            } else {
                secureLogger.info("FødselshendelseTask feilet!", e)
            }
        }
    }

    companion object {

        const val TASK_STEP_TYPE = "behandleFødselshendelseTask"
        val LOG = LoggerFactory.getLogger(this::class.java)
        val secureLogger = LoggerFactory.getLogger("secureLogger")

        fun opprettTask(behandleFødselshendelseTaskDTO: BehandleFødselshendelseTaskDTO): Task {
            return Task.nyTask(
                    type = TASK_STEP_TYPE,
                    payload = objectMapper.writeValueAsString(behandleFødselshendelseTaskDTO),
                    properties = Properties().apply {
                        this["morsIdent"] = behandleFødselshendelseTaskDTO.nyBehandling.morsIdent
                    }
            )
        }
    }
}

data class KontrollertRollbackException(val fødselshendelsePreLansering: FødselshendelsePreLansering?) : RuntimeException()