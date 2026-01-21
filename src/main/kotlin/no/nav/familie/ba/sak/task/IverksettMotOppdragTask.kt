package no.nav.familie.ba.sak.task

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask.Companion.TASK_STEP_TYPE
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.ba.sak.task.dto.IverksettingTaskDTO
import no.nav.familie.ba.sak.task.dto.StatusFraOppdragDTO
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(taskStepType = TASK_STEP_TYPE, beskrivelse = "Iverksett vedtak mot oppdrag", maxAntallFeil = 3)
class IverksettMotOppdragTask(
    private val stegService: StegService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val taskRepository: TaskRepositoryWrapper,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val iverksettingTask = jsonMapper.readValue(task.payload, IverksettingTaskDTO::class.java)
        stegService.håndterIverksettMotØkonomi(
            behandling = behandlingHentOgPersisterService.hent(iverksettingTask.behandlingsId),
            iverksettingTaskDTO = iverksettingTask,
        )
    }

    override fun onCompletion(task: Task) {
        val iverksettingTask = jsonMapper.readValue(task.payload, IverksettingTaskDTO::class.java)
        val personIdent =
            behandlingHentOgPersisterService
                .hent(iverksettingTask.behandlingsId)
                .fagsak.aktør
                .aktivFødselsnummer()
        val statusFraOppdragTask =
            StatusFraOppdragTask.opprettTask(
                StatusFraOppdragDTO(
                    personIdent = personIdent,
                    fagsystem = FAGSYSTEM,
                    behandlingsId = iverksettingTask.behandlingsId,
                    vedtaksId = iverksettingTask.vedtaksId,
                ),
                properties = task.metadata,
            )

        val sendMeldingTilBisysTask =
            Task(
                type = SendMeldingTilBisysTask.TASK_STEP_TYPE,
                payload = iverksettingTask.behandlingsId.toString(),
            )

        taskRepository.save(statusFraOppdragTask)
        taskRepository.save(sendMeldingTilBisysTask)
    }

    companion object {
        const val TASK_STEP_TYPE = "iverksettMotOppdrag"

        fun opprettTask(
            behandling: Behandling,
            vedtak: Vedtak,
            saksbehandlerId: String,
        ): Task =
            opprettTask(
                behandling.fagsak.aktør,
                behandling.id,
                vedtak.id,
                saksbehandlerId,
                behandling.fagsak.id,
            )

        fun opprettTask(
            aktør: Aktør,
            behandlingsId: Long,
            vedtaksId: Long,
            saksbehandlerId: String,
            fagsakId: Long,
        ): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload =
                    jsonMapper.writeValueAsString(
                        IverksettingTaskDTO(
                            personIdent = aktør.aktivFødselsnummer(),
                            behandlingsId = behandlingsId,
                            vedtaksId = vedtaksId,
                            saksbehandlerId = saksbehandlerId,
                        ),
                    ),
                properties =
                    Properties().apply {
                        this["personIdent"] = aktør.aktivFødselsnummer()
                        this["behandlingId"] = behandlingsId.toString()
                        this["vedtakId"] = vedtaksId.toString()
                        this["fagsakId"] = fagsakId.toString()
                    },
            )
    }
}
