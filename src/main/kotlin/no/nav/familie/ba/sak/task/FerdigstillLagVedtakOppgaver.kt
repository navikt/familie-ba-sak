﻿package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties

data class FerdigstillLagVedtakOppgaverDTO(
    val behandlingId: Long,
)

@Service
@TaskStepBeskrivelse(
    taskStepType = FerdigstillLagVedtakOppgaver.TASK_STEP_TYPE,
    beskrivelse = "Ferdigstill oppgavene for å lage vedtak i GOSYS",
    maxAntallFeil = 3,
)
class FerdigstillLagVedtakOppgaver(
    private val oppgaveService: OppgaveService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val ferdigstillLagVedtakOppgaverDTO = objectMapper.readValue(task.payload, FerdigstillLagVedtakOppgaverDTO::class.java)
        oppgaveService.ferdigstillLagVedtakOppgaver(behandlingId = ferdigstillLagVedtakOppgaverDTO.behandlingId)
    }

    companion object {
        const val TASK_STEP_TYPE = "ferdigstillBehandleVedtakOppgaver"

        fun opprettTask(
            behandlingId: Long,
        ): Task {
            return Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(FerdigstillLagVedtakOppgaverDTO(behandlingId = behandlingId)),
                properties =
                    Properties().apply {
                        this["behandlingsId"] = behandlingId.toString()
                    },
            )
        }
    }
}
