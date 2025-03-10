package no.nav.familie.ba.sak.integrasjoner.journalføring

import jakarta.transaction.Transactional
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.ekstern.restDomene.RestFerdigstillOppgaveKnyttJournalpost
import no.nav.familie.ba.sak.ekstern.restDomene.RestJournalføring
import no.nav.familie.ba.sak.ekstern.restDomene.TilknyttetBehandling
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
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
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
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
class InnkommendeJournalføringServiceV2(
    private val integrasjonClient: IntegrasjonClient,
    private val fagsakService: FagsakService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val loggService: LoggService,
    private val stegService: StegService,
    private val journalføringMetrikkV2: JournalføringMetrikkV2,
    private val behandlingSøknadsinfoService: BehandlingSøknadsinfoService,
    private val klageService: KlageService,
    private val unleashService: UnleashNextMedContextService,
) {
    fun hentDokument(
        journalpostId: String,
        dokumentInfoId: String,
    ): ByteArray = integrasjonClient.hentDokument(dokumentInfoId, journalpostId)

    fun hentJournalpost(journalpostId: String): Journalpost = integrasjonClient.hentJournalpost(journalpostId)

    fun hentJournalposterForBruker(brukerId: String): List<TilgangsstyrtJournalpost> =
        integrasjonClient
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
        val kanBehandleKlage = unleashService.isEnabled(FeatureToggle.BEHANDLE_KLAGE)
        val tilknyttedeBehandlinger: MutableList<TilknyttetBehandling> = request.tilknyttedeBehandlinger.toMutableList()
        val journalpost = integrasjonClient.hentJournalpost(journalpostId)
        val brevkode = journalpost.dokumenter?.firstNotNullOfOrNull { it.brevkode }

        val fagsak =
            if (request.fagsakId != null) {
                fagsakService.hentPåFagsakId(request.fagsakId)
            } else if (request.opprettOgKnyttTilNyBehandling) {
                fagsakService.hentEllerOpprettFagsak(
                    personIdent = request.bruker.id,
                    type = FagsakType.NORMAL,
                    institusjon = null,
                )
            } else {
                throw Feil("Forventet en fagsak ved journalføring")
            }

        if (request.opprettOgKnyttTilNyBehandling) {
            if (kanBehandleKlage && request.nyBehandlingstype == Journalføringsbehandlingstype.KLAGE) {
                val kravMottattDato = request.datoMottatt?.toLocalDate() ?: throw Feil("Dato mottatt ikke satt.")
                val klagebehandlingId = klageService.opprettKlage(fagsak, kravMottattDato)
                tilknyttedeBehandlinger.add(TilknyttetBehandling(Journalføringsbehandlingstype.KLAGE, klagebehandlingId.toString()))
            } else {
                val nyBehandling =
                    opprettBehandlingOgEvtFagsakForJournalføring(
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

        journalføringMetrikkV2.tellManuellJournalføringsmetrikker(request, tilknyttedeBehandlinger)

        return fagsak.id.toString()
    }

    fun knyttJournalpostTilFagsakOgFerdigstillOppgave(
        request: RestFerdigstillOppgaveKnyttJournalpost,
        oppgaveId: Long,
    ): String {
        val kanBehandleKlage = unleashService.isEnabled(FeatureToggle.BEHANDLE_KLAGE)
        val tilknyttedeBehandlinger: MutableList<TilknyttetBehandling> = request.tilknyttedeBehandlinger.toMutableList()

        val journalpost = hentJournalpost(request.journalpostId)
        journalpost.sak?.fagsakId

        val fagsak =
            if (request.fagsakId != null) {
                fagsakService.hentPåFagsakId(request.fagsakId)
            } else if (request.opprettOgKnyttTilNyBehandling) {
                fagsakService.hentEllerOpprettFagsak(
                    personIdent = request.bruker.id,
                    type = FagsakType.NORMAL,
                    institusjon = null,
                )
            } else {
                throw Feil("Forventet en fagsak ved ferdigstilling av oppgave")
            }

        if (request.opprettOgKnyttTilNyBehandling) {
            if (kanBehandleKlage && request.nyBehandlingstype == Journalføringsbehandlingstype.KLAGE) {
                val kravMottattDato = request.datoMottatt?.toLocalDate() ?: throw Feil("Dato mottatt ikke satt.")
                val klagebehandlingId = klageService.opprettKlage(fagsak, kravMottattDato)
                tilknyttedeBehandlinger.add(TilknyttetBehandling(Journalføringsbehandlingstype.KLAGE, klagebehandlingId.toString()))
            } else {
                val brevkode = journalpost.dokumenter?.firstNotNullOfOrNull { it.brevkode }
                val nyBehandling =
                    opprettBehandlingOgEvtFagsakForJournalføring(
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

        integrasjonClient.ferdigstillOppgave(oppgaveId = oppgaveId)

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
            integrasjonClient.oppdaterJournalpost(request, journalpostId)
            genererOgOpprettLogg(journalpostId, barnetrygdBehandlinger)
            secureLogger.info("Ferdigstiller journalpost $journalpostId")
            integrasjonClient.ferdigstillJournalpost(
                journalpostId = journalpostId,
                journalførendeEnhet = behandlendeEnhet,
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
        behandlingSøknadsinfoService.lagreNedSøknadsinfo(
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
