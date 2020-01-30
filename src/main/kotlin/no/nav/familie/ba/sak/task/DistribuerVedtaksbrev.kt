package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.integrasjoner.IntegrasjonTjeneste
import no.nav.familie.ba.sak.task.DistribuerVedtaksbrev.Companion.TASK_STEP_TYPE
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = TASK_STEP_TYPE, beskrivelse = "Send vedtaksbrev til Dokdist", maxAntallFeil = 3)
class DistribuerVedtaksbrev(
    private val integrasjonTjeneste: IntegrasjonTjeneste
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        LOG.debug("Iverksetter vedtaksbrev med journalpostId ${task.payload} mot dokdist")
        integrasjonTjeneste.distribuerVedtaksbrev(task.payload)
    }

    override fun onCompletion(task: Task) {
        LOG.debug("Iverksetting vedtaksbrev med journalpostId ${task.payload} gikk OK")
    }

    companion object {
        const val TASK_STEP_TYPE = "distribuerVedtaksbrev"
        val LOG = LoggerFactory.getLogger(DistribuerVedtaksbrev::class.java)
    }
}