package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.behandling.autobrev.Autobrev6og18ÅrService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.task.dto.Autobrev6og18ÅrDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
@TaskStepBeskrivelse(taskStepType = SendAutobrev6og18ÅrTask.TASK_STEP_TYPE,
                     beskrivelse = "Send autobrev for barn som fyller 6 og 18 år til Dokdist",
                     maxAntallFeil = 3,
                     triggerTidVedFeilISekunder = 60 * 60 * 24)
class SendAutobrev6og18ÅrTask(
        private val autobrev6og18ÅrService: Autobrev6og18ÅrService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val autobrevDTO = objectMapper.readValue(task.payload, Autobrev6og18ÅrDTO::class.java)

        if (!LocalDate.now().toYearMonth().equals(autobrevDTO.årMåned)) {
            throw Feil("Task for autobrev må kjøres innenfor måneden det skal sjekkes mot.")
        }

        autobrev6og18ÅrService.opprettOmregningsoppgaveForBarnIBrytingsalder(autobrevDTO)
    }

    companion object {

        const val TASK_STEP_TYPE = "sendAutobrevVed6og18År"
        val LOG: Logger = LoggerFactory.getLogger(SendAutobrev6og18ÅrTask::class.java)
    }
}

fun Person.fyllerAntallÅrInneværendeMåned(år: Int): Boolean {
    return this.fødselsdato.isSameOrAfter(LocalDate.now().minusYears(år.toLong()).førsteDagIInneværendeMåned()) &&
           this.fødselsdato.isSameOrBefore(LocalDate.now().minusYears(år.toLong()).sisteDagIMåned())
}

