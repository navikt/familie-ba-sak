package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.task.dto.Autobrev6og18ÅrDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = SendAutobrev6og18ÅrTask.TASK_STEP_TYPE,
                     beskrivelse = "Send autobrev for barn som fyller 6 og 18 år til Dokdist",
                     maxAntallFeil = 3,
                     triggerTidVedFeilISekunder = 60 * 60 * 24)
class SendAutobrev6og18ÅrTask(
        private val behandlingService: BehandlingService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val autobrevDTO = objectMapper.readValue(task.payload, Autobrev6og18ÅrDTO::class.java)
        val behandling = behandlingService.hent(autobrevDTO.behandlingsId)

        // Finne ut om fagsak er løpende -> hvis nei, avslutt

        // Hvis barn er 18 år og ingen andre barn er på fagsaken -> avslutt

        // Finne ut om fagsak har behandling som ikke er fullført -> hvis ja, feile task og logge feil og øke metrikk
        //  hvis tasken forsøker for siste gang -> opprett oppgave for å håndtere videre manuelt

        // Opprett ny behandling (revurdering) med årsak "Omregning". Vilkårsvurdering skal være uforandret. Fullfør
        // behandling uten manuell to-trinnskontroll og oversendelse til økonomi.

        // Send brev, journalfør og skriv metrikk.


        LOG.info("SendAutobrev6og18ÅrTask for behandling ${autobrevDTO.behandlingsId}")
    }

    companion object {
        const val TASK_STEP_TYPE = "sendAutobrevVed6og18År"
        val LOG: Logger = LoggerFactory.getLogger(SendAutobrev6og18ÅrTask::class.java)
    }
}

