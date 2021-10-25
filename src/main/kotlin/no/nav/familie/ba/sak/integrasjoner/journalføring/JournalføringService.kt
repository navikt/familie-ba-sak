package no.nav.familie.ba.sak.integrasjoner.journalføring

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
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
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.eksterne.kontrakter.Kategori
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.getDataOrThrow
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
class JournalføringService(
    private val integrasjonClient: IntegrasjonClient,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService,
    private val journalføringRepository: JournalføringRepository,
    private val loggService: LoggService,
    private val stegService: StegService,
    private val journalføringMetrikk: JournalføringMetrikk,
    private val featureToggleService: FeatureToggleService
) {

    fun hentDokument(journalpostId: String, dokumentInfoId: String): Ressurs<ByteArray> {
        return integrasjonClient.hentDokument(dokumentInfoId, journalpostId)
    }

    fun hentJournalpost(journalpostId: String): Ressurs<Journalpost> {
        return integrasjonClient.hentJournalpost(journalpostId)
    }

    fun hentJournalposterForBruker(brukerId: String): Ressurs<List<Journalpost>> {
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

    fun opprettBehandlingOgEvtFagsakForJournalføring(
        personIdent: String,
        navIdent: String,
        type: BehandlingType,
        årsak: BehandlingÅrsak,
        kategori: BehandlingKategori = BehandlingKategori.NASJONAL,
        underkategori: BehandlingUnderkategori
    ): Behandling {
        fagsakService.hentEllerOpprettFagsak(PersonIdent(personIdent))
        return stegService.håndterNyBehandling(
            NyBehandling(
                kategori = kategori,
                underkategori = underkategori,
                søkersIdent = personIdent,
                behandlingType = type,
                behandlingÅrsak = årsak,
                navIdent = navIdent,
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

        val nyBehandling: Behandling? = if (request.opprettOgKnyttTilNyBehandling) {

            val kategori = request.kategori ?: BehandlingKategori.NASJONAL
            if (kategori == BehandlingKategori.EØS && !featureToggleService.isEnabled(FeatureToggleConfig.KAN_BEHANDLE_EØS)) {
                throw FunksjonellFeil(
                    melding = "EØS er ikke påskrudd",
                    frontendFeilmelding = "Det er ikke støtte for å behandle EØS søknad."
                )
            }

            val underkategori = request.underkategori ?: request.hentUnderkategori()
            if (underkategori == BehandlingUnderkategori.UTVIDET && !featureToggleService.isEnabled(FeatureToggleConfig.KAN_BEHANDLE_UTVIDET)) {
                throw FunksjonellFeil(
                    melding = "Utvidet er ikke påskrudd",
                    frontendFeilmelding = "Det er ikke støtte for å behandle utvidet søknad og du må fjerne tilknytningen til behandling."
                )
            }

            val nyBehandling =
                opprettBehandlingOgEvtFagsakForJournalføring(
                    personIdent = request.bruker.id,
                    navIdent = request.navIdent,
                    type = request.nyBehandlingstype,
                    årsak = request.nyBehandlingsårsak,
                    kategori = kategori,
                    underkategori = underkategori
                )
            tilknyttedeBehandlingIder.add(nyBehandling.id.toString())
            nyBehandling
        } else null

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

        journalføringMetrikk.tellManuellJournalføringsmetrikker(journalpost.data, request, behandlinger)
        if (nyBehandling != null) {
            opprettOppgaveFor(nyBehandling, request.navIdent)
        }

        return sak.fagsakId ?: ""
    }

    fun lagreJournalpostOgKnyttFagsakTilJournalpost(
        tilknyttedeBehandlingIder: List<String>,
        journalpostId: String
    ): Pair<Sak, List<Behandling>> {

        val behandlinger = tilknyttedeBehandlingIder.map {
            behandlingService.hent(it.toLong())
        }

        val journalpost = hentJournalpost(journalpostId).getDataOrThrow()
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
            ?: hentJournalpost(journalpostId).data?.dokumenter?.first()?.dokumentInfoId
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
            integrasjonClient.oppdaterJournalpost(request, journalpostId)
            genererOgOpprettLogg(journalpostId, behandlinger)
            integrasjonClient.ferdigstillJournalpost(
                journalpostId = journalpostId,
                journalførendeEnhet = behandlendeEnhet
            )
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
        oppgaveService.opprettOppgave(
            behandlingId = behandling.id,
            oppgavetype = Oppgavetype.BehandleSak,
            fristForFerdigstillelse = LocalDate.now(),
            tilordnetNavIdent = navIdent
        )
    }

    private fun genererOgOpprettLogg(journalpostId: String, behandlinger: List<Behandling>) {
        val journalpost = hentJournalpost(journalpostId)
        val loggTekst = journalpost.data?.dokumenter?.fold("") { loggTekst, dokumentInfo ->
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

        val datoMottatt = journalpost.data?.datoMottatt ?: throw FunksjonellFeil(
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

        private val logger = LoggerFactory.getLogger(JournalføringService::class.java)
    }
}
