package no.nav.familie.ba.sak.dokument

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat.DELVIS_INNVILGET
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat.ENDRET_OG_FORTSATT_INNVILGET
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat.ENDRET_OG_OPPHØRT
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat.INNVILGET
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat.INNVILGET_OG_OPPHØRT
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat.OPPHØRT
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.steg.BehandlerRolle
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.brev.BrevKlient
import no.nav.familie.ba.sak.brev.domene.maler.Brev
import no.nav.familie.ba.sak.brev.domene.maler.tilBrevmal
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.dokument.DokumentController.ManueltBrevRequest
import no.nav.familie.ba.sak.dokument.domene.BrevType
import no.nav.familie.ba.sak.dokument.domene.DokumentHeaderFelter
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.journalføring.JournalføringService
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.opplysningsplikt.OpplysningspliktService
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.dokarkiv.Førsteside
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class DokumentService(
        private val dokGenKlient: DokGenKlient,
        private val malerService: MalerService,
        private val persongrunnlagService: PersongrunnlagService,
        private val integrasjonClient: IntegrasjonClient,
        private val arbeidsfordelingService: ArbeidsfordelingService,
        private val loggService: LoggService,
        private val journalføringService: JournalføringService,
        private val opplysningspliktService: OpplysningspliktService,
        private val behandlingService: BehandlingService,
        private val brevKlient: BrevKlient,
        private val featureToggleService: FeatureToggleService
) {

    private val antallBrevSendt: Map<BrevType, Counter> = BrevType.values().map {
        it to Metrics.counter("brev.sendt",
                              "brevmal", it.visningsTekst)
    }.toMap()

    fun hentBrevForVedtak(vedtak: Vedtak): Ressurs<ByteArray> {
        val pdf = vedtak.stønadBrevPdF
                  ?: throw Feil("Klarte ikke finne brev for vetak med id ${vedtak.id}")
        return Ressurs.success(pdf)
    }

    fun genererBrevForVedtak(vedtak: Vedtak): ByteArray {
        return Result.runCatching {
            if (!vedtak.behandling.skalBehandlesAutomatisk && vedtak.behandling.steg > StegType.BESLUTTE_VEDTAK) {
                throw Feil("Ikke tillatt å generere brev etter at behandlingen er sendt fra beslutter")
            }

            val søker = persongrunnlagService.hentSøker(behandlingId = vedtak.behandling.id)
                        ?: error("Finner ikke søker på vedtaket")

            val behandlingResultat = behandlingService.hent(behandlingId = vedtak.behandling.id).resultat

            val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = vedtak.behandling.id)
                                           ?: throw Feil(message = "Finner ikke personopplysningsgrunnlag ved generering av vedtaksbrev",
                                                         frontendFeilmelding = "Finner ikke personopplysningsgrunnlag ved generering av vedtaksbrev")

            val headerFelter = DokumentHeaderFelter(fodselsnummer = søker.personIdent.ident,
                                                    navn = søker.navn,
                                                    antallBarn = if (vedtak.behandling.skalBehandlesAutomatisk)
                                                        personopplysningGrunnlag.barna.size else null,
                                                    dokumentDato = LocalDate.now().tilDagMånedÅr(),
                                                    maalform = søker.målform)


            val toggleSuffix = brevToggelNavnSuffix(vedtak, behandlingResultat)

            if (featureToggleService.isEnabled("familie-ba-sak.bruk-ny-brevlosning.vedtak-${toggleSuffix}", false)) {
                val målform = persongrunnlagService.hentSøkersMålform(vedtak.behandling.id)
                val vedtaksbrev = malerService.mapTilNyttVedtaksbrev(vedtak, behandlingResultat)
                return brevKlient.genererBrev(målform.tilSanityFormat(), vedtaksbrev)
            } else {
                val malMedData = malerService.mapTilVedtakBrevfelter(vedtak, behandlingResultat)
                return dokGenKlient.lagPdfForMal(malMedData, headerFelter)
            }
        }
                .fold(
                        onSuccess = { it },
                        onFailure = {
                            throw Feil(message = "Klarte ikke generere vedtaksbrev: ${it.message}",
                                       frontendFeilmelding = "Det har skjedd en feil, og brevet er ikke sendt. Prøv igjen, og ta kontakt med brukerstøtte hvis problemet vedvarer.",
                                       httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                                       throwable = it)
                        }
                )
    }

    private fun brevToggelNavnSuffix(vedtak: Vedtak,
                                     behandlingResultat: BehandlingResultat): String {
        return if (vedtak.behandling.skalBehandlesAutomatisk) {
            BrevToggleSuffix.IKKE_STØTTET.suffix
        } else when (vedtak.behandling.type) {
            BehandlingType.FØRSTEGANGSBEHANDLING -> when (behandlingResultat) {
                INNVILGET, INNVILGET_OG_OPPHØRT, DELVIS_INNVILGET -> BrevToggleSuffix.FØRSTEGANGSBEHANDLING.suffix
                else -> BrevToggleSuffix.IKKE_STØTTET.suffix
            }
            BehandlingType.REVURDERING -> when (behandlingResultat) {
                INNVILGET, DELVIS_INNVILGET -> BrevToggleSuffix.VEDTAK_ENDRING.suffix
                OPPHØRT -> BrevToggleSuffix.OPPHØR.suffix
                INNVILGET_OG_OPPHØRT, ENDRET_OG_OPPHØRT -> BrevToggleSuffix.OPPHØR_MED_ENDRING.suffix
                else -> BrevToggleSuffix.IKKE_STØTTET.suffix
            }
            else -> BrevToggleSuffix.IKKE_STØTTET.suffix
        }
    }

    fun genererManueltBrev(behandling: Behandling,
                           manueltBrevRequest: ManueltBrevRequest,
                           erForhåndsvisning: Boolean = false): ByteArray =
            if (featureToggleService.isEnabled("familie-ba-sak.bruk-ny-brevlosning.${manueltBrevRequest.brevmal.malId}", false)) {
                genererManueltBrevMedFamilieBrev(behandling = behandling,
                                                 manueltBrevRequest = manueltBrevRequest,
                                                 erForhåndsvisning = erForhåndsvisning)
            } else {
                genererManueltBrevMedDokgen(behandling = behandling,
                                            manueltBrevRequest = manueltBrevRequest,
                                            erForhåndsvisning = erForhåndsvisning)
            }

    private fun genererManueltBrevMedFamilieBrev(behandling: Behandling,
                                                 manueltBrevRequest: ManueltBrevRequest,
                                                 erForhåndsvisning: Boolean = false): ByteArray {
        Result.runCatching {
            val mottaker =
                    persongrunnlagService.hentPersonPåBehandling(PersonIdent(manueltBrevRequest.mottakerIdent), behandling)
                    ?: error("Finner ikke mottaker på vedtaket")

            val (enhetNavn, målform) = hentEnhetNavnOgMålform(behandling)

            val brev: Brev = manueltBrevRequest.tilBrevmal(enhetNavn, mottaker)
            return brevKlient.genererBrev(målform = målform.tilSanityFormat(),
                                          brev = brev)
        }.fold(
                onSuccess = { it },
                onFailure = {
                    if (it is Feil) {
                        throw it
                    } else throw Feil(message = "Klarte ikke generere brev for ${manueltBrevRequest.brevmal.visningsTekst}. ${it.message}",
                                      frontendFeilmelding = "${if (erForhåndsvisning) "Det har skjedd en feil" else "Det har skjedd en feil, og brevet er ikke sendt"}. Prøv igjen, og ta kontakt med brukerstøtte hvis problemet vedvarer.",
                                      httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                                      throwable = it)
                }
        )
    }

    private fun genererManueltBrevMedDokgen(behandling: Behandling,
                                            manueltBrevRequest: ManueltBrevRequest,
                                            erForhåndsvisning: Boolean = false): ByteArray =
            Result.runCatching {
                val mottaker =
                        persongrunnlagService.hentPersonPåBehandling(PersonIdent(manueltBrevRequest.mottakerIdent), behandling)
                        ?: error("Finner ikke mottaker på vedtaket")

                val headerFelter = DokumentHeaderFelter(fodselsnummer = mottaker.personIdent.ident,
                                                        navn = mottaker.navn,
                                                        dokumentDato = LocalDate.now().tilDagMånedÅr(),
                                                        maalform = mottaker.målform)
                val malMedData =
                        malerService.mapTilManuellMalMedData(behandling = behandling, manueltBrevRequest = manueltBrevRequest)
                dokGenKlient.lagPdfForMal(malMedData, headerFelter)
            }.fold(
                    onSuccess = { it },
                    onFailure = {
                        if (it is Feil) {
                            throw it
                        } else throw Feil(message = "Klarte ikke generere brev for ${manueltBrevRequest.brevmal.visningsTekst}",
                                          frontendFeilmelding = "${if (erForhåndsvisning) "Det har skjedd en feil" else "Det har skjedd en feil, og brevet er ikke sendt"}. Prøv igjen, og ta kontakt med brukerstøtte hvis problemet vedvarer.",
                                          httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                                          throwable = it)
                    }
            )


    fun sendManueltBrev(behandling: Behandling,
                        manueltBrevRequest: ManueltBrevRequest): Ressurs<String> {

        val mottaker =
                persongrunnlagService.hentPersonPåBehandling(PersonIdent(manueltBrevRequest.mottakerIdent), behandling)
                ?: error("Finner ikke mottaker på behandlingen")

        val generertBrev = genererManueltBrev(behandling, manueltBrevRequest)

        val enhet = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandling.id).behandlendeEnhetId

        val førsteside = if (manueltBrevRequest.brevmal.genererForside) {
            Førsteside(maalform = mottaker.målform.name,
                       navSkjemaId = "NAV 33.00-07",
                       overskriftsTittel = "Ettersendelse til søknad om barnetrygd ordinær NAV 33-00.07")
        } else null

        val journalpostId = integrasjonClient.journalførManueltBrev(fnr = manueltBrevRequest.mottakerIdent,
                                                                    fagsakId = behandling.fagsak.id.toString(),
                                                                    journalførendeEnhet = enhet,
                                                                    brev = generertBrev,
                                                                    førsteside = førsteside,
                                                                    brevType = manueltBrevRequest.brevmal.arkivType)

        journalføringService.lagreJournalPost(behandling, journalpostId)

        if (manueltBrevRequest.brevmal == BrevType.INNHENTE_OPPLYSNINGER) {
            opplysningspliktService.lagreBlankOpplysningsplikt(behandlingId = behandling.id)
        }

        return distribuerBrevOgLoggHendelse(journalpostId = journalpostId,
                                            behandlingId = behandling.id,
                                            loggTekst = manueltBrevRequest.brevmal.visningsTekst.capitalize(),
                                            loggBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                                            brevType = manueltBrevRequest.brevmal)
    }

    fun distribuerBrevOgLoggHendelse(journalpostId: String,
                                     behandlingId: Long,
                                     loggTekst: String,
                                     loggBehandlerRolle: BehandlerRolle,
                                     brevType: BrevType

    ): Ressurs<String> {
        val distribuerBrevBestillingId = integrasjonClient.distribuerBrev(journalpostId)
        loggService.opprettDistribuertBrevLogg(behandlingId = behandlingId,
                                               tekst = loggTekst,
                                               rolle = loggBehandlerRolle)
        antallBrevSendt[brevType]?.increment()

        return distribuerBrevBestillingId
    }

    private fun hentEnhetNavnOgMålform(behandling: Behandling): Pair<String, Målform> {
        return Pair(arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandling.id).behandlendeEnhetNavn,
                    persongrunnlagService.hentSøker(behandling.id)?.målform ?: Målform.NB)
    }

    companion object {
        enum class BrevToggleSuffix(val suffix: String) {
            IKKE_STØTTET("ikke-stottet"),
            FØRSTEGANGSBEHANDLING("forstegangsbehandling"),
            VEDTAK_ENDRING("vedtak-endring"),
            OPPHØR("opphor"),
            OPPHØR_MED_ENDRING("opphor-med-endring")
        }
    }

}
