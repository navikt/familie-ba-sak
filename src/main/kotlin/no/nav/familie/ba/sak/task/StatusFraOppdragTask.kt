package no.nav.familie.ba.sak.task

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.steg.StatusFraOppdragMedTask
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.task.StatusFraOppdragTask.Companion.TASK_STEP_TYPE
import no.nav.familie.ba.sak.task.dto.StatusFraOppdragDTO
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties

/**
 * Task som kjører 100 ganger før den blir satt til feilet.
 * 100 ganger tilsvarer ca 1 døgn med rekjøringsintervall 15 minutter.
 *
 *
 * Infotrygd er vanligvis stengt mellom 21 og 6, men ikke alltid.
 * Hvis tasken/steget feiler i denne tida så lager den en ny task og kjører den kl 06
 */
@Service
@TaskStepBeskrivelse(
    taskStepType = TASK_STEP_TYPE,
    beskrivelse = "Henter status fra oppdrag",
    maxAntallFeil = 100,
)
class StatusFraOppdragTask(
    private val stegService: StegService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val taskRepository: TaskRepositoryWrapper,
    private val featureToggleService: FeatureToggleService,
) : AsyncTaskStep {
    /**
     * Metoden prøver å hente kvittering i ét døgn.
     * Får tasken kvittering som ikke er OK feiler vi tasken.
     */
    @WithSpan
    override fun doTask(task: Task) {
        val statusFraOppdragDTO = jsonMapper.readValue(task.payload, StatusFraOppdragDTO::class.java)

        stegService.håndterStatusFraØkonomi(
            behandling = behandlingHentOgPersisterService.hent(behandlingId = statusFraOppdragDTO.behandlingsId),
            statusFraOppdragMedTask = StatusFraOppdragMedTask(statusFraOppdragDTO = statusFraOppdragDTO, task = task),
        )
    }

    override fun onCompletion(task: Task) {
        val statusFraOppdragDTO = jsonMapper.readValue(task.payload, StatusFraOppdragDTO::class.java)

        val nyTaskV2 = PubliserVedtakV2Task.opprettTask(statusFraOppdragDTO.personIdent, statusFraOppdragDTO.behandlingsId)
        taskRepository.save(nyTaskV2)
    }

    companion object {
        const val TASK_STEP_TYPE = "statusFraOppdrag"

        fun opprettTask(
            statusFraOppdragDTO: StatusFraOppdragDTO,
            properties: Properties,
        ): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = jsonMapper.writeValueAsString(statusFraOppdragDTO),
                properties = properties,
            )
    }
}
