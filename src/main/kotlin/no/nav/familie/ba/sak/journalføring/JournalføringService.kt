package no.nav.familie.ba.sak.journalføring

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.restDomene.RestOppdaterJournalpost
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.assertGenerelleSuksessKriterier
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.journalføring.domene.*
import no.nav.familie.ba.sak.journalføring.domene.Sakstype.FAGSAK
import no.nav.familie.ba.sak.journalføring.domene.Sakstype.GENERELL_SAK
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.oppgave.OppgaveService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus.FERDIGSTILT
import no.nav.familie.kontrakter.felles.journalpost.Sak
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import javax.transaction.Transactional

@Service
class JournalføringService(private val integrasjonClient: IntegrasjonClient,
                           private val behandlingService: BehandlingService,
                           private val oppgaveService: OppgaveService,
                           private val journalføringRepository: JournalføringRepository,
                           private val loggService: LoggService) {

    fun hentDokument(journalpostId: String, dokumentInfoId: String): ByteArray {
        return integrasjonClient.hentDokument(dokumentInfoId, journalpostId)
    }

    fun hentJournalpost(journalpostId: String): Ressurs<Journalpost> {
        return integrasjonClient.hentJournalpost(journalpostId)
    }

    @Transactional
    fun ferdigstill(request: RestOppdaterJournalpost,
                    journalpostId: String,
                    behandlendeEnhet: String,
                    oppgaveId: String): String {

        val (sak, behandlinger) = lagreJournalpostOgKnyttFagsakTilJournalpost(request.tilknyttedeBehandlingIder, journalpostId)

        håndterLogiskeVedlegg(request, journalpostId)

        oppdaterOgFerdigstill(request = request.oppdaterMedDokumentOgSak(sak),
                              journalpostId = journalpostId,
                              behandlendeEnhet = behandlendeEnhet,
                              oppgaveId = oppgaveId,
                              behandlinger = behandlinger)

        when (val aktivBehandling = behandlinger.find { it.aktiv }) {
            null -> LOG.info("Knytter til ${behandlinger.size} behandlinger som ikke er aktive")
            else -> opprettOppgaveFor(aktivBehandling, request.navIdent)
        }

        return sak.fagsakId ?: ""
    }

    fun lagreJournalPost(behandling: Behandling,
                         journalpostId: String) = journalføringRepository.save(DbJournalpost(behandling = behandling,
                                                                                             journalpostId = journalpostId))

    fun lagreJournalpostOgKnyttFagsakTilJournalpost(tilknyttedeBehandlingIder: List<String>,
                                                    journalpostId: String): Pair<Sak, List<Behandling>> {

        val behandlinger = tilknyttedeBehandlingIder.map {
            behandlingService.hent(it.toLong())
        }

        behandlinger.forEach {
            journalføringRepository.save(DbJournalpost(behandling = it, journalpostId = journalpostId))
        }

        val fagsak = when (tilknyttedeBehandlingIder.isNotEmpty()) {
            true -> {
                val fagsaker = behandlinger.map { it.fagsak }.toSet()

                if (fagsaker.size != 1) {
                    throw Feil(message = "Behandlings'idene tilhørerer ikke samme fagsak, eller vi fant ikke fagsaken.",
                               frontendFeilmelding = "Oppslag på fagsak feilet med behandlingene som ble sendt inn.")
                }
                fagsaker.first()
            }
            false -> null
        }

        val sak = Sak(fagsakId = fagsak?.id?.toString(),
                      fagsaksystem = fagsak?.let { FagsakSystem.BA.name },
                      sakstype = fagsak?.let { FAGSAK.type } ?: GENERELL_SAK.type,
                      arkivsaksystem = null,
                      arkivsaksnummer = null)

        return Pair(sak, behandlinger)

    }

    private fun håndterLogiskeVedlegg(request: RestOppdaterJournalpost, journalpostId: String) {
        val fjernedeVedlegg = request.eksisterendeLogiskeVedlegg.partition { request.logiskeVedlegg.contains(it) }.second
        val nyeVedlegg = request.logiskeVedlegg.partition { request.eksisterendeLogiskeVedlegg.contains(it) }.second

        val dokumentInfoId = request.dokumentInfoId.takeIf { it.isNotEmpty() }
                             ?: hentJournalpost(journalpostId).data?.dokumenter?.first()?.dokumentInfoId
                             ?: error("Fant ikke dokumentInfoId på journalpost")

        fjernedeVedlegg.forEach {
            integrasjonClient.slettLogiskVedlegg(it.logiskVedleggId, dokumentInfoId)
        }
        nyeVedlegg.forEach {
            integrasjonClient.leggTilLogiskVedlegg(LogiskVedleggRequest(it.tittel), dokumentInfoId)
        }
    }

    private fun oppdaterOgFerdigstill(request: OppdaterJournalpostRequest,
                                      journalpostId: String,
                                      behandlendeEnhet: String,
                                      oppgaveId: String,
                                      behandlinger: List<Behandling>) {
        runCatching {
            integrasjonClient.oppdaterJournalpost(request, journalpostId)

            val journalpost = hentJournalpost(journalpostId)
            assertGenerelleSuksessKriterier(journalpost)
            val loggTekst = journalpost.data?.dokumenter?.fold("") { loggTekst, dokumentInfo ->
                loggTekst +
                "${dokumentInfo.tittel}" +
                dokumentInfo.logiskeVedlegg?.fold("") { logiskeVedleggTekst, logiskVedlegg ->
                    logiskeVedleggTekst +
                    "\n\u2002\u2002${logiskVedlegg.tittel}"
                } + "\n"
            } ?: throw FunksjonellFeil("Fant ingen dokumenter",
                                       frontendFeilmelding = "Noe gikk galt. Prøv igjen eller kontakt brukerstøtte hvis problemet vedvarer.")

            val datoMottatt = journalpost.data?.datoMottatt ?: throw FunksjonellFeil("Fant ingen dokumenter",
                                                                                     frontendFeilmelding = "Noe gikk galt. Prøv igjen eller kontakt brukerstøtte hvis problemet vedvarer.")
            behandlinger.forEach {
                loggService.opprettMottattDokument(behandling = it,
                                                   tekst = loggTekst,
                                                   mottattDato = datoMottatt)
            }

            integrasjonClient.ferdigstillJournalpost(journalpostId = journalpostId, journalførendeEnhet = behandlendeEnhet)
            integrasjonClient.ferdigstillOppgave(oppgaveId = oppgaveId.toLong())
        }.onFailure {
            hentJournalpost(journalpostId).data?.journalstatus.apply {
                if (this == FERDIGSTILT) {
                    integrasjonClient.ferdigstillOppgave(oppgaveId = oppgaveId.toLong())
                } else {
                    throw it
                }
            }
        }
    }

    private fun opprettOppgaveFor(behandling: Behandling, navIdent: String) {
        oppgaveService.opprettOppgave(behandlingId = behandling.id,
                                      oppgavetype = Oppgavetype.BehandleSak,
                                      fristForFerdigstillelse = LocalDate.now(),
                                      tilordnetNavIdent = navIdent)
    }

    companion object {

        private val LOG = LoggerFactory.getLogger(this::class.java)
    }
}