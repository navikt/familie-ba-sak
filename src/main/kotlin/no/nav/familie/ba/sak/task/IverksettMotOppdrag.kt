package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.task.IverksettMotOppdrag.Companion.TASK_STEP_TYPE
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.ba.sak.task.dto.IverksettingTaskDTO
import no.nav.familie.ba.sak.task.dto.StatusFraOppdragDTO
import no.nav.familie.ba.sak.økonomi.ØkonomiService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
@TaskStepBeskrivelse(taskStepType = TASK_STEP_TYPE, beskrivelse = "Iverksett vedtak mot oppdrag", maxAntallFeil = 3)
class IverksettMotOppdrag(
        private val behandlingService: BehandlingService,
        private val økonomiService: ØkonomiService,
        private val taskRepository: TaskRepository
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val iverksettingTask = objectMapper.readValue(task.payload, IverksettingTaskDTO::class.java)
        LOG.debug("Iverksetter vedtak med ID ${iverksettingTask.vedtaksId} mot oppdrag")
        økonomiService.oppdaterTilkjentYtelseOgIverksettVedtak(iverksettingTask.behandlingsId,
                                       iverksettingTask.vedtaksId,
                                       iverksettingTask.saksbehandlerId)
    }

    override fun onCompletion(task: Task) {
        val iverksettingTask = objectMapper.readValue(task.payload, IverksettingTaskDTO::class.java)
        val behandling = behandlingService.hent(iverksettingTask.behandlingsId)
        check(behandling.status == BehandlingStatus.SENDT_TIL_IVERKSETTING) {
            "Skal iverksette mot økonomi, men behandlingen har status ${behandling.status}."
        }

        LOG.debug("Iverksetting av vedtak med ID ${iverksettingTask.vedtaksId} mot oppdrag gikk OK.")

        val nyTask = Task.nyTask(
                type = StatusFraOppdrag.TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(StatusFraOppdragDTO(
                        personIdent = iverksettingTask.personIdent,
                        fagsystem = FAGSYSTEM,
                        behandlingsId = iverksettingTask.behandlingsId,
                        vedtaksId = iverksettingTask.vedtaksId
                )),
                properties = task.metadata
        )
        taskRepository.save(nyTask)
    }

    companion object {
        const val TASK_STEP_TYPE = "iverksettMotOppdrag"
        val LOG = LoggerFactory.getLogger(IverksettMotOppdrag::class.java)


        fun opprettTask(behandling: Behandling, vedtak: Vedtak, saksbehandlerId: String): Task {

            return opprettTask(behandling.fagsak.personIdent.ident,
                               behandling.id,
                               vedtak.id,
                               saksbehandlerId)
        }

        fun opprettTask(personIdent: String, behandlingsId: Long, vedtaksId: Long, saksbehandlerId: String): Task {
            return Task.nyTask(type = TASK_STEP_TYPE,
                               payload = objectMapper.writeValueAsString(IverksettingTaskDTO(
                                       personIdent = personIdent,
                                       behandlingsId = behandlingsId,
                                       vedtaksId = vedtaksId,
                                       saksbehandlerId = saksbehandlerId
                               )),
                               properties = Properties().apply {
                                   this["personIdent"] = personIdent
                                   this["behandlingsId"] = behandlingsId.toString()
                                   this["vedtakId"] = vedtaksId.toString()
                               }
            )
        }
    }
}
