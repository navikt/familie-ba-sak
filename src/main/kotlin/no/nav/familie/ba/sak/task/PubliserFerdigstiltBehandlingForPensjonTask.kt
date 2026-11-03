package no.nav.familie.ba.sak.task

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.ekstern.pensjon.FerdigstiltBehandlingForPensjonRepository
import no.nav.familie.ba.sak.ekstern.pensjon.kafka.FerdigstiltBehandlingForPensjonPayload
import no.nav.familie.ba.sak.ekstern.pensjon.kafka.PensjonKafkaProducer
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = PubliserFerdigstiltBehandlingForPensjonTask.TASK_STEP_TYPE,
    beskrivelse = "Publiserer lagret ferdigstilt behandling til pensjon",
    maxAntallFeil = 3,
)
class PubliserFerdigstiltBehandlingForPensjonTask(
    private val ferdigstiltBehandlingForPensjonRepository: FerdigstiltBehandlingForPensjonRepository,
    private val pensjonKafkaProducer: PensjonKafkaProducer,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val sekvensnummer = task.payload.toLong()
        val ferdigstiltBehandlingForPensjon = ferdigstiltBehandlingForPensjonRepository.findByIdOrNull(sekvensnummer)
        if (ferdigstiltBehandlingForPensjon == null) {
            throw Feil("Fant ikke ferdigstilt behandling for pensjon med sekvensnummer=$sekvensnummer")
        }
        val payload = FerdigstiltBehandlingForPensjonPayload.fra(ferdigstiltBehandlingForPensjon)
        pensjonKafkaProducer.sendMeldingOmFerdigstiltBehandlingForPensjon(payload)
    }

    companion object {
        const val TASK_STEP_TYPE = "publiserFerdigstiltBehandlingForPensjonTask"

        fun opprettTask(sekvensnummer: Long): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = sekvensnummer.toString(),
            )
    }
}
