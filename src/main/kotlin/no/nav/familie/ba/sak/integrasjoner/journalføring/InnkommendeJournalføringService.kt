package no.nav.familie.ba.sak.integrasjoner.journalføring

import jakarta.transaction.Transactional
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.ekstern.restDomene.FerdigstillOppgaveKnyttJournalpostDto
import no.nav.familie.ba.sak.ekstern.restDomene.RestJournalføring
import no.nav.familie.ba.sak.ekstern.restDomene.TilknyttetBehandling
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.FagsakSystem
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.Journalføringsbehandlingstype
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.LogiskVedleggRequest
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.OppdaterJournalpostRequest
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.Sakstype.FAGSAK
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.Søknadsinfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.klage.KlageService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.kontrakter.ba.søknad.v4.Søknadstype
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus.FERDIGSTILT
import no.nav.familie.kontrakter.felles.journalpost.Sak
import no.nav.familie.kontrakter.felles.journalpost.TilgangsstyrtJournalpost
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class InnkommendeJournalføringService(
    private val integrasjonKlient: IntegrasjonKlient,
    private val fagsakService: FagsakService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val loggService: LoggService,
    private val stegService: StegService,
    private val journalføringMetrikk: JournalføringMetrikk,
    private val behandlingSøknadsinfoService: BehandlingSøknadsinfoService,
    private val klageService: KlageService,
) {
    fun hentDokument(
        journalpostId: String,
        dokumentInfoId: String,
    ): ByteArray = integrasjonKlient.hentDokument(dokumentInfoId, journalpostId)

    fun hentJournalpost(journalpostId: String): Journalpost = integrasjonKlient.hentJournalpost(journalpostId)

    fun hentJournalposterForBruker(brukerId: String): List<TilgangsstyrtJournalpost> =
        integrasjonKlient
            .hentTilgangsstyrteJournalposterForBruker(
                JournalposterForBrukerRequest(
                    antall = 1000,
                    brukerId = Bruker(id = brukerId, type = BrukerIdType.FNR),
                    tema = listOf(Tema.BAR),
                ),
            )

    private fun oppdaterLogiskeVedlegg(request: RestJournalføring) {
        request.dokumenter.forEach { dokument ->
            val fjernedeVedlegg =
                (dokument.eksisterendeLogiskeVedlegg ?: emptyList())
                    .partition { (dokument.logiskeVedlegg ?: emptyList()).contains(it) }
                    .second
            val nyeVedlegg =
                (dokument.logiskeVedlegg ?: emptyList())
                    .partition {
                        (dokument.eksisterendeLogiskeVedlegg ?: emptyList()).contains(it)
                    }.second
            fjernedeVedlegg.forEach {
                integrasjonKlient.slettLogiskVedlegg(it.logiskVedleggId, dokument.dokumentInfoId)
            }
            nyeVedlegg.forEach {
                integrasjonKlient.leggTilLogiskVedlegg(LogiskVedleggRequest(it.tittel), dokument.dokumentInfoId)
            }
        }
    }

    private fun opprettBehandlingForJournalføring(
        personIdent: String,
        navIdent: String,
        type: BehandlingType,
        årsak: BehandlingÅrsak,
        kategori: BehandlingKategori? = null,
        underkategori: BehandlingUnderkategori? = null,
        søknadMottattDato: LocalDate? = null,
        søknadsinfo: Søknadsinfo? = null,
        fagsakId: Long,
    ): Behandling =
        stegService.håndterNyBehandlingOgSendInfotrygdFeed(
            NyBehandling(
                kategori = kategori,
                underkategori = underkategori,
                søkersIdent = personIdent,
                behandlingType = type,
                behandlingÅrsak = årsak,
                navIdent = navIdent,
                søknadMottattDato = søknadMottattDato,
                søknadsinfo = søknadsinfo,
                fagsakId = fagsakId,
            ),
        )

    @Transactional
    fun journalfør(
        request: RestJournalføring,
        journalpostId: String,
        behandlendeEnhet: String,
        oppgaveId: String,
    ): String {
        val fagsak =
            fagsakService.hentEllerOpprettFagsak(
                personIdent = request.bruker.id,
                type = request.fagsakType,
                institusjon = request.institusjon,
            )

        val tilknyttedeBehandlinger: MutableList<TilknyttetBehandling> = request.tilknyttedeBehandlinger.toMutableList()
        val journalpost = integrasjonKlient.hentJournalpost(journalpostId)
        val brevkode = journalpost.dokumenter?.firstNotNullOfOrNull { it.brevkode }

        if (request.opprettOgKnyttTilNyBehandling) {
            if (request.nyBehandlingstype == Journalføringsbehandlingstype.KLAGE) {
                val klageMottattDato = request.datoMottatt?.toLocalDate() ?: throw Feil("Dato mottatt ikke satt ved journalføring for journalpostId $journalpostId og oppgaveId $oppgaveId. for fagsak ${fagsak.id}")
                val klagebehandlingId = klageService.opprettKlage(fagsak, klageMottattDato)
                tilknyttedeBehandlinger.add(TilknyttetBehandling(Journalføringsbehandlingstype.KLAGE, klagebehandlingId.toString()))
            } else {
                val nyBehandling =
                    opprettBehandlingForJournalføring(
                        personIdent = request.bruker.id,
                        navIdent = request.navIdent,
                        type = request.nyBehandlingstype.tilBehandingType(),
                        årsak = request.nyBehandlingsårsak,
                        kategori = request.kategori,
                        underkategori = request.underkategori,
                        søknadMottattDato = request.datoMottatt?.toLocalDate(),
                        søknadsinfo =
                            brevkode?.let {
                                Søknadsinfo(
                                    journalpostId = journalpost.journalpostId,
                                    brevkode = it,
                                    erDigital = journalpost.kanal == NAV_NO,
                                )
                            },
                        fagsakId = fagsak.id,
                    )
                tilknyttedeBehandlinger.add(TilknyttetBehandling(request.nyBehandlingstype, nyBehandling.id.toString()))
            }
        }

        val barnetrygdBehandlinger =
            tilknyttedeBehandlinger
                .filter { !it.behandlingstype.skalBehandlesIEksternApplikasjon() }
                .map { behandlingHentOgPersisterService.hent(it.behandlingId.toLong()) }

        val erSøknad = brevkode == Søknadstype.ORDINÆR.søknadskode || brevkode == Søknadstype.UTVIDET.søknadskode

        if (erSøknad && !request.opprettOgKnyttTilNyBehandling) {
            barnetrygdBehandlinger.forEach { tidligereBehandling ->
                lagreNedSøknadsinfoKnyttetTilBehandling(journalpost, brevkode!!, tidligereBehandling)
            }
        }

        oppdaterLogiskeVedlegg(request)

        val sak =
            Sak(
                fagsakId = fagsak.id.toString(),
                fagsaksystem = FagsakSystem.BA.name,
                sakstype = FAGSAK.type,
                arkivsaksystem = null,
                arkivsaksnummer = null,
            )

        oppdaterOgFerdigstill(
            request = request.oppdaterMedDokumentOgSak(sak, journalpost),
            journalpostId = journalpostId,
            behandlendeEnhet = behandlendeEnhet,
            oppgaveId = oppgaveId,
            barnetrygdBehandlinger = barnetrygdBehandlinger,
        )

        journalføringMetrikk.tellManuellJournalføringsmetrikker(request.journalpostTittel, tilknyttedeBehandlinger)

        return fagsak.id.toString()
    }

    fun knyttJournalpostTilFagsakOgFerdigstillOppgave(
        request: FerdigstillOppgaveKnyttJournalpostDto,
        oppgaveId: Long,
    ): String {
        val fagsak =
            fagsakService.hentEllerOpprettFagsak(
                personIdent = request.bruker.id,
            )
        val tilknyttedeBehandlinger: MutableList<TilknyttetBehandling> = request.tilknyttedeBehandlinger.toMutableList()
        val journalpost = hentJournalpost(request.journalpostId)

        if (request.opprettOgKnyttTilNyBehandling) {
            if (request.nyBehandlingstype == Journalføringsbehandlingstype.KLAGE) {
                val klageMottattDato = request.datoMottatt?.toLocalDate() ?: throw Feil("Dato mottatt ikke satt ved ferdigstilling av opppgave med oppgaveId $oppgaveId for fagsak ${fagsak.id}")
                val klagebehandlingId = klageService.opprettKlage(fagsak, klageMottattDato)
                tilknyttedeBehandlinger.add(TilknyttetBehandling(Journalføringsbehandlingstype.KLAGE, klagebehandlingId.toString()))
            } else {
                val brevkode = journalpost.dokumenter?.firstNotNullOfOrNull { it.brevkode }
                val nyBehandling =
                    opprettBehandlingForJournalføring(
                        personIdent = request.bruker.id,
                        navIdent = request.navIdent,
                        type = request.nyBehandlingstype.tilBehandingType(),
                        årsak = request.nyBehandlingsårsak,
                        kategori = request.kategori,
                        underkategori = request.underkategori,
                        søknadMottattDato = request.datoMottatt?.toLocalDate(),
                        søknadsinfo =
                            brevkode?.let {
                                Søknadsinfo(
                                    journalpostId = journalpost.journalpostId,
                                    brevkode = it,
                                    erDigital = journalpost.kanal == NAV_NO,
                                )
                            },
                        fagsakId = fagsak.id,
                    )
                tilknyttedeBehandlinger.add(TilknyttetBehandling(request.nyBehandlingstype, nyBehandling.id.toString()))
            }
        }

        integrasjonKlient.ferdigstillOppgave(oppgaveId = oppgaveId)

        return fagsak.id.toString()
    }

    private fun oppdaterOgFerdigstill(
        request: OppdaterJournalpostRequest,
        journalpostId: String,
        behandlendeEnhet: String,
        oppgaveId: String,
        barnetrygdBehandlinger: List<Behandling>,
    ) {
        runCatching {
            secureLogger.info("Oppdaterer journalpost $journalpostId med $request")
            integrasjonKlient.oppdaterJournalpost(request, journalpostId)
            genererOgOpprettLogg(journalpostId, barnetrygdBehandlinger)
            secureLogger.info("Ferdigstiller journalpost $journalpostId")
            integrasjonKlient.ferdigstillJournalpost(
                journalpostId = journalpostId,
                journalførendeEnhet = behandlendeEnhet,
            )
            integrasjonKlient.ferdigstillOppgave(oppgaveId = oppgaveId.toLong())
        }.onFailure {
            hentJournalpost(journalpostId).journalstatus.apply {
                if (this == FERDIGSTILT) {
                    integrasjonKlient.ferdigstillOppgave(oppgaveId = oppgaveId.toLong())
                } else {
                    throw it
                }
            }
        }
    }

    private fun genererOgOpprettLogg(
        journalpostId: String,
        behandlinger: List<Behandling>,
    ) {
        val journalpost = hentJournalpost(journalpostId)
        val loggTekst =
            journalpost.dokumenter?.fold("") { loggTekst, dokumentInfo ->
                loggTekst +
                    "${dokumentInfo.tittel}" +
                    dokumentInfo.logiskeVedlegg?.fold("") { logiskeVedleggTekst, logiskVedlegg ->
                        logiskeVedleggTekst +
                            "\n\u2002\u2002${logiskVedlegg.tittel}"
                    } + "\n"
            } ?: throw FunksjonellFeil(
                "Fant ingen dokumenter",
                frontendFeilmelding = "Noe gikk galt. Prøv igjen eller kontakt brukerstøtte hvis problemet vedvarer.",
            )

        val datoMottatt =
            journalpost.datoMottatt ?: throw FunksjonellFeil(
                "Fant ingen dokumenter",
                frontendFeilmelding = "Noe gikk galt. Prøv igjen eller kontakt brukerstøtte hvis problemet vedvarer.",
            )
        behandlinger.forEach {
            loggService.opprettMottattDokument(
                behandling = it,
                tekst = loggTekst,
                mottattDato = datoMottatt,
            )
        }
    }

    private fun lagreNedSøknadsinfoKnyttetTilBehandling(
        journalpost: Journalpost,
        brevkode: String,
        behandling: Behandling,
    ) {
        behandlingSøknadsinfoService.lagreSøknadsinfo(
            mottattDato = journalpost.datoMottatt?.toLocalDate() ?: LocalDate.now(),
            søknadsinfo =
                Søknadsinfo(
                    journalpostId = journalpost.journalpostId,
                    brevkode = brevkode,
                    erDigital = journalpost.kanal == NAV_NO,
                ),
            behandling = behandling,
        )
    }

    companion object {
        const val NAV_NO = "NAV_NO"
    }
}
