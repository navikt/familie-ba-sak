package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.brev.DokumentDistribueringService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = ForvaltningRedistribuerBrevTask.TASK_STEP_TYPE,
    beskrivelse = "Forvaltning: Redistribuer brev",
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
)
@Deprecated("Skal kun brukes for å patche feil fra dok")
class ForvaltningRedistribuerBrevTask(
    private val dokumentDistribueringService: DokumentDistribueringService,
) : AsyncTaskStep {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        val distribuerDokumentDTO = objectMapper.readValue(task.payload, DistribuerDokumentDTO::class.java)
        logger.info("distribuerDokumentFraTaskForFerdigstiltBehandling: task: ${task.id}, distribuerDokumentDTO=$distribuerDokumentDTO")
        dokumentDistribueringService.prøvDistribuerBrevOgLoggHendelseFraBehandling(distribuerDokumentDTO = distribuerDokumentDTO, loggBehandlerRolle = BehandlerRolle.SYSTEM)
    }

    companion object {
        const val TASK_STEP_TYPE = "forvaltningRedistribuerBrevTask"

        fun opprettTask(
            distribuerDokumentDTO: DistribuerDokumentDTO,
        ): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(distribuerDokumentDTO),
            )
    }
}
