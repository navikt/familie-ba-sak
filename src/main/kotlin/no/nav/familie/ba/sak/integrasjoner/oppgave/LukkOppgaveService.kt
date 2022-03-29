package no.nav.familie.ba.sak.integrasjoner.oppgave

import no.nav.familie.ba.sak.ekstern.restDomene.RestLukkOppgaveKnyttJournalpost
import no.nav.familie.ba.sak.integrasjoner.journalføring.JournalføringService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LukkOppgaveService(
    private var oppgaveService: OppgaveService,
    private var journalføringService: JournalføringService
) {

    @Transactional
    fun lukkOppgaveOgKnyttJournalpostTilBehandling(oppgaveId: Long, request: RestLukkOppgaveKnyttJournalpost): String? {
        val oppgave = oppgaveService.hentOppgave(oppgaveId)

        val fagsakId = if (request.knyttJournalpostTilFagsak) {
            journalføringService.knyttJournalpostTilFagsak(request)
        } else ""

        oppgaveService.ferdigstillOppgave(oppgave)

        return fagsakId
    }
}
