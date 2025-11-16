package no.nav.familie.ba.sak.kjerne.porteføljejustering

import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.collections.contains

@Service
class PorteføljejusteringService(
    private val integrasjonKlient: IntegrasjonKlient,
    private val taskService: TaskService,
) {
    fun lagTaskForOverføringAvOppgaverFraSteinkjer() {
        val oppgaverISteinkjer =
            integrasjonKlient
                .hentOppgaver(
                    finnOppgaveRequest =
                        FinnOppgaveRequest(
                            tema = Tema.BAR,
                            enhet = BarnetrygdEnhet.STEINKJER.enhetsnummer,
                        ),
                ).oppgaver

        logger.info("Fant ${oppgaverISteinkjer.size} oppgaver i Steinkjer")

        val oppgaverSomSkalFlyttes =
            oppgaverISteinkjer
                .filterNot { it.saksreferanse?.matches("\\d+[A-Z]\\d+".toRegex()) == true } // Filtrere bort infotrygd-oppgaver
                .filterNot {
                    it.mappeId == null && it.oppgavetype !in
                        listOf(Oppgavetype.BehandleSak.value, Oppgavetype.BehandleSED.value, Oppgavetype.Journalføring.value)
                } // Vi skal ikke flytte oppgaver som ikke har mappe id med mindre det er av type BehandleSak, BehandleSed eller Journalføring.

        oppgaverSomSkalFlyttes.forEach { oppgave ->
            oppgave.id?.let {
                taskService.save(PorteføljejusteringTask.opprettTask(it))
            }
        }

        logger.info("Oppretting av ${oppgaverSomSkalFlyttes.size} tasks for overføring av oppgave fullført.")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PorteføljejusteringService::class.java)
    }
}
