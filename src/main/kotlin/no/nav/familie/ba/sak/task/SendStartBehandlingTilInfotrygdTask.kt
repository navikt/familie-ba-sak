package no.nav.familie.ba.sak.task

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdFeedKlient
import no.nav.familie.ba.sak.integrasjoner.infotrygd.domene.StartBehandlingDto
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = SendStartBehandlingTilInfotrygdTask.TASK_STEP_TYPE,
    beskrivelse = "Send startbehandling til Infotrygd feed.",
)
class SendStartBehandlingTilInfotrygdTask(
    private val infotrygdFeedKlient: InfotrygdFeedKlient,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val startBehandlingDto = jsonMapper.readValue(task.payload, StartBehandlingDto::class.java)
        infotrygdFeedKlient.sendStartBehandlingTilInfotrygd(startBehandlingDto)
    }

    companion object {
        const val TASK_STEP_TYPE = "SendStartBehandlingTilInfotrygd"

        fun opprettTask(aktørStoenadsmottaker: Aktør): Task {
            secureLogger.info("Oppretter task for å sende StartBehandling for ${aktørStoenadsmottaker.aktivFødselsnummer()} til Infotrygd.")

            val metadata =
                Properties().apply {
                    this["personIdenter"] = aktørStoenadsmottaker.aktivFødselsnummer()
                    if (!MDC.get(MDCConstants.MDC_CALL_ID).isNullOrEmpty()) {
                        this["callId"] = MDC.get(MDCConstants.MDC_CALL_ID) ?: IdUtils.generateId()
                    }
                }

            return Task(
                type = TASK_STEP_TYPE,
                payload =
                    jsonMapper.writeValueAsString(
                        StartBehandlingDto(
                            fnrStoenadsmottaker = aktørStoenadsmottaker.aktivFødselsnummer(),
                        ),
                    ),
                properties = metadata,
            )
        }
    }
}
