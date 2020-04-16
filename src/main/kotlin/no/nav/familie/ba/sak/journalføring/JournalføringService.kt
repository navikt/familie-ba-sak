package no.nav.familie.ba.sak.journalføring

import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.journalføring.domene.Journalpost
import no.nav.familie.ba.sak.journalføring.domene.OppdaterJournalpostRequest
import no.nav.familie.ba.sak.oppgave.OppgaveService

import org.springframework.stereotype.Service

@Service
class JournalføringService(private val integrasjonClient: IntegrasjonClient,
                           private val oppgaveService: OppgaveService) {

    fun hentDokument(journalpostId: String, dokumentInfoId: String): ByteArray {
        return integrasjonClient.hentDokument(dokumentInfoId, journalpostId)
    }

    fun hentJournalpost(journalpostId: String): Journalpost {
        return integrasjonClient.hentJournalpost(journalpostId)
    }

    fun ferdigstill(oppdaterJournalpostRequest: OppdaterJournalpostRequest, journalpostId: String, journalførendeEnhet: String, oppgaveId: String) {
        integrasjonClient.oppdaterJournalpost(oppdaterJournalpostRequest, journalpostId)
        integrasjonClient.ferdigstillJournalpost(journalpostId, journalførendeEnhet)
        //oppgaveService.ferdigstillOppgave(oppgaveId) //TODO
    }
}