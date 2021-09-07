package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.task.dto.Autobrev6og18ÅrDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

@Service
class TaskService(
        val taskRepository: TaskRepository
) {

    fun opprettOppgaveTask(behandlingId: Long,
                           oppgavetype: Oppgavetype,
                           beskrivelse: String? = null) {
        taskRepository.save(OpprettOppgaveTask.opprettTask(
                behandlingId = behandlingId,
                oppgavetype = oppgavetype,
                fristForFerdigstillelse = LocalDate.now(),
                beskrivelse = beskrivelse
        ))
    }

    fun opprettSendFeedTilInfotrygdTask(barnasIdenter: List<String>) {
        taskRepository.save(SendFeedTilInfotrygdTask.opprettTask(barnasIdenter))
    }

    fun opprettAutovedtakFor6Og18ÅrBarn(fagsakId: Long, alder: Int) {
        taskRepository.save(Task.nyTask(type = SendAutobrev6og18ÅrTask.TASK_STEP_TYPE,
                                        payload = objectMapper.writeValueAsString(
                                                Autobrev6og18ÅrDTO(fagsakId = fagsakId,
                                                                   alder = alder,
                                                                   årMåned = inneværendeMåned())),
                                        properties = Properties().apply {
                                            this["fagsak"] = fagsakId.toString()
                                            if (!MDC.get(MDCConstants.MDC_CALL_ID).isNullOrEmpty()) {
                                                this["callId"] = MDC.get(MDCConstants.MDC_CALL_ID) ?: IdUtils.generateId()
                                            }
                                        }
        ))
    }

    companion object {
        const val RETRY_BACKOFF_5000MS = "\${retry.backoff.delay:5000}"
    }
}