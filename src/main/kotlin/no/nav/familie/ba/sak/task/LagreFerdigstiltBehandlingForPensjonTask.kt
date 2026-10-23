package no.nav.familie.ba.sak.task

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.ekstern.pensjon.FerdigstiltBehandlingForPensjon
import no.nav.familie.ba.sak.ekstern.pensjon.FerdigstiltBehandlingForPensjonRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.task.dto.LagreFerdigstiltBehandlingForPensjonDTO
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = LagreFerdigstiltBehandlingForPensjonTask.TASK_STEP_TYPE,
    beskrivelse = "Lagrer data om en ferdigstilt behandling for pensjon slik at det kan senere sendes på kafka",
    maxAntallFeil = 3,
)
class LagreFerdigstiltBehandlingForPensjonTask(
    private val vedtakService: VedtakService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val ferdigstiltBehandlingForPensjonRepository: FerdigstiltBehandlingForPensjonRepository,
    private val taskRepository: TaskRepositoryWrapper,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val dto = jsonMapper.readValue(task.payload, LagreFerdigstiltBehandlingForPensjonDTO::class.java)

        ferdigstiltBehandlingForPensjonRepository.låsTabell()

        val eksisterendeFerdigstiltBehandling = ferdigstiltBehandlingForPensjonRepository.findByBehandlingId(dto.behandlingId)
        if (eksisterendeFerdigstiltBehandling != null) {
            throw Feil("Ferdigstilt behandling for pensjon er allerede lagret for behandling=${dto.behandlingId} med sekvensnummer=${eksisterendeFerdigstiltBehandling.sekvensnummer}")
        }

        val vedtaktidspunkt = vedtakService.hentAktivForBehandlingThrows(dto.behandlingId).vedtaksdato ?: throw Feil("Fant ikke vedtaksdato for vedtak for behandling=${dto.behandlingId}")
        val endringstidspunkt = vedtaksperiodeService.finnEndringstidspunktForBehandling(dto.behandlingId)

        val lagretFerdigstiltBehandling =
            ferdigstiltBehandlingForPensjonRepository.save(
                FerdigstiltBehandlingForPensjon(
                    sekvensnummer = ferdigstiltBehandlingForPensjonRepository.finnHøyesteSekvensnummer() + 1,
                    behandlingId = dto.behandlingId,
                    ident = dto.personIdent,
                    vedtaktidspunkt = vedtaktidspunkt,
                    endringstidspunkt = endringstidspunkt,
                ),
            )

        val publiserTask = PubliserFerdigstiltBehandlingForPensjonTask.opprettTask(lagretFerdigstiltBehandling.sekvensnummer)
        taskRepository.save(publiserTask)
    }

    companion object {
        const val TASK_STEP_TYPE = "lagreFerdigstiltBehandlingForPensjonTask"

        fun opprettTask(
            personIdent: String,
            behandlingId: Long,
        ): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload =
                    jsonMapper.writeValueAsString(
                        LagreFerdigstiltBehandlingForPensjonDTO(
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
