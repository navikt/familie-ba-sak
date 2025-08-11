package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet
import no.nav.familie.ba.sak.task.PorteføljejusteringTask.Companion.TASK_STEP_TYPE
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory

@TaskStepBeskrivelse(taskStepType = TASK_STEP_TYPE, beskrivelse = "Finner oppgaver som skal flyttes til ny enhet og oppretter tasker for å oppdatere enhet")
class PorteføljejusteringTask(
    private val oppgaveService: OppgaveService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val finnOppgaveRequest =
            FinnOppgaveRequest(
                tema = Tema.BAR,
                enhet = BarnetrygdEnhet.STEINKJER.enhetsnummer,
                offset = 0,
                limit = task.payload.toLong(),
            )
        val finnOppgaveResponseDto: FinnOppgaveResponseDto = oppgaveService.hentOppgaver(finnOppgaveRequest)
        val grupperteOppgaver = grupperOppgaverEtterBehandlesAvApplikasjonOgOppgavetype(finnOppgaveResponseDto.oppgaver)
        secureLogger.info(objectMapper.writeValueAsString(grupperteOppgaver))
    }

    private fun grupperOppgaverEtterBehandlesAvApplikasjonOgOppgavetype(
        oppgaver: List<Oppgave>,
    ): Map<String, Map<String?, Int>> = oppgaver.groupBy { oppgave -> oppgave.behandlesAvApplikasjon ?: "behandlesAvApplikasjonIkkeSatt" }.mapValues { (_, oppgaver) -> oppgaver.groupBy { oppgave -> oppgave.oppgavetype }.mapValues { (_, oppgaver) -> oppgaver.size } }

    companion object {
        val logger = LoggerFactory.getLogger(PorteføljejusteringTask::class.java)
        const val TASK_STEP_TYPE = "porteføljejusteringTask"

        fun opprettTask(antallOppgaver: Long): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = antallOppgaver.toString(),
            )
    }
}
