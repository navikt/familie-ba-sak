package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.config.FeatureToggleService
//import no.nav.familie.ba.sak.integrasjoner.IntegrasjonTjeneste
import no.nav.familie.ba.sak.task.DistribuerVedtaksbrev.Companion.TASK_STEP_TYPE
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = TASK_STEP_TYPE, beskrivelse = "Send vedtaksbrev til Dokdist", maxAntallFeil = 3)
class DistribuerVedtaksbrev(
        //private val integrasjonTjeneste: IntegrasjonTjeneste,
        private val featureToggleService: FeatureToggleService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        if (featureToggleService.isEnabled("familie-ba-sak.distribuer-vedtaksbrev")){
            //LOG.info("Iverksetter distribusjon av vedtaksbrev med journalpostId ${task.payload}")
            //integrasjonTjeneste.distribuerVedtaksbrev(task.payload)  // TODO: Kommenter inn etter at feature toggle er verifisert i prod
        } else {
            LOG.info("Hopper over istribusjon av vedtaksbrev. Funksjonen er skrudd av")
        }
    }

    companion object {
        const val TASK_STEP_TYPE = "distribuerVedtaksbrev"
        val LOG: Logger = LoggerFactory.getLogger(DistribuerVedtaksbrev::class.java)
    }
}
