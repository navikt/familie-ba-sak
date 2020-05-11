package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.behandling.steg.JournalførVedtaksbrevDTO
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask.Companion.TASK_STEP_TYPE
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = TASK_STEP_TYPE, beskrivelse = "Journalfør brev i Joark", maxAntallFeil = 3)
class JournalførVedtaksbrevTask(
        private val vedtakService: VedtakService,
        private val stegService: StegService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val vedtakId = task.payload.toLong()
        val behandling = vedtakService.hent(vedtakId).behandling

        stegService.håndterJournalførVedtaksbrev(behandling, JournalførVedtaksbrevDTO(vedtakId = vedtakId, task = task))
    }

    companion object {
        const val TASK_STEP_TYPE = "journalførTilJoark"
        val LOG: Logger = LoggerFactory.getLogger(JournalførVedtaksbrevTask::class.java)
    }
}
