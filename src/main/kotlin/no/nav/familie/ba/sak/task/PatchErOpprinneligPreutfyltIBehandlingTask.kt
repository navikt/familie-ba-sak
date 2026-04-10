package no.nav.familie.ba.sak.task

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultatRepository
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

@Service
@TaskStepBeskrivelse(
    taskStepType = PatchErOpprinneligPreutfyltIBehandlingTask.TASK_STEP_TYPE,
    beskrivelse = "Setter erOpprinneligPreutfyltIBehandling til sistEndretIBehandlingId for vilkårresultater der feltet mangler.",
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
)
class PatchErOpprinneligPreutfyltIBehandlingTask(
    private val vilkårResultatRepository: VilkårResultatRepository,
) : AsyncTaskStep {
    private val logger = LoggerFactory.getLogger(PatchErOpprinneligPreutfyltIBehandlingTask::class.java)

    @WithSpan
    override fun doTask(task: Task) {
        val dto = jsonMapper.readValue(task.payload, PatchErOpprinneligPreutfyltIBehandlingDto::class.java)

        val (vilkårResultatIder, durationHenting) =
            measureTimedValue {
                vilkårResultatRepository
                    .finnPreutfylteVilkårResultaterUtenBehandlingId(dto.antall)
            }

        logger.info("Fant ${vilkårResultatIder.size} vilkårresultater på ${durationHenting.inWholeSeconds} sekunder")

        if (dto.dryRun) return

        val durationOppdatering =
            measureTime {
                vilkårResultatIder.forEach { id ->
                    vilkårResultatRepository.oppdaterErOpprinneligPreutfyltIBehandling(id = id)
                }
            }

        logger.info("Oppdaterte ${vilkårResultatIder.size} vilkårresultater på ${durationOppdatering.inWholeSeconds} sekunder")
    }

    companion object {
        const val TASK_STEP_TYPE = "PatchErOpprinneligPreutfyltIBehandling"

        fun opprettTask(
            antall: Int,
            dryRun: Boolean,
        ): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = jsonMapper.writeValueAsString(PatchErOpprinneligPreutfyltIBehandlingDto(antall, dryRun)),
            )
    }
}

data class PatchErOpprinneligPreutfyltIBehandlingDto(
    val antall: Int,
    val dryRun: Boolean,
)
