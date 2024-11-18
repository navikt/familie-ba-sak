package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.brev.DokumentDistribueringService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = DistribuerVedtaksbrevTilFullmektigEllerVergeTask.TASK_STEP_TYPE,
    beskrivelse = "Send vedtaksbrev til institusjon verge eller manuell brev mottaker til Dokdist",
    maxAntallFeil = 3,
)
class DistribuerVedtaksbrevTilFullmektigEllerVergeTask(
    private val dokumentDistribueringService: DokumentDistribueringService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val distribuerDokumentDTO = objectMapper.readValue(task.payload, DistribuerDokumentDTO::class.java)
        dokumentDistribueringService.prøvDistribuerBrevOgLoggHendelseFraBehandling(
            distribuerDokumentDTO = distribuerDokumentDTO,
            loggBehandlerRolle = BehandlerRolle.SYSTEM,
        )
    }

    companion object {
        fun opprettDistribuerVedtaksbrevTilFullmektigEllerVergeTask(
            distribuerDokumentDTO: DistribuerDokumentDTO,
            properties: Properties,
        ): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(distribuerDokumentDTO),
                properties = properties,
            ).copy(
                triggerTid = utledNesteTriggerTidIHverdagerForTask(),
            )

        const val TASK_STEP_TYPE = "distribuerVedtaksbrevTilVergeEllerManuellBrevMottaker"
    }
}
