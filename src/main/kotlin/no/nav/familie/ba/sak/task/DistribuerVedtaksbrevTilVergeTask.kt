package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.brev.DokumentDistribueringService
import no.nav.familie.ba.sak.kjerne.brev.hentBrevmal
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = DistribuerVedtaksbrevTilVergeTask.TASK_STEP_TYPE,
    beskrivelse = "Send vedtaksbrev til verge til Dokdist",
    maxAntallFeil = 3
)
class DistribuerVedtaksbrevTilVergeTask(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val dokumentDistribueringService: DokumentDistribueringService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val distribuerVedtaksbrevDTO =
            objectMapper.readValue(task.payload, DistribuerVedtaksbrevTilVergeDTO::class.java)

        val behandling = behandlingHentOgPersisterService.hent(distribuerVedtaksbrevDTO.behandlingId)

        dokumentDistribueringService.prøvDistribuerBrevOgLoggHendelse(
            distribuerDokumentDTO = DistribuerDokumentDTO(
                journalpostId = distribuerVedtaksbrevDTO.journalpostId,
                personEllerInstitusjonIdent = distribuerVedtaksbrevDTO.personIdent,
                behandlingId = distribuerVedtaksbrevDTO.behandlingId,
                brevmal = hentBrevmal(behandling),
                erManueltSendt = false
            ),
            loggBehandlerRolle = BehandlerRolle.SYSTEM
        )
    }

    companion object {

        fun opprettDistribuerVedtaksbrevTilVergeTask(
            distribuerVedtaksbrevTilVergeDTO: DistribuerVedtaksbrevTilVergeDTO,
            properties: Properties
        ): Task {
            return Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(distribuerVedtaksbrevTilVergeDTO),
                properties = properties
            ).copy(
                triggerTid = nesteGyldigeTriggertidForBehandlingIHverdager()
            )
        }

        const val TASK_STEP_TYPE = "distribuerVedtaksbrevTilVerge"
    }
}

data class DistribuerVedtaksbrevTilVergeDTO(
    val behandlingId: Long,
    val journalpostId: String,
    val personIdent: String
)
