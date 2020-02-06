package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.task.StatusFraOppdrag.Companion.TASK_STEP_TYPE
import no.nav.familie.ba.sak.økonomi.OppdragProtokollStatus
import no.nav.familie.ba.sak.økonomi.StatusFraOppdragDTO
import no.nav.familie.ba.sak.økonomi.ØkonomiService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
@TaskStepBeskrivelse(taskStepType = TASK_STEP_TYPE, beskrivelse = "Henter status fra oppdrag", maxAntallFeil = 100)
class StatusFraOppdrag(
        private val økonomiService: ØkonomiService,
        private val behandlingService: BehandlingService,
        private val taskRepository: TaskRepository
) : AsyncTaskStep {

    /**
     * Metoden prøver å hente kvittering i ét døgn.
     * Får tasken kvittering som ikke er OK feiler vi tasken.
     */
    override fun doTask(task: Task) {
        val statusFraOppdragDTO = objectMapper.readValue(task.payload, StatusFraOppdragDTO::class.java)
        val behandling = behandlingService.hentBehandling(statusFraOppdragDTO.behandlingsId)

        Result.runCatching { økonomiService.hentStatus(statusFraOppdragDTO) }
                .onFailure { throw it }
                .onSuccess {
                    LOG.debug("Mottok status '$it' fra oppdrag")
                    if (it != OppdragProtokollStatus.KVITTERT_OK) {
                        if (it == OppdragProtokollStatus.LAGT_PÅ_KØ) {
                            task.triggerTid = LocalDateTime.now().plusMinutes(15)
                            taskRepository.save(task)
                        } else {
                            task.status = Status.MANUELL_OPPFØLGING
                            taskRepository.save(task)
                        }

                        throw Exception("Mottok status '$it' fra oppdrag")
                    } else {
                        behandlingService.oppdaterStatusPåBehandling(
                                statusFraOppdragDTO.behandlingsId,
                                BehandlingStatus.IVERKSATT
                        )

                        if (behandling?.type != BehandlingType.MIGRERING_FRA_INFOTRYGD) {
                            opprettTaskJournalførVedtaksbrev(statusFraOppdragDTO.vedtaksId, task)
                        }
                    }
                }
    }

    private fun opprettTaskJournalførVedtaksbrev(vedtakId: Long, gammelTask: Task) {
        val task = Task.nyTask(JournalførVedtaksbrev.TASK_STEP_TYPE, "$vedtakId", gammelTask.metadata)
        taskRepository.save(task)
    }

    companion object {
        const val TASK_STEP_TYPE = "statusFraOppdrag"
        val LOG: Logger = LoggerFactory.getLogger(StatusFraOppdrag::class.java)
    }
}
