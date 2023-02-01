package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.task.dto.Autobrev6og18ÅrDTO
import no.nav.familie.ba.sak.task.dto.AutobrevOpphørSmåbarnstilleggDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.domene.Task
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth
import java.util.Properties

@Service
class OpprettTaskService(
    val taskRepository: TaskRepositoryWrapper
) {

    fun opprettOppgaveTask(
        behandlingId: Long,
        oppgavetype: Oppgavetype,
        beskrivelse: String? = null,
        fristForFerdigstillelse: LocalDate = LocalDate.now()
    ) {
        taskRepository.save(
            OpprettOppgaveTask.opprettTask(
                behandlingId = behandlingId,
                oppgavetype = oppgavetype,
                fristForFerdigstillelse = fristForFerdigstillelse,
                beskrivelse = beskrivelse
            )
        )
    }

    fun opprettSendFeedTilInfotrygdTask(barnasIdenter: List<String>) {
        taskRepository.save(SendFødselsmeldingTilInfotrygdTask.opprettTask(barnasIdenter))
    }

    fun opprettSendStartBehandlingTilInfotrygdTask(aktørStoenadsmottaker: Aktør) {
        taskRepository.save(SendStartBehandlingTilInfotrygdTask.opprettTask(aktørStoenadsmottaker))
    }

    fun opprettAutovedtakFor6Og18ÅrBarn(fagsakId: Long, alder: Int) {
        overstyrTaskMedNyCallId(IdUtils.generateId()) {
            taskRepository.save(
                Task(
                    type = SendAutobrev6og18ÅrTask.TASK_STEP_TYPE,
                    payload = objectMapper.writeValueAsString(
                        Autobrev6og18ÅrDTO(
                            fagsakId = fagsakId,
                            alder = alder,
                            årMåned = inneværendeMåned()
                        )
                    ),
                    properties = Properties().apply {
                        this["fagsak"] = fagsakId.toString()
                    }
                )
            )
        }
    }

    fun opprettAutovedtakForOpphørSmåbarnstilleggTask(fagsakId: Long) {
        overstyrTaskMedNyCallId(IdUtils.generateId()) {
            taskRepository.save(
                Task(
                    type = SendAutobrevOpphørSmåbarnstilleggTask.TASK_STEP_TYPE,
                    payload = objectMapper.writeValueAsString(
                        AutobrevOpphørSmåbarnstilleggDTO(
                            fagsakId = fagsakId
                        )
                    ),
                    properties = Properties().apply {
                        this["fagsakId"] = fagsakId.toString()
                    }
                )
            )
        }
    }

    fun opprettSatsendringTask(fagsakId: Long, satstidspunkt: YearMonth) {
        overstyrTaskMedNyCallId(IdUtils.generateId()) {
            taskRepository.save(
                Task(
                    type = SatsendringTask.TASK_STEP_TYPE,
                    payload = objectMapper.writeValueAsString(SatsendringTaskDto(fagsakId, satstidspunkt)),
                    properties = Properties().apply {
                        this["fagsakId"] = fagsakId.toString()
                    }
                )
            )
        }
    }

    private inline fun <T> overstyrTaskMedNyCallId(callId: String, body: () -> T): T {
        val originalCallId = MDC.get(MDCConstants.MDC_CALL_ID) ?: null

        return try {
            MDC.put(MDCConstants.MDC_CALL_ID, callId)
            body()
        } finally {
            if (originalCallId == null) {
                MDC.remove(MDCConstants.MDC_CALL_ID)
            } else {
                MDC.put(MDCConstants.MDC_CALL_ID, originalCallId)
            }
        }
    }

    companion object {
        const val RETRY_BACKOFF_5000MS = "\${retry.backoff.delay:5000}"
    }
}
