package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.dokument.DokumentService
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.BrevType
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.EnkelBrevtype
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.Vedtaksbrevtype
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.task.DistribuerDokumentTask.Companion.TASK_STEP_TYPE
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.*

@Service
@TaskStepBeskrivelse(taskStepType = TASK_STEP_TYPE, beskrivelse = "Send vedtaksbrev til Dokdist", maxAntallFeil = 3)
class DistribuerDokumentTask(
        private val stegService: StegService,
        private val behandlingService: BehandlingService,
        private val dokumentService: DokumentService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val distribuerDokumentDTO = objectMapper.readValue(task.payload, DistribuerDokumentDTO::class.java)

        when (distribuerDokumentDTO.brevType) {
            is Vedtaksbrevtype -> {
                stegService.håndterDistribuerVedtaksbrev(behandling = behandlingService.hent(distribuerDokumentDTO.behandlingId),
                                                         distribuerDokumentDTO = distribuerDokumentDTO)
            }
            is EnkelBrevtype -> {
                dokumentService.distribuerBrevOgLoggHendelse(journalpostId = distribuerDokumentDTO.journalpostId,
                                                             behandlingId = distribuerDokumentDTO.behandlingId,
                                                             loggBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                                                             brevType = distribuerDokumentDTO.brevType)
            }
            else -> {
                throw Feil("Klarte ikke identifisere brevtype ${distribuerDokumentDTO.brevType.visningsTekst} ved DistribuerDokumentTask")
            }
        }
    }

    companion object {

        fun opprettDistribuerDokumentTask(distribuerDokumentDTO: DistribuerDokumentDTO,
                                          properties: Properties): Task {
            return Task.nyTask(
                    type = TASK_STEP_TYPE,
                    payload = objectMapper.writeValueAsString(distribuerDokumentDTO),
                    properties = properties,
            ).copy(
                    triggerTid = nesteGyldigeTriggertidForBehandlingIHverdager()
            )
        }

        const val TASK_STEP_TYPE = "distribuerVedtaksbrev"
    }
}

data class DistribuerDokumentDTO(
        val behandlingId: Long,
        val journalpostId: String,
        val personIdent: String,
        val brevType: BrevType,
)
