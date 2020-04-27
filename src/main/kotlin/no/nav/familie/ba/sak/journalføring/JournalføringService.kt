package no.nav.familie.ba.sak.journalføring

import no.nav.familie.ba.sak.behandling.NyBehandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.journalføring.domene.OppdaterJournalpostRequest
import no.nav.familie.ba.sak.journalføring.domene.Sak
import no.nav.familie.ba.sak.journalføring.domene.Sakstype.*
import no.nav.familie.ba.sak.oppgave.OppgaveService
import no.nav.familie.ba.sak.oppgave.OppgaveService.Behandlingstema.*
import no.nav.familie.kontrakter.felles.Ressurs

import org.springframework.stereotype.Service

@Service
class JournalføringService(private val integrasjonClient: IntegrasjonClient,
                           private val fagsakService: FagsakService,
                           private val stegService: StegService,
                           private val oppgaveService: OppgaveService) {

    fun hentDokument(journalpostId: String, dokumentInfoId: String): ByteArray {
        return integrasjonClient.hentDokument(dokumentInfoId, journalpostId)
    }

    fun hentJournalpost(journalpostId: String): Ressurs<no.nav.familie.ba.sak.integrasjoner.domene.Journalpost> {
        return integrasjonClient.hentJournalpost(journalpostId)
    }

    fun ferdigstill(request: OppdaterJournalpostRequest, journalpostId: String, behandlendeEnhet: String, oppgaveId: String): String {
        val sak = when (request.knyttTilFagsak) {
            false -> Sak(sakstype = GENERELL_SAK.type)
            true -> {
                val fagsakId = request.sak?.fagsakId ?: fagsakService.hentEllerOpprettFagsakForPersonIdent(request.bruker.id).id.toString()
                Sak(fagsakId = fagsakId, fagsaksystem = "BA", sakstype = FAGSAK.type)
            }
        }

        oppdaterOgFerdigstillJournalpostPlusOppgave(request.copy(sak = sak) , journalpostId, behandlendeEnhet, oppgaveId)

        if (sak.sakstype == FAGSAK.type) {
            oppgaveService.opprettOppgaveForNyBehandling(opprettNyBehandling(request, journalpostId), enhetsId = request.tildeltEnhetsnr)
        }

        return sak.fagsakId ?: ""
    }

    private fun oppdaterOgFerdigstillJournalpostPlusOppgave(request: OppdaterJournalpostRequest, journalpostId: String, behandlendeEnhet: String, oppgaveId: String) {
        integrasjonClient.oppdaterJournalpost(request, journalpostId)
        integrasjonClient.ferdigstillJournalpost(journalpostId = journalpostId, journalførendeEnhet = behandlendeEnhet)
        integrasjonClient.ferdigstillOppgave(oppgaveId = oppgaveId.toLong())
    }

    private fun opprettNyBehandling(request: OppdaterJournalpostRequest, journalpostId: String): Long {
        val kategori = when (request.behandlingstema) { BARNETRYGD_EØS.kode -> BehandlingKategori.EØS
                                                                       else -> BehandlingKategori.NASJONAL }

        val underkategori = when (request.behandlingstema) { UTVIDET_BARNETRYGD.kode -> BehandlingUnderkategori.UTVIDET
                                                                                else -> BehandlingUnderkategori.ORDINÆR }

        val nyBehandling = NyBehandling(kategori,
                                        underkategori,
                                        request.bruker.id,
                                        BehandlingType.FØRSTEGANGSBEHANDLING,
                                        journalpostId)

        return stegService.håndterNyBehandling(nyBehandling).id
    }
}