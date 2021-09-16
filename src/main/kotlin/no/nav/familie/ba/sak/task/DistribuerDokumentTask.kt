package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.dokument.DokumentService
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.task.DistribuerDokumentTask.Companion.TASK_STEP_TYPE
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
@TaskStepBeskrivelse(taskStepType = TASK_STEP_TYPE, beskrivelse = "Send dokument til Dokdist", maxAntallFeil = 3)
class DistribuerDokumentTask(
    private val stegService: StegService,
    private val behandlingService: BehandlingService,
    private val dokumentService: DokumentService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val distribuerDokumentDTO = objectMapper.readValue(task.payload, DistribuerDokumentDTO::class.java)

        if (distribuerDokumentDTO.erManueltSendt && !distribuerDokumentDTO.brevmal.erVedtaksbrev) {
            dokumentService.distribuerBrevOgLoggHendelse(
                journalpostId = distribuerDokumentDTO.journalpostId,
                behandlingId = distribuerDokumentDTO.behandlingId,
                loggBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                brevMal = distribuerDokumentDTO.brevmal
            )

        } else if (!distribuerDokumentDTO.erManueltSendt && distribuerDokumentDTO.brevmal.erVedtaksbrev && distribuerDokumentDTO.behandlingId != null) {
            stegService.håndterDistribuerVedtaksbrev(
                behandling = behandlingService.hent(distribuerDokumentDTO.behandlingId),
                distribuerDokumentDTO = distribuerDokumentDTO
            )
        } else {
            throw Feil("erManueltSendt=${distribuerDokumentDTO.erManueltSendt} ikke støttet for brev=${distribuerDokumentDTO.brevmal.visningsTekst}")
        }
    }

    companion object {

        fun opprettDistribuerDokumentTask(
            distribuerDokumentDTO: DistribuerDokumentDTO,
            properties: Properties,
            envService: EnvService?
        ): Task {
            return Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(distribuerDokumentDTO),
                properties = properties,
            ).copy(
                triggerTid = if (envService != null && envService.erE2E()) LocalDateTime.now() else nesteGyldigeTriggertidForBehandlingIHverdager()
            )
        }

        const val TASK_STEP_TYPE = "distribuerDokument"
    }
}

data class DistribuerDokumentDTO(
    val behandlingId: Long?,
    val journalpostId: String,
    val personIdent: String,
    val brevmal: Brevmal,
    val erManueltSendt: Boolean,
)
