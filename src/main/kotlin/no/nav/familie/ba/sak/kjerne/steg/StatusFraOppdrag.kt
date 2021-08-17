package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.ba.sak.task.IverksettMotFamilieTilbakeTask
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.ba.sak.task.dto.StatusFraOppdragDTO
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.prosessering.internal.RekjørSenereException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

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

        val oppdragStatus = økonomiService.hentStatus(statusFraOppdragDTO.oppdragId)
        logger.debug("Mottok status '$oppdragStatus' fra oppdrag")
        if (oppdragStatus != OppdragStatus.KVITTERT_OK) {
            if (oppdragStatus == OppdragStatus.LAGT_PÅ_KØ) {
                throw RekjørSenereException(årsak = "Mottok lagt på kø kvittering fra oppdrag.",
                                            triggerTid = if (erKlokkenMellom21Og06()) kl06IdagEllerNesteDag() else LocalDateTime.now()
                                                    .plusMinutes(15))
            } else {
                task.status = Status.MANUELL_OPPFØLGING
                taskRepository.save(task)
            }

            error("Mottok status '$oppdragStatus' fra oppdrag")
        } else {
            when (hentNesteStegForNormalFlyt(behandling)) {
                StegType.JOURNALFØR_VEDTAKSBREV -> opprettTaskJournalførVedtaksbrev(statusFraOppdragDTO.vedtaksId,
                                                                                    task)
                StegType.IVERKSETT_MOT_FAMILIE_TILBAKE -> opprettTaskIverksettMotTilbake(
                        statusFraOppdragDTO.behandlingsId,
                        task.metadata)
                StegType.FERDIGSTILLE_BEHANDLING -> opprettFerdigstillBehandling(statusFraOppdragDTO)
                else -> error("Neste task er ikke implementert.")
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

    private fun erKlokkenMellom21Og06(): Boolean {
        val localTime = LocalTime.now()
        return localTime.isAfter(LocalTime.of(21, 0)) || localTime.isBefore(LocalTime.of(6, 0))
    }

    private fun kl06IdagEllerNesteDag(): LocalDateTime {
        val now = LocalDateTime.now()
        return if (now.toLocalTime().isBefore(LocalTime.of(6, 0))) {
            now.toLocalDate().atTime(6, 0)
        } else {
            now.toLocalDate().plusDays(1).atTime(6, 0)
        }
    }

    override fun stegType(): StegType {
        return StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI
    }

    companion object {

        private val logger = LoggerFactory.getLogger(StatusFraOppdrag::class.java)
    }
}