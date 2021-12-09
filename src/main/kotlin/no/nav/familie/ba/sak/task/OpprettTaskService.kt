package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.task.dto.AutobrevAlderDTO
import no.nav.familie.ba.sak.task.dto.AutobrevOpphørOvergangsstonadDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.log.IdUtils
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.Properties

@Service
class OpprettTaskService(
    val taskRepository: TaskRepositoryWrapper
) {

    fun opprettOppgaveTask(
        behandlingId: Long,
        oppgavetype: Oppgavetype,
        beskrivelse: String? = null
    ) {
        taskRepository.save(
            OpprettOppgaveTask.opprettTask(
                behandlingId = behandlingId,
                oppgavetype = oppgavetype,
                fristForFerdigstillelse = LocalDate.now(),
                beskrivelse = beskrivelse
            )
        )
    }

    fun opprettSendFeedTilInfotrygdTask(barnasIdenter: List<String>) {
        taskRepository.save(SendFødselsmeldingTilInfotrygdTask.opprettTask(barnasIdenter))
    }

    fun opprettSendStartBehandlingTilInfotrygdTask(fnrStoenadsmottaker: String) {
        taskRepository.save(SendStartBehandlingTilInfotrygdTask.opprettTask(fnrStoenadsmottaker))
    }

    fun opprettAutovedtakForBarn(fagsakId: Long, alder: Int) {
        taskRepository.save(
            Task(
                type = SendAutobrevTask.TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(
                    AutobrevAlderDTO(
                        fagsakId = fagsakId,
                        alder = alder,
                        årMåned = inneværendeMåned()
                    )
                ),
                properties = Properties().apply {
                    this["fagsak"] = fagsakId.toString()
                    this["callId"] = IdUtils.generateId()
                }
            )
        )
    }

    fun opprettAutovedtakForOvergangsstonadOpphørTask(fagsakId: Long) {
        taskRepository.save(
            Task(
                type = SendAutobrevOpphørOvergangsstonadTask.TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(
                    AutobrevOpphørOvergangsstonadDTO(
                        fagsakId = fagsakId,
                    )
                ),
                properties = Properties().apply {
                    this["fagsak"] = fagsakId.toString()
                    this["callId"] = IdUtils.generateId()
                }
            )
        )
    }

    companion object {
        const val RETRY_BACKOFF_5000MS = "\${retry.backoff.delay:5000}"
    }
}
