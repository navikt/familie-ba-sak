package no.nav.familie.ba.sak.kjerne.porteføljejustering

import com.fasterxml.jackson.module.kotlin.readValue
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.internal.BehandlesAvApplikasjon
import no.nav.familie.ba.sak.internal.ForvalterController
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.collections.contains

@Service
@TaskStepBeskrivelse(
    taskStepType = StartPorteføljejusteringTask.TASK_STEP_TYPE,
    beskrivelse = "Finne oppgaver som skal flyttes og opprette tasks for flytting",
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
)
class StartPorteføljejusteringTask(
    private val integrasjonKlient: IntegrasjonKlient,
    private val taskService: TaskService,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val startPorteføljejusteringTaskDto: StartPorteføljejusteringTaskDto = objectMapper.readValue(task.payload)
        val oppgaverISteinkjer =
            integrasjonKlient
                .hentOppgaver(
                    finnOppgaveRequest =
                        FinnOppgaveRequest(
                            tema = Tema.BAR,
                            enhet = BarnetrygdEnhet.STEINKJER.enhetsnummer,
                        ),
                ).oppgaver

        logger.info("Fant ${oppgaverISteinkjer.size} barnetrygd oppgaver i Steinkjer")

        val oppgaverSomSkalFlyttes =
            oppgaverISteinkjer
                .filterNot { it.saksreferanse?.matches("\\d+[A-Z]\\d+".toRegex()) == true } // Filtrere bort infotrygd-oppgaver
                .filterNot {
                    it.mappeId == null && it.oppgavetype !in
                        listOf(Oppgavetype.BehandleSak.value, Oppgavetype.BehandleSED.value, Oppgavetype.Journalføring.value)
                } // Vi skal ikke flytte oppgaver som ikke har mappe id med mindre det er av type BehandleSak, BehandleSed eller Journalføring.
                .filter { startPorteføljejusteringTaskDto.behandlesAvApplikasjon?.let { behandlesAvApplikasjon -> it.behandlesAvApplikasjon == behandlesAvApplikasjon.verdi } ?: true } // Filtrere på applikasjon hvis satt

        logger.info("Fant ${oppgaverSomSkalFlyttes.size} barnetrygd oppgaver som skal flyttes")

        val totalAntallOppgaverSomSkalFlyttes = oppgaverSomSkalFlyttes.size
        var opprettedeTasks = 0

        if (!startPorteføljejusteringTaskDto.dryRun) {
            oppgaverSomSkalFlyttes
                .take(startPorteføljejusteringTaskDto.antallTasks ?: oppgaverSomSkalFlyttes.size)
                .forEach { oppgave ->
                    oppgave.id?.let {
                        taskService.save(
                            PorteføljejusteringFlyttOppgaveTask.opprettTask(
                                oppgaveId = it,
                                enhetId = oppgave.tildeltEnhetsnr,
                                mappeId = oppgave.mappeId?.toString(),
                            ),
                        )
                        opprettedeTasks++
                    }
                }
        }

        logger.info("Antall oppgaver totalt:$totalAntallOppgaverSomSkalFlyttes, Antall tasks opprettet for flytting:$opprettedeTasks")
    }

    companion object {
        const val TASK_STEP_TYPE = "startPorteføljejusteringTask"
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)

        fun opprettTask(
            antallTasks: Int? = null,
            behandlesAvApplikasjon: BehandlesAvApplikasjon? = null,
            dryRun: Boolean = true,
        ): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(StartPorteføljejusteringTaskDto(antallTasks, behandlesAvApplikasjon, dryRun)),
            )
    }
}
