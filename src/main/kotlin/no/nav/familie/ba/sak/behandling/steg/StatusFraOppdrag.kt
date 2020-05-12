package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.ba.sak.task.dto.StatusFraOppdragDTO
import no.nav.familie.ba.sak.økonomi.OppdragProtokollStatus
import no.nav.familie.ba.sak.økonomi.ØkonomiService
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

data class StatusFraOppdragMedTask(
        val statusFraOppdragDTO: StatusFraOppdragDTO,
        val task: Task
)

@Service
class StatusFraOppdrag(
        private val behandlingService: BehandlingService,
        private val økonomiService: ØkonomiService,
        private val taskRepository: TaskRepository) : BehandlingSteg<StatusFraOppdragMedTask> {

    private val LOG = LoggerFactory.getLogger(this.javaClass)

    override fun utførStegOgAngiNeste(behandling: Behandling,
                                      data: StatusFraOppdragMedTask,
                                      stegService: StegService?): StegType {
        val statusFraOppdragDTO = data.statusFraOppdragDTO
        val task = data.task

        Result.runCatching { økonomiService.hentStatus(data.statusFraOppdragDTO) }
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

                        error("Mottok status '$it' fra oppdrag")
                    } else {
                        behandlingService.oppdaterStatusPåBehandling(
                                statusFraOppdragDTO.behandlingsId,
                                BehandlingStatus.IVERKSATT
                        )

                        if (behandling.type !== BehandlingType.MIGRERING_FRA_INFOTRYGD
                            && behandling.type !== BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT
                            && behandling.type !== BehandlingType.TEKNISK_OPPHØR) {
                            opprettTaskJournalførVedtaksbrev(statusFraOppdragDTO.vedtaksId,
                                                             task)
                        } else {
                            opprettFerdigstillBehandling(statusFraOppdragDTO)
                        }
                    }
                }

        return hentNesteStegForNormalFlyt(behandling)
    }

    private fun opprettFerdigstillBehandling(statusFraOppdragDTO: StatusFraOppdragDTO) {
        val ferdigstillBehandling = FerdigstillBehandlingTask.opprettTask(personIdent = statusFraOppdragDTO.personIdent,
                                                                          behandlingsId = statusFraOppdragDTO.behandlingsId)
        taskRepository.save(ferdigstillBehandling)
    }

    private fun opprettTaskJournalførVedtaksbrev(vedtakId: Long, gammelTask: Task) {
        val task = Task.nyTask(JournalførVedtaksbrevTask.TASK_STEP_TYPE,
                               "$vedtakId",
                               gammelTask.metadata)
        taskRepository.save(task)
    }

    override fun stegType(): StegType {
        return StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI
    }
}