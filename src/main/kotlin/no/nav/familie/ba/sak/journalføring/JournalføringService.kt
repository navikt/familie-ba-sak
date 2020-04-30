package no.nav.familie.ba.sak.journalføring

import no.nav.familie.ba.sak.behandling.NyBehandling
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.behandling.fagsak.Fagsak
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.journalføring.domene.AvsenderMottaker
import no.nav.familie.ba.sak.journalføring.domene.DokumentType
import no.nav.familie.ba.sak.journalføring.domene.LogiskVedleggRequest
import no.nav.familie.ba.sak.journalføring.domene.OppdaterJournalpostRequest
import no.nav.familie.ba.sak.journalføring.domene.Sakstype.FAGSAK
import no.nav.familie.ba.sak.journalføring.domene.Sakstype.GENERELL_SAK
import no.nav.familie.ba.sak.journalføring.restDomene.RestOppdaterJournalpost
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.oppgave.OppgaveService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.journalpost.*
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus.FERDIGSTILT
import no.nav.familie.kontrakter.felles.oppgave.*
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class JournalføringService(private val integrasjonClient: IntegrasjonClient,
                           private val fagsakService: FagsakService,
                           private val stegService: StegService,
                           private val loggService: LoggService,
                           private val oppgaveService: OppgaveService) {

    fun hentDokument(journalpostId: String, dokumentInfoId: String): ByteArray {
        return integrasjonClient.hentDokument(dokumentInfoId, journalpostId)
    }

    fun hentJournalpost(journalpostId: String): Ressurs<Journalpost> {
        return integrasjonClient.hentJournalpost(journalpostId)
    }

    fun ferdigstill(request: RestOppdaterJournalpost,
                    journalpostId: String,
                    behandlendeEnhet: String,
                    oppgaveId: String): String {

        val (fagsak, behandling) = when (request.knyttTilFagsak) {
            true -> {
                fagsakService.hentEllerOpprettFagsakForPersonIdent(request.bruker.id) to
                    runCatching { opprettNyBehandling(request.bruker.id, journalpostId) }.getOrNull()
            }
            else -> null to null
        }

        val sak = Sak(fagsakId = fagsak?.id.toString(),
                      fagsaksystem = fagsak?.let { "BA" },
                      sakstype = fagsak?.let { FAGSAK.type } ?: GENERELL_SAK.type,
                      arkivsaksystem = null,
                      arkivsaksnummer = null)

        håndterLogiskeVedlegg(request, journalpostId)

        oppdaterOgFerdigstill(request = mapTilOppdaterJournalpostRequest(request, sak),
                              journalpostId = journalpostId,
                              behandlendeEnhet = behandlendeEnhet,
                              oppgaveId = oppgaveId)

        if (fagsak != null) {
            when (behandling) {
                null -> opprettOppgaveUtenBehandling(fagsak, request, behandlendeEnhet)
                else -> {
                    loggService.opprettMottattDokument(behandling = behandling,
                                                       datoMottatt = request.datoMottatt,
                                                       dokumentType = DokumentType.SØKNAD)
                    opprettOppgaveFor(behandling, request.navIdent)
                }
            }
        }

        return sak.fagsakId ?: ""
    }

    private fun håndterLogiskeVedlegg(request: RestOppdaterJournalpost, journalpostId: String) {
        val fjernedeVedlegg = request.eksisterendeLogiskeVedlegg.partition { request.logiskeVedlegg.contains(it) }.second
        val nyeVedlegg = request.logiskeVedlegg.partition { request.eksisterendeLogiskeVedlegg.contains(it) }.second

        val dokumentInfoId = hentJournalpost(journalpostId).data?.dokumenter?.first()?.dokumentInfoId ?:
            error("Fant ikke dokumentInfoId på journalpost") // TODO Frontend kan sende med dokumentInfoId'en i requesten

        fjernedeVedlegg.forEach {
            integrasjonClient.slettLogiskVedlegg(it.logiskVedleggId, dokumentInfoId)
        }
        nyeVedlegg.forEach {
            integrasjonClient.leggTilLogiskVedlegg(LogiskVedleggRequest(it.tittel), dokumentInfoId)
        }
    }

    private fun opprettNyBehandling(søkersIdent: String, journalpostId: String): Behandling {
        val nyBehandling = NyBehandling(søkersIdent = søkersIdent,
                                        behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                                        kategori = BehandlingKategori.NASJONAL,
                                        underkategori = BehandlingUnderkategori.ORDINÆR,
                                        journalpostID = journalpostId)
        return stegService.håndterNyBehandling(nyBehandling)
    }

    private fun oppdaterOgFerdigstill(request: OppdaterJournalpostRequest,
                                      journalpostId: String,
                                      behandlendeEnhet: String,
                                      oppgaveId: String) {
        runCatching {
            integrasjonClient.oppdaterJournalpost(request, journalpostId)
            integrasjonClient.ferdigstillJournalpost(journalpostId = journalpostId, journalførendeEnhet = behandlendeEnhet)
            integrasjonClient.ferdigstillOppgave(oppgaveId = oppgaveId.toLong())
        }.onFailure {
            hentJournalpost(journalpostId).data?.journalstatus.apply {
                if (this == FERDIGSTILT) {
                    integrasjonClient.ferdigstillOppgave(oppgaveId = oppgaveId.toLong())
                }
            }
        }
    }

    private fun mapTilOppdaterJournalpostRequest(rest: RestOppdaterJournalpost, sak: Sak): OppdaterJournalpostRequest {
        return OppdaterJournalpostRequest(avsenderMottaker = AvsenderMottaker(rest.avsender.id, navn = rest.avsender.navn),
                                          bruker = Bruker(rest.bruker.id, BrukerIdType.FNR),
                                          sak = sak,
                                          dokumentType = rest.dokumenttype,
                                          datoMottatt = rest.datoMottatt)
    }

    private fun opprettOppgaveFor(behandling: Behandling, navIdent: String) {
        oppgaveService.opprettOppgave(behandlingId = behandling.id,
                                      oppgavetype = Oppgavetype.BehandleSak,
                                      fristForFerdigstillelse = LocalDate.now(),
                                      tilordnetNavIdent = navIdent)
    }

    private fun opprettOppgaveUtenBehandling(fagsak: Fagsak, request: RestOppdaterJournalpost, behandlendeEnhet: String) {
        oppgaveService.opprettOppgave(OpprettOppgave(ident = OppgaveIdent(ident = request.bruker.id, type = IdentType.Aktør),
                                                     saksId = fagsak.id.toString(),
                                                     tema = Tema.BAR,
                                                     oppgavetype = Oppgavetype.BehandleSak,
                                                     fristFerdigstillelse = LocalDate.now(),
                                                     beskrivelse = oppgaveService.lagOppgaveTekst(fagsak.id),
                                                     enhetsnummer = behandlendeEnhet,
                                                     behandlingstema = OppgaveService.Behandlingstema.ORDINÆR_BARNETRYGD.kode,
                                                     tilordnetRessurs = request.navIdent))
    }
}