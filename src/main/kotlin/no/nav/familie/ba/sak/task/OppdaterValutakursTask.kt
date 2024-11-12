package no.nav.familie.ba.sak.task

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.AutomatiskOppdaterValutakursService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
@TaskStepBeskrivelse(
    taskStepType = OppdaterValutakursTask.TASK_STEP_TYPE,
    beskrivelse = "Oppdater valutakurs i behandling etter endringstidspunkt.",
    maxAntallFeil = 3,
)
class OppdaterValutakursTask(
    private val automatiskOppdaterValutakursService: AutomatiskOppdaterValutakursService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val dto: OppdaterValutakursTaskDTO = objectMapper.readValue(task.payload)

        val behandling = behandlingHentOgPersisterService.hent(dto.behandlingId)
        require(behandling.steg == StegType.BEHANDLINGSRESULTAT && behandling.aktiv && behandling.status == BehandlingStatus.UTREDES) {
            "Behandling ${dto.behandlingId} er ikke i behandlingresultatsteg."
        }

        automatiskOppdaterValutakursService.oppdaterValutakurserEtterEndringstidspunkt(
            behandlingId = BehandlingId(dto.behandlingId),
            endringstidspunkt = dto.endringstidspunkt,
        )
    }

    companion object {
        fun opprettTask(
            behandlingId: Long,
            endringstidspunkt: YearMonth,
        ): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(OppdaterValutakursTaskDTO(behandlingId, endringstidspunkt)),
            )

        const val TASK_STEP_TYPE = "oppdaterValutakurs"
    }

    private data class OppdaterValutakursTaskDTO(
        val behandlingId: Long,
        val endringstidspunkt: YearMonth = YearMonth.now(),
    )
}
