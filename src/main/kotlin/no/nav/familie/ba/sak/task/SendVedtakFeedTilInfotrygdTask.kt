package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.behandling.steg.SendVedtakFeedTilInfotrygdDTO
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.util.*

@Service
@TaskStepBeskrivelse(taskStepType = SendVedtakFeedTilInfotrygdTask.TASK_STEP_TYPE,
                     beskrivelse = "Send vedtak til Infotrygd feed.")
class SendVedtakFeedTilInfotrygdTask(
        private val vedtakService: VedtakService,
        private val stegService: StegService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val vedtakId = task.payload.toLong()
        val behandling = vedtakService.hent(vedtakId).behandling

        stegService.håndterSendVedtakfeedTilInfotrygd(behandling, SendVedtakFeedTilInfotrygdDTO(vedtakId = vedtakId))
    }

    companion object {
        const val TASK_STEP_TYPE = "sendVedtakFeedTilInfotrygd"
        val secureLogger = LoggerFactory.getLogger("secureLogger")

        fun opprettTask(vedtakId: Long): Task {
            secureLogger.info("Send vedtakfeed for vedtak med id $vedtakId til Infotrygd.")

            val metadata = Properties().apply {
                this["vedtaksId"] = vedtakId.toString()
                if (!MDC.get(MDCConstants.MDC_CALL_ID).isNullOrEmpty()) {
                    this["callId"] = MDC.get(MDCConstants.MDC_CALL_ID) ?: IdUtils.generateId()
                }
            }

            return Task.nyTask(type = TASK_STEP_TYPE,
                               payload = "$vedtakId",
                               properties = metadata
            )
        }
    }
}
