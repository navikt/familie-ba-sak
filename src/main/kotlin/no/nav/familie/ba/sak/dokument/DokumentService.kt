package no.nav.familie.ba.sak.dokument

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.steg.BehandlerRolle
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.brev.BrevService
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
        private val brevService: BrevService,
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

            val malMedData = malerService.mapTilVedtakBrevfelter(vedtak, behandlingResultat)
            dokGenKlient.lagPdfForMal(malMedData, headerFelter)
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

    fun genererManueltBrev(behandling: Behandling,
                           manueltBrevRequest: ManueltBrevRequest): ByteArray =
            if (featureToggleService.isEnabled("familie-ba-sak.bruk-ny-brevlosning.${manueltBrevRequest.brevmal.malId}", false)) {
                brevService.genererBrevPdf(behandling = behandling, manueltBrevRequest = manueltBrevRequest)
            } else {
                genererManueltBrevMedDokgen(behandling = behandling, manueltBrevRequest = manueltBrevRequest)
            }

    private fun genererManueltBrevMedDokgen(behandling: Behandling,
                                            manueltBrevRequest: ManueltBrevRequest): ByteArray =
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
                                          frontendFeilmelding = "Det har skjedd en feil, og brevet er ikke sendt. Prøv igjen, og ta kontakt med brukerstøtte hvis problemet vedvarer.",
                                          httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                                          throwable = it)
                    }
            )


    fun sendManueltBrev(behandling: Behandling,
                        manueltBrevRequest: ManueltBrevRequest): Ressurs<String> {

        val mottaker =
                persongrunnlagService.hentPersonPåBehandling(PersonIdent(manueltBrevRequest.mottakerIdent), behandling)
                ?: error("Finner ikke mottaker på behandlingen")

        val generertBrev =
                if (featureToggleService.isEnabled("familie-ba-sak.bruk-ny-brevlosning.${manueltBrevRequest.brevmal.malId}",
                                                   false)) {
                    brevService.genererBrevPdf(behandling, manueltBrevRequest)
                } else {
                    genererManueltBrev(behandling, manueltBrevRequest)
                }

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
}
