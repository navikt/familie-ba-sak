package no.nav.familie.ba.sak.journalføring

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.restDomene.RestOppdaterJournalpost
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.journalføring.domene.*
import no.nav.familie.ba.sak.journalføring.domene.Sakstype.FAGSAK
import no.nav.familie.ba.sak.journalføring.domene.Sakstype.GENERELL_SAK
import no.nav.familie.ba.sak.oppgave.OppgaveService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.journalpost.DokumentInfo
import no.nav.familie.kontrakter.felles.journalpost.Dokumentstatus
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus.FERDIGSTILT
import no.nav.familie.kontrakter.felles.journalpost.Sak
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class JournalføringService(private val integrasjonClient: IntegrasjonClient,
                           private val behandlingService: BehandlingService,
                           private val oppgaveService: OppgaveService,
                           private val journalføringRepository: JournalføringRepository) {

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

        val (sak, behandlinger) = knyttJournalpostTilBehandlingOgFagsakTilJournalpost(request.tilknyttedeBehandlingIder, journalpostId)

        håndterLogiskeVedlegg(request, journalpostId)

        oppdaterOgFerdigstill(request = request.oppdaterMedDokumentOgSak(sak),
                              journalpostId = journalpostId,
                              behandlendeEnhet = behandlendeEnhet,
                              oppgaveId = oppgaveId)

        when (val aktivBehandling = behandlinger.find { it.aktiv }) {
            null -> LOG.info("Knytter til ${behandlinger.size} behandlinger som ikke er aktive")
            else -> opprettOppgaveFor(aktivBehandling, request.navIdent)
        }

        return sak.fagsakId ?: ""
    }

    fun knyttJournalpostTilBehandlingOgFagsakTilJournalpost(tilknyttedeBehandlingIder: List<String>, journalpostId: String): Pair<Sak, List<Behandling>> {

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

        val dokumentInfoId = request.dokumentInfoId.takeIf { !it.isEmpty() }
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
                                      oppgaveId: String) {
        runCatching {
            integrasjonClient.oppdaterJournalpost(request, journalpostId)
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

    private fun mapTilOppdaterJournalpostRequest(restOppdaterJournalpost: RestOppdaterJournalpost,
                                                 sak: Sak): OppdaterJournalpostRequest {
        val dokument = DokumentInfo(dokumentInfoId = restOppdaterJournalpost.dokumentInfoId,
                                    tittel = restOppdaterJournalpost.dokumentTittel,
                                    brevkode = null,
                                    dokumentstatus = Dokumentstatus.FERDIGSTILT,
                                    dokumentvarianter = null,
                                    logiskeVedlegg = null)

        return OppdaterJournalpostRequest(avsenderMottaker = AvsenderMottaker(restOppdaterJournalpost.avsender.id,
                                                                              navn = restOppdaterJournalpost.avsender.navn),
                                          bruker = Bruker(restOppdaterJournalpost.bruker.id,
                                                          navn = restOppdaterJournalpost.bruker.navn),
                                          sak = sak,
                                          tittel = restOppdaterJournalpost.dokumentTittel,
                                          dokumenter = listOf(dokument))
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