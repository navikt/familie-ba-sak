package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask.Companion.TASK_STEP_TYPE
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.ba.sak.task.dto.IverksettingTaskDTO
import no.nav.familie.ba.sak.task.dto.StatusFraOppdragDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(taskStepType = TASK_STEP_TYPE, beskrivelse = "Iverksett vedtak mot oppdrag", maxAntallFeil = 3)
class IverksettMotOppdragTask(
    private val stegService: StegService,
    private val behandlingService: BehandlingService,
    private val beregningService: BeregningService,
    private val personidentService: PersonidentService,
    private val taskRepository: TaskRepositoryWrapper
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val iverksettingTask = objectMapper.readValue(task.payload, IverksettingTaskDTO::class.java)
        val behandling = behandlingService.hent(iverksettingTask.behandlingsId)

        if (beregningService.skalIverksettes(
                behandlingId = behandling.id,
                behandlingResultat = behandling.resultat,
                fagsakId = behandling.fagsak.id
            )
        ) {
            stegService.håndterIverksettMotØkonomi(
                behandling = behandlingService.hent(iverksettingTask.behandlingsId),
                iverksettingTaskDTO = iverksettingTask
            )
        }
    }

    override fun onCompletion(task: Task) {
        val iverksettingTask = objectMapper.readValue(task.payload, IverksettingTaskDTO::class.java)

        val behandling = behandlingService.hent(iverksettingTask.behandlingsId)
        val personIdent = personidentService.hentAktør(iverksettingTask.personIdent).aktivFødselsnummer()

        if (beregningService.skalIverksettes(
                behandlingId = behandling.id,
                behandlingResultat = behandling.resultat,
                fagsakId = behandling.fagsak.id,
            )
        ) {
            val statusFraOppdragTask = Task(
                type = StatusFraOppdragTask.TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(
                    StatusFraOppdragDTO(
                        aktørId = iverksettingTask.personIdent,
                        personIdent = personIdent,
                        fagsystem = FAGSYSTEM,
                        behandlingsId = iverksettingTask.behandlingsId,
                        vedtaksId = iverksettingTask.vedtaksId
                    )
                ),
                properties = task.metadata
            )

            val sendMeldingTilBisysTask = Task(
                type = SendMeldingTilBisysTask.TASK_STEP_TYPE,
                payload = iverksettingTask.behandlingsId.toString()
            )

            taskRepository.save(statusFraOppdragTask)
            taskRepository.save(sendMeldingTilBisysTask)
        } else {
            val journalførVedtaksbrevTask = JournalførVedtaksbrevTask.opprettTaskJournalførVedtaksbrev(
                vedtakId = iverksettingTask.vedtaksId,
                personIdent = personIdent,
                behandlingId = iverksettingTask.behandlingsId
            )
            taskRepository.save(journalførVedtaksbrevTask)
        }
    }

    companion object {
        const val TASK_STEP_TYPE = "iverksettMotOppdrag"

        fun opprettTask(behandling: Behandling, vedtak: Vedtak, saksbehandlerId: String): Task {

            return opprettTask(
                behandling.fagsak.aktør,
                behandling.id,
                vedtak.id,
                saksbehandlerId
            )
        }

        fun opprettTask(aktør: Aktør, behandlingsId: Long, vedtaksId: Long, saksbehandlerId: String): Task {
            return Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(
                    IverksettingTaskDTO(
                        personIdent = aktør.aktivFødselsnummer(),
                        behandlingsId = behandlingsId,
                        vedtaksId = vedtaksId,
                        saksbehandlerId = saksbehandlerId
                    )
                ),
                properties = Properties().apply {
                    this["personIdent"] = aktør.aktivFødselsnummer()
                    this["behandlingsId"] = behandlingsId.toString()
                    this["vedtakId"] = vedtaksId.toString()
                }
            )
        }
    }
}
