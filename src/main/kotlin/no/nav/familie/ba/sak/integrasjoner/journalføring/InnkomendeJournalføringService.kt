package no.nav.familie.ba.sak.integrasjoner.journalføring

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.ekstern.restDomene.RestFerdigstillOppgaveKnyttJournalpost
import no.nav.familie.ba.sak.ekstern.restDomene.RestJournalføring
import no.nav.familie.ba.sak.ekstern.restDomene.RestOppdaterJournalpost
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.DbJournalpost
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.DbJournalpostType
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.FagsakSystem
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.JournalføringRepository
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.LogiskVedleggRequest
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.OppdaterJournalpostRequest
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.Sakstype.FAGSAK
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.Sakstype.GENERELL_SAK
import no.nav.familie.ba.sak.integrasjoner.pdl.secureLogger
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakEier
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.task.OpprettOppgaveTask
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus.FERDIGSTILT
import no.nav.familie.kontrakter.felles.journalpost.Sak
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import javax.transaction.Transactional

@Service
class InnkomendeJournalføringService(
    private val integrasjonClient: IntegrasjonClient,
    private val fagsakService: FagsakService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val journalføringRepository: JournalføringRepository,
    private val loggService: LoggService,
    private val stegService: StegService,
    private val journalføringMetrikk: JournalføringMetrikk,
) {

    fun hentDokument(journalpostId: String, dokumentInfoId: String): ByteArray {
        return integrasjonClient.hentDokument(dokumentInfoId, journalpostId)
    }

    fun hentJournalpost(journalpostId: String): Journalpost {
        return integrasjonClient.hentJournalpost(journalpostId)
    }

    fun hentJournalposterForBruker(brukerId: String): List<Journalpost> {
        return integrasjonClient.hentJournalposterForBruker(
            JournalposterForBrukerRequest(
                antall = 1000,
                brukerId = Bruker(id = brukerId, type = BrukerIdType.FNR),
                tema = listOf(Tema.BAR)
            )
        )
    }

    @Transactional
    fun ferdigstill(
        request: RestOppdaterJournalpost,
        journalpostId: String,
        behandlendeEnhet: String,
        oppgaveId: String
    ): String {

        val (sak, behandlinger) = lagreJournalpostOgKnyttFagsakTilJournalpost(
            request.tilknyttedeBehandlingIder,
            journalpostId
        )

        håndterLogiskeVedlegg(request, journalpostId)

        oppdaterOgFerdigstill(
            request = request.oppdaterMedDokumentOgSak(sak),
            journalpostId = journalpostId,
            behandlendeEnhet = behandlendeEnhet,
            oppgaveId = oppgaveId,
            behandlinger = behandlinger
        )

        when (val aktivBehandling = behandlinger.find { it.aktiv }) {
            null -> logger.info("Knytter til ${behandlinger.size} behandlinger som ikke er aktive")
            else -> opprettOppgaveFor(aktivBehandling, request.navIdent)
        }

        return sak.fagsakId ?: ""
    }

    private fun oppdaterLogiskeVedlegg(request: RestJournalføring) {
        request.dokumenter.forEach { dokument ->
            val fjernedeVedlegg = (dokument.eksisterendeLogiskeVedlegg ?: emptyList())
                .partition { (dokument.logiskeVedlegg ?: emptyList()).contains(it) }.second
            val nyeVedlegg = (dokument.logiskeVedlegg ?: emptyList()).partition {
                (dokument.eksisterendeLogiskeVedlegg ?: emptyList()).contains(it)
            }.second
            fjernedeVedlegg.forEach {
                integrasjonClient.slettLogiskVedlegg(it.logiskVedleggId, dokument.dokumentInfoId)
            }
            nyeVedlegg.forEach {
                integrasjonClient.leggTilLogiskVedlegg(LogiskVedleggRequest(it.tittel), dokument.dokumentInfoId)
            }
        }
    }

    private fun opprettBehandlingOgEvtFagsakForJournalføring(
        personIdent: String,
        navIdent: String,
        type: BehandlingType,
        årsak: BehandlingÅrsak,
        kategori: BehandlingKategori? = null,
        underkategori: BehandlingUnderkategori? = null,
        søknadMottattDato: LocalDate? = null,
        eier: FagsakEier = FagsakEier.OMSORGSPERSON
    ): Behandling {
        fagsakService.hentEllerOpprettFagsak(personIdent, eier = eier)
        return stegService.håndterNyBehandlingOgSendInfotrygdFeed(
            NyBehandling(
                kategori = kategori,
                underkategori = underkategori,
                søkersIdent = personIdent,
                behandlingType = type,
                behandlingÅrsak = årsak,
                navIdent = navIdent,
                søknadMottattDato = søknadMottattDato,
                fagsakEier = eier
            )
        )
    }

    @Transactional
    fun journalfør(
        request: RestJournalføring,
        journalpostId: String,
        behandlendeEnhet: String,
        oppgaveId: String
    ): String {

        val tilknyttedeBehandlingIder: MutableList<String> = request.tilknyttedeBehandlingIder.toMutableList()

        if (request.opprettOgKnyttTilNyBehandling) {
            val nyBehandling =
                opprettBehandlingOgEvtFagsakForJournalføring(
                    personIdent = request.bruker.id,
                    navIdent = request.navIdent,
                    type = request.nyBehandlingstype,
                    årsak = request.nyBehandlingsårsak,
                    kategori = request.kategori,
                    underkategori = request.underkategori,
                    søknadMottattDato = request.datoMottatt?.toLocalDate(),
                    eier = request.fagsakEier
                )
            tilknyttedeBehandlingIder.add(nyBehandling.id.toString())
        }

        val (sak, behandlinger) = lagreJournalpostOgKnyttFagsakTilJournalpost(tilknyttedeBehandlingIder, journalpostId)

        oppdaterLogiskeVedlegg(request)

        val journalpost = integrasjonClient.hentJournalpost(journalpostId)
        oppdaterOgFerdigstill(
            request = request.oppdaterMedDokumentOgSak(sak),
            journalpostId = journalpostId,
            behandlendeEnhet = behandlendeEnhet,
            oppgaveId = oppgaveId,
            behandlinger = behandlinger
        )

        journalføringMetrikk.tellManuellJournalføringsmetrikker(journalpost, request, behandlinger)
        return sak.fagsakId ?: ""
    }

    fun knyttJournalpostTilFagsakOgFerdigstillOppgave(
        request: RestFerdigstillOppgaveKnyttJournalpost,
        oppgaveId: Long
    ): String {
        val tilknyttedeBehandlingIder: MutableList<String> = request.tilknyttedeBehandlingIder.toMutableList()

        val journalpost = hentJournalpost(request.journalpostId)
        journalpost.sak?.fagsakId

        if (request.opprettOgKnyttTilNyBehandling) {
            val nyBehandling =
                opprettBehandlingOgEvtFagsakForJournalføring(
                    personIdent = request.bruker.id,
                    navIdent = request.navIdent,
                    type = request.nyBehandlingstype,
                    årsak = request.nyBehandlingsårsak,
                    kategori = request.kategori,
                    underkategori = request.underkategori,
                    søknadMottattDato = request.datoMottatt?.toLocalDate()
                )
            tilknyttedeBehandlingIder.add(nyBehandling.id.toString())
        }

        val (sak) = lagreJournalpostOgKnyttFagsakTilJournalpost(tilknyttedeBehandlingIder, journalpost.journalpostId)

        integrasjonClient.ferdigstillOppgave(oppgaveId = oppgaveId)

        return sak.fagsakId ?: ""
    }

    fun lagreJournalpostOgKnyttFagsakTilJournalpost(
        tilknyttedeBehandlingIder: List<String>,
        journalpostId: String
    ): Pair<Sak, List<Behandling>> {

        val behandlinger = tilknyttedeBehandlingIder.map {
            behandlingHentOgPersisterService.hent(it.toLong())
        }

        val journalpost = hentJournalpost(journalpostId)
        behandlinger.forEach {
            journalføringRepository.save(
                DbJournalpost(
                    behandling = it,
                    journalpostId = journalpostId,
                    type = DbJournalpostType.valueOf(journalpost.journalposttype.name)
                )
            )
        }

        val fagsak = when (tilknyttedeBehandlingIder.isNotEmpty()) {
            true -> {
                behandlinger.map { it.fagsak }.toSet().firstOrNull()
                    ?: throw FunksjonellFeil(
                        melding = "Behandlings'idene tilhørerer ikke samme fagsak, eller vi fant ikke fagsaken.",
                        frontendFeilmelding = "Oppslag på fagsak feilet med behandlingene som ble sendt inn."
                    )
            }
            false -> null
        }

        val sak = Sak(
            fagsakId = fagsak?.id?.toString(),
            fagsaksystem = fagsak?.let { FagsakSystem.BA.name },
            sakstype = fagsak?.let { FAGSAK.type } ?: GENERELL_SAK.type,
            arkivsaksystem = null,
            arkivsaksnummer = null
        )

        return Pair(sak, behandlinger)
    }

    private fun håndterLogiskeVedlegg(request: RestOppdaterJournalpost, journalpostId: String) {
        val fjernedeVedlegg =
            request.eksisterendeLogiskeVedlegg.partition { request.logiskeVedlegg.contains(it) }.second
        val nyeVedlegg = request.logiskeVedlegg.partition { request.eksisterendeLogiskeVedlegg.contains(it) }.second

        val dokumentInfoId = request.dokumentInfoId.takeIf { it.isNotEmpty() }
            ?: hentJournalpost(journalpostId).dokumenter?.first()?.dokumentInfoId
            ?: error("Fant ikke dokumentInfoId på journalpost")

        fjernedeVedlegg.forEach {
            integrasjonClient.slettLogiskVedlegg(it.logiskVedleggId, dokumentInfoId)
        }
        nyeVedlegg.forEach {
            integrasjonClient.leggTilLogiskVedlegg(LogiskVedleggRequest(it.tittel), dokumentInfoId)
        }
    }

    private fun oppdaterOgFerdigstill(
        request: OppdaterJournalpostRequest,
        journalpostId: String,
        behandlendeEnhet: String,
        oppgaveId: String,
        behandlinger: List<Behandling>
    ) {
        runCatching {
            secureLogger.info("Oppdaterer journalpost $journalpostId med $request")
            integrasjonClient.oppdaterJournalpost(request, journalpostId)
            genererOgOpprettLogg(journalpostId, behandlinger)
            secureLogger.info("Ferdigstiller journalpost $journalpostId")
            integrasjonClient.ferdigstillJournalpost(
                journalpostId = journalpostId,
                journalførendeEnhet = behandlendeEnhet
            )
            integrasjonClient.ferdigstillOppgave(oppgaveId = oppgaveId.toLong())
        }.onFailure {
            hentJournalpost(journalpostId).journalstatus.apply {
                if (this == FERDIGSTILT) {
                    integrasjonClient.ferdigstillOppgave(oppgaveId = oppgaveId.toLong())
                } else {
                    throw it
                }
            }
        }
    }

    private fun opprettOppgaveFor(behandling: Behandling, navIdent: String) {
        OpprettOppgaveTask.opprettTask(
            behandlingId = behandling.id,
            oppgavetype = Oppgavetype.BehandleSak,
            fristForFerdigstillelse = LocalDate.now(),
            tilordnetRessurs = navIdent
        )
    }

    private fun genererOgOpprettLogg(journalpostId: String, behandlinger: List<Behandling>) {
        val journalpost = hentJournalpost(journalpostId)
        val loggTekst = journalpost.dokumenter?.fold("") { loggTekst, dokumentInfo ->
            loggTekst +
                "${dokumentInfo.tittel}" +
                dokumentInfo.logiskeVedlegg?.fold("") { logiskeVedleggTekst, logiskVedlegg ->
                    logiskeVedleggTekst +
                        "\n\u2002\u2002${logiskVedlegg.tittel}"
                } + "\n"
        } ?: throw FunksjonellFeil(
            "Fant ingen dokumenter",
            frontendFeilmelding = "Noe gikk galt. Prøv igjen eller kontakt brukerstøtte hvis problemet vedvarer."
        )

        val datoMottatt = journalpost.datoMottatt ?: throw FunksjonellFeil(
            "Fant ingen dokumenter",
            frontendFeilmelding = "Noe gikk galt. Prøv igjen eller kontakt brukerstøtte hvis problemet vedvarer."
        )
        behandlinger.forEach {
            loggService.opprettMottattDokument(
                behandling = it,
                tekst = loggTekst,
                mottattDato = datoMottatt
            )
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(InnkomendeJournalføringService::class.java)
    }
}

private val RestJournalføring.fagsakEier: FagsakEier
    get() = if (erEnsligMindreårig || erPåInstitusjon) FagsakEier.BARN else FagsakEier.OMSORGSPERSON
