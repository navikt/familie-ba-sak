package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.FagsakStatus
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.task.dto.FerdigstillBehandlingDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
@TaskStepBeskrivelse(taskStepType = FerdigstillBehandling.TASK_STEP_TYPE,
                     beskrivelse = "Ferdigstill behandling",
                     maxAntallFeil = 3)
class FerdigstillBehandling(
        val fagsakService: FagsakService,
        val behandlingService: BehandlingService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val ferdigstillBehandling = objectMapper.readValue(task.payload, FerdigstillBehandlingDTO::class.java)
        LOG.info("Forsøker å ferdigstille behandling ${ferdigstillBehandling.behandlingsId}")

        val behandling = behandlingService.hent(ferdigstillBehandling.behandlingsId)
        val fagsak = behandling?.fagsak

        if (behandling?.status !== BehandlingStatus.IVERKSATT) {
            throw IllegalStateException("Prøver å ferdigstille behandling ${ferdigstillBehandling.behandlingsId}, men status er ${behandling?.status}")
        }

        if (behandling.resultat == BehandlingResultat.INNVILGET && fagsak?.status != FagsakStatus.LØPENDE) {
            fagsakService.oppdaterStatus(fagsak!!, FagsakStatus.LØPENDE)
        } else {
            fagsakService.oppdaterStatus(fagsak!!, FagsakStatus.STANSET)
        }

        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.FERDIGSTILT)
    }

    override fun onCompletion(task: Task) {
        val ferdigstillBehandling = objectMapper.readValue(task.payload, FerdigstillBehandlingDTO::class.java)

        LOG.info("Ferdigstillelse av behandling ${ferdigstillBehandling.behandlingsId} vellykket")
    }

    companion object {
        const val TASK_STEP_TYPE = "ferdigstillBehandling"
        val LOG = LoggerFactory.getLogger(FerdigstillBehandling::class.java)


        fun opprettTask(personIdent: String, behandlingsId: Long): Task {
            return Task.nyTask(type = TASK_STEP_TYPE,
                               payload = objectMapper.writeValueAsString(FerdigstillBehandlingDTO(
                                       personIdent = personIdent,
                                       behandlingsId = behandlingsId
                               )),
                               properties = Properties().apply {
                                   this["personIdent"] = personIdent
                                   this["behandlingsId"] = behandlingsId.toString()
                               }
            )
        }
    }
}