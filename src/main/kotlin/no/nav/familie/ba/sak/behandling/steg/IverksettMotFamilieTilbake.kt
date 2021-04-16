package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.ba.sak.task.dto.IverksettMotFamilieTilbakeDTO
import no.nav.familie.ba.sak.task.dto.StatusFraOppdragDTO
import no.nav.familie.ba.sak.økonomi.ØkonomiService
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

data class IverksettMotFamilieTilbakeTask(
        val iverksettMotFamilieTilbakeDTO: IverksettMotFamilieTilbakeDTO,
        val task: Task
)

@Service
class IverksettMotFamilieTilbake(
        private val tilbakeService: TilbakeService,
        private val taskRepository: TaskRepository) : BehandlingSteg<IverksettMotFamilieTilbakeTask> {

    override fun utførStegOgAngiNeste(behandling: Behandling,
                                      data: IverksettMotFamilieTilbakeTask): StegType {
        val statusFraOppdragDTO = data.iverksettMotFamilieTilbakeDTO
        val task = data.task

        if (behandling.sendVedtaksbrev()) {
            opprettTaskJournalførVedtaksbrev(statusFraOppdragDTO.vedtaksId,
                                             task)
        } else {
            opprettFerdigstillBehandling(statusFraOppdragDTO)
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

    companion object {

        private val logger = LoggerFactory.getLogger(StatusFraOppdrag::class.java)
    }
}