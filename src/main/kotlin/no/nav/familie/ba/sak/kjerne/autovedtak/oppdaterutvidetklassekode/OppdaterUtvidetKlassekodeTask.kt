package no.nav.familie.ba.sak.kjerne.autovedtak.oppdaterutvidetklassekode

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ba.sak.config.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = OppdaterUtvidetKlassekodeTask.TASK_STEP_TYPE,
    beskrivelse = "Migrer fagsak til ny klassekode for utvidet barnetrygd",
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
)
class OppdaterUtvidetKlassekodeTask(
    private val autovedtakOppdaterUtvidetKlassekodeService: AutovedtakOppdaterUtvidetKlassekodeService,
    private val unleashService: UnleashNextMedContextService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        if (!unleashService.isEnabled(FeatureToggle.KJØR_AUTOVEDTAK_OPPDATER_KLASSEKODE_FOR_UTVIDET_BARNETRYGD)) {
            logger.info("Hopper ut av kjøring av migrering til ny klassekode for utvidet barnetrygd da toggle er skrudd av")
            return
        }

        val fagsakId: Long = objectMapper.readValue(task.payload)
        autovedtakOppdaterUtvidetKlassekodeService.utførMigreringTilOppdatertUtvidetKlassekode(fagsakId = fagsakId)
    }

    companion object {
        const val TASK_STEP_TYPE = "oppdaterUtvidetKlassekodeTask"
        private val logger = LoggerFactory.getLogger(OppdaterUtvidetKlassekodeTask::class.java)

        fun lagTask(fagsakId: Long) =
            Task(
                type = TASK_STEP_TYPE,
                payload = fagsakId.toString(),
                properties =
                    Properties().apply {
                        this["fagsakId"] = fagsakId.toString()
                    },
            )
    }
}
