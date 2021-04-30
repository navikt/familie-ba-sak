package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.ba.sak.task.IverksettMotFamilieTilbakeTask
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.ba.sak.task.dto.StatusFraOppdragDTO
import no.nav.familie.ba.sak.økonomi.ØkonomiService
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties

data class StatusFraOppdragMedTask(
        val statusFraOppdragDTO: StatusFraOppdragDTO,
        val task: Task
)

@Service
class StatusFraOppdrag(
        private val økonomiService: ØkonomiService,
        private val taskRepository: TaskRepository) : BehandlingSteg<StatusFraOppdragMedTask> {

    override fun utførStegOgAngiNeste(behandling: Behandling,
                                      data: StatusFraOppdragMedTask): StegType {
        val statusFraOppdragDTO = data.statusFraOppdragDTO
        val task = data.task

        Result.runCatching { økonomiService.hentStatus(statusFraOppdragDTO.oppdragId) }
                .onFailure { throw it }
                .onSuccess {
                    logger.debug("Mottok status '$it' fra oppdrag")
                    if (it != OppdragStatus.KVITTERT_OK) {
                        if (it == OppdragStatus.LAGT_PÅ_KØ) {
                            task.triggerTid = LocalDateTime.now().plusMinutes(15)
                            taskRepository.save(task)
                        } else {
                            task.status = Status.MANUELL_OPPFØLGING
                            taskRepository.save(task)
                        }

                        error("Mottok status '$it' fra oppdrag")
                    } else {
                        val nesteSteg = hentNesteStegForNormalFlyt(behandling)
                        when (nesteSteg) {
                            StegType.JOURNALFØR_VEDTAKSBREV -> opprettTaskJournalførVedtaksbrev(statusFraOppdragDTO.vedtaksId,
                                                                                                task)
                            StegType.IVERKSETT_MOT_FAMILIE_TILBAKE -> opprettTaskIverksettMotTilbake(
                                    statusFraOppdragDTO.behandlingsId,
                                    task.metadata)
                            StegType.FERDIGSTILLE_BEHANDLING -> opprettFerdigstillBehandling(statusFraOppdragDTO)
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

    private fun opprettTaskIverksettMotTilbake(behandlingsId: Long, metadata: Properties) {
        val ferdigstillBehandling = IverksettMotFamilieTilbakeTask.opprettTask(
                behandlingsId, metadata
        )
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

    companion object {

        private val logger = LoggerFactory.getLogger(StatusFraOppdrag::class.java)
    }
}