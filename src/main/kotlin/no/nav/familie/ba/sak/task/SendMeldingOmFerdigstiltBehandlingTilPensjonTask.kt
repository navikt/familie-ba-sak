package no.nav.familie.ba.sak.task

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.ekstern.pensjon.kafka.FerdigstiltBehandlingHendelse
import no.nav.familie.ba.sak.ekstern.pensjon.kafka.PensjonKafkaProducer
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.task.dto.SendMeldingOmFerdigstiltBehandlingTilPensjonDTO
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = SendMeldingOmFerdigstiltBehandlingTilPensjonTask.TASK_STEP_TYPE,
    beskrivelse = "Sender melding om ferdigstilt behandling til pensjon",
    maxAntallFeil = 3,
)
class SendMeldingOmFerdigstiltBehandlingTilPensjonTask(
    private val vedtakService: VedtakService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val pensjonKafkaProducer: PensjonKafkaProducer,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val dto = jsonMapper.readValue(task.payload, SendMeldingOmFerdigstiltBehandlingTilPensjonDTO::class.java)

        val vedtaktidspunkt = vedtakService.hentAktivForBehandlingThrows(dto.behandlingId).vedtaksdato ?: throw Feil("Fant ikke vedtaksdato for vedtak for behandling=${dto.behandlingId}")
        val endringstidspunkt = vedtaksperiodeService.finnEndringstidspunktForBehandling(dto.behandlingId)

        val hendelse = FerdigstiltBehandlingHendelse(dto.personIdent, vedtaktidspunkt, endringstidspunkt)

        pensjonKafkaProducer.sendMeldingOmFerdigstiltBehandlingTilPensjon(hendelse)
    }

    companion object {
        const val TASK_STEP_TYPE = "sendMeldingOmFerdigstiltBehandlingTilPensjonTask"

        fun opprettTask(
            personIdent: String,
            behandlingId: Long,
        ): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload =
                    jsonMapper.writeValueAsString(
                        SendMeldingOmFerdigstiltBehandlingTilPensjonDTO(
                            personIdent = personIdent,
                            behandlingId = behandlingId,
                        ),
                    ),
                properties =
                    Properties().apply {
                        this["personIdent"] = personIdent
                        this["behandlingId"] = behandlingId.toString()
                    },
            )
    }
}
