package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.ba.sak.task.IverksettMotFamilieTilbakeTask
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.ba.sak.task.dto.StatusFraOppdragDTO
import no.nav.familie.ba.sak.task.nesteGyldigeTriggertidForBehandlingIHverdager
import no.nav.familie.ba.sak.økonomi.ØkonomiService
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.error.RekjørSenereException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties

data class StatusFraOppdragMedTask(
    val statusFraOppdragDTO: StatusFraOppdragDTO,
    val task: Task
)

@Service
class StatusFraOppdrag(
    private val økonomiService: ØkonomiService,
    private val taskRepository: TaskRepositoryWrapper
) : BehandlingSteg<StatusFraOppdragMedTask> {

    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: StatusFraOppdragMedTask
    ): StegType {
        val statusFraOppdragDTO = data.statusFraOppdragDTO
        val task = data.task

        val oppdragStatus = økonomiService.hentStatus(statusFraOppdragDTO.oppdragId)
        logger.debug("Mottok status '$oppdragStatus' fra oppdrag")
        if (oppdragStatus != OppdragStatus.KVITTERT_OK) {
            if (oppdragStatus == OppdragStatus.LAGT_PÅ_KØ) {
                throw RekjørSenereException(
                    årsak = "Mottok lagt på kø kvittering fra oppdrag.",
                    triggerTid = nesteGyldigeTriggertidForBehandlingIHverdager(minutesToAdd = 15)
                )
            } else {
                taskRepository.save(task.copy(status = Status.MANUELL_OPPFØLGING))
            }

            error("Mottok status '$oppdragStatus' fra oppdrag")
        } else {
            when (hentNesteStegForNormalFlyt(behandling)) {
                StegType.JOURNALFØR_VEDTAKSBREV -> opprettTaskJournalførVedtaksbrev(
                    statusFraOppdragDTO.vedtaksId,
                    task
                )
                StegType.IVERKSETT_MOT_FAMILIE_TILBAKE -> opprettTaskIverksettMotTilbake(
                    statusFraOppdragDTO.behandlingsId,
                    task.metadata
                )
                StegType.FERDIGSTILLE_BEHANDLING -> opprettFerdigstillBehandling(statusFraOppdragDTO)
                else -> error("Neste task er ikke implementert.")
            }
        }

        return hentNesteStegForNormalFlyt(behandling)
    }

    private fun opprettFerdigstillBehandling(statusFraOppdragDTO: StatusFraOppdragDTO) {
        val ferdigstillBehandling = FerdigstillBehandlingTask.opprettTask(
            personIdent = statusFraOppdragDTO.personIdent,
            behandlingsId = statusFraOppdragDTO.behandlingsId
        )
        taskRepository.save(ferdigstillBehandling)
    }

    private fun opprettTaskIverksettMotTilbake(behandlingsId: Long, metadata: Properties) {
        val ferdigstillBehandling = IverksettMotFamilieTilbakeTask.opprettTask(
            behandlingsId, metadata
        )
        taskRepository.save(ferdigstillBehandling)
    }

    private fun opprettTaskJournalførVedtaksbrev(vedtakId: Long, gammelTask: Task) {
        val task = Task(
            type = JournalførVedtaksbrevTask.TASK_STEP_TYPE,
            payload = "$vedtakId",
            properties = gammelTask.metadata
        )
        taskRepository.save(task)
    }

    override fun stegType(): StegType {
        return StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI
    }

    companion object {

        private val logger = LoggerFactory.getLogger(StatusFraOppdrag::class.java)
    }
}
