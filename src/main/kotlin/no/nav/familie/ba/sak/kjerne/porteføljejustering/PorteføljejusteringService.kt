package no.nav.familie.ba.sak.kjerne.porteføljejustering

import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.StatusEnum
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.stereotype.Service

@Service
class PorteføljejusteringService(
    private val integrasjonKlient: IntegrasjonKlient,
    private val taskService: TaskService,
) {
    fun lagTaskForOverføringAvOppgaver() {
        val oppgaver =
            integrasjonKlient.hentOppgaver(finnOppgaveRequest = FinnOppgaveRequest(tema = Tema.BAR, enhet = BarnetrygdEnhet.STEINKJER.enhetsnummer)).oppgaver

        val filtrerteOppgaver =
            oppgaver
                .filterNot { it.saksreferanse?.matches("\\d+[A-Z]\\d+".toRegex()) == true } // Filtrere bort infotrygd-oppgaver
                .filterNot { it.status in setOf(StatusEnum.FERDIGSTILT, StatusEnum.FEILREGISTRERT) }

        filtrerteOppgaver.forEach { oppgave ->
            oppgave.id?.let {
                taskService.save(PorteføljejusteringTask.opprettTask(it))
            }
        }
    }
}
