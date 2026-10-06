package no.nav.familie.ba.sak.task

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.ekstern.pensjon.kafka.FerdigstilBehandlingHendelse
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
    beskrivelse = "Sender melding of ferdigstil behandling til pensjon",
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

        val vedtaktidspunkt = vedtakService.hentAktivForBehandlingThrows(dto.behandlingsId).vedtaksdato ?: throw Feil("Fant ikke vedtaksdato for vedtak for behandling=${dto.behandlingsId}")
        val endringstidspunkt = vedtaksperiodeService.finnEndringstidspunktForBehandling(dto.behandlingsId)

        val hendelse = FerdigstilBehandlingHendelse(dto.personIdent, vedtaktidspunkt, endringstidspunkt)

        pensjonKafkaProducer.sendMeldingOmFerdigstiltBehandlingTilPensjon(hendelse)
    }

    companion object {
        const val TASK_STEP_TYPE = "sendMeldingOmFerdigstiltBehandlingTilPensjonTask"

        fun opprettTask(
            personIdent: String,
            behandlingsId: Long,
        ): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload =
                    jsonMapper.writeValueAsString(
                        SendMeldingOmFerdigstiltBehandlingTilPensjonDTO(
                            personIdent = personIdent,
                            behandlingsId = behandlingsId,
                        ),
                    ),
                properties =
                    Properties().apply {
                        this["personIdent"] = personIdent
                        this["behandlingId"] = behandlingsId.toString()
                    },
            )
    }
}
