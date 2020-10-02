package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.infotrygd.InfotrygdFeedClient
import no.nav.familie.ba.sak.infotrygd.InfotrygdFeedService
import no.nav.familie.ba.sak.infotrygd.domene.InfotrygdFødselhendelsesFeedDto
import no.nav.familie.ba.sak.infotrygd.domene.InfotrygdFødselhendelsesFeedTaskDto
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.util.*

@Service
@TaskStepBeskrivelse(taskStepType = SendFeedTilInfotrygdTask.TASK_STEP_TYPE,
                     beskrivelse = "Send fødselshendelse til Infotrygd feed.")
class SendFeedTilInfotrygdTask(
        private val infotrygdFeedClient: InfotrygdFeedClient) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val infotrygdFeedTaskDto = objectMapper.readValue(task.payload, InfotrygdFødselhendelsesFeedTaskDto::class.java)

        infotrygdFeedTaskDto.fnrBarn.forEach {
            infotrygdFeedClient.sendFødselhendelsesFeedTilInfotrygd(InfotrygdFødselhendelsesFeedDto(fnrBarn = it))
        }
    }

    companion object {

        const val TASK_STEP_TYPE = "sendFeedTilInfotrygd"

        fun opprettTask(fnrBarn: List<String>): Task {
            InfotrygdFeedService.secureLogger.info("Send fødselsmelding for $fnrBarn til Infotrygd.")

            val metadata = Properties().apply {
                this["personIdenterBarn"] = fnrBarn.toString()
                if (!MDC.get(MDCConstants.MDC_CALL_ID).isNullOrEmpty()) {
                    this["callId"] = MDC.get(MDCConstants.MDC_CALL_ID) ?: IdUtils.generateId()
                }
            }

            return Task.nyTask(type = TASK_STEP_TYPE,
                               payload = objectMapper.writeValueAsString(InfotrygdFødselhendelsesFeedTaskDto(
                                       fnrBarn = fnrBarn)),
                               properties = metadata
            )
        }
    }
}
