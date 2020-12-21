package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.NyBehandling
import no.nav.familie.ba.sak.behandling.autobrev.Autobrev6og18ÅrService
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
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
                     triggerTidVedFeilISekunder = 60 * 60 * 24) //TODO: Vi trenger kanskje ikke denne? Hva er default?
class SendAutobrev6og18ÅrTask(
        private val autobrev6og18ÅrService: Autobrev6og18ÅrService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val autobrevDTO = objectMapper.readValue(task.payload, Autobrev6og18ÅrDTO::class.java)

        autobrev6og18ÅrService.opprettOmregningsoppgaveForBarnIBrytingsAlder(autobrevDTO)
    }

    companion object {

        const val TASK_STEP_TYPE = "sendAutobrevVed6og18År"
        val LOG: Logger = LoggerFactory.getLogger(SendAutobrev6og18ÅrTask::class.java)
    }
}

fun Person.fyllerAntallÅrInneværendeMåned(år: Int): Boolean {
    return this.fødselsdato.isAfter(LocalDate.now().minusYears(år.toLong()).førsteDagIInneværendeMåned()) &&
           this.fødselsdato.isBefore(LocalDate.now().minusYears(år.toLong()).sisteDagIMåned())
}

