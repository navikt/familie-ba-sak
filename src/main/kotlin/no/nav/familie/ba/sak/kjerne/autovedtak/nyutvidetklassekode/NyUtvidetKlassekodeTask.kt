package no.nav.familie.ba.sak.kjerne.autovedtak.nyutvidetklassekode

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ba.sak.config.FeatureToggleConfig.Companion.KJØR_AUTOVEDTAK_NY_KLASSEKODE_FOR_UTVIDET_BARNETRYGD
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.unleash.UnleashService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = NyUtvidetKlassekodeTask.TASK_STEP_TYPE,
    beskrivelse = "Migrer fagsak til ny klassekode for utvidet barnetrygd",
    maxAntallFeil = 1,
)
class NyUtvidetKlassekodeTask(
    private val autovedtakNyUtvidetKlassekodeService: AutovedtakNyUtvidetKlassekodeService,
    private val unleashService: UnleashService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        if (!unleashService.isEnabled(KJØR_AUTOVEDTAK_NY_KLASSEKODE_FOR_UTVIDET_BARNETRYGD)) {
            logger.info("Hopper ut av kjøring av migrering til ny klassekode for utvidet barnetrygd da toggle er skrudd av")
            return
        }

        val fagsakId: Long = objectMapper.readValue(task.payload)
        autovedtakNyUtvidetKlassekodeService.utførMigreringTilNyUtvidetKlassekode(fagsakId = fagsakId)
    }

    companion object {
        const val TASK_STEP_TYPE = "nyUtvidetKlassekodeTask"
        private val logger = LoggerFactory.getLogger(this::class.java)

        fun lagTask(fagsakId: Long) =
            Task(
                type = TASK_STEP_TYPE,
                payload = fagsakId.toString(),
            )
    }
}
