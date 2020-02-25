package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonTjeneste
import no.nav.familie.ba.sak.task.DistribuerVedtaksbrev.Companion.TASK_STEP_TYPE
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = TASK_STEP_TYPE, beskrivelse = "Send vedtaksbrev til Dokdist", maxAntallFeil = 3)
class DistribuerVedtaksbrev(
        private val integrasjonTjeneste: IntegrasjonTjeneste,
        private val featureToggleService: FeatureToggleService,
        private val taskRepository: TaskRepository
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val distribuerVedtaksbrevDTO = objectMapper.readValue(task.payload, DistribuerVedtaksbrevDTO::class.java)

        if (featureToggleService.isEnabled("familie-ba-sak.distribuer-vedtaksbrev")) {
            LOG.info("Iverksetter distribusjon av vedtaksbrev med journalpostId ${distribuerVedtaksbrevDTO.journalpostId}")
            integrasjonTjeneste.distribuerVedtaksbrev(distribuerVedtaksbrevDTO.journalpostId)
        } else {
            LOG.info("Hopper over istribusjon av vedtaksbrev. Funksjonen er skrudd av")
        }

        val ferdigstillBehandlingTask = FerdigstillBehandling.opprettTask(
                personIdent = distribuerVedtaksbrevDTO.personIdent,
                behandlingsId = distribuerVedtaksbrevDTO.behandlingId)
        taskRepository.save(ferdigstillBehandlingTask)
    }

    companion object {
        const val TASK_STEP_TYPE = "distribuerVedtaksbrev"
        val LOG: Logger = LoggerFactory.getLogger(DistribuerVedtaksbrev::class.java)
    }
}

data class DistribuerVedtaksbrevDTO(
        val behandlingId: Long,
        val journalpostId: String,
        val personIdent: String
)
