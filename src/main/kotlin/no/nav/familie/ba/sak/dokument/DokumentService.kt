package no.nav.familie.ba.sak.dokument

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.steg.BehandlerRolle
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatService
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.dokument.DokumentController.BrevType
import no.nav.familie.ba.sak.dokument.DokumentController.ManueltBrevRequest
import no.nav.familie.ba.sak.dokument.domene.DokumentHeaderFelter
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.journalføring.JournalføringService
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class DokumentService(
        private val behandlingResultatService: BehandlingResultatService,
        private val dokGenKlient: DokGenKlient,
        private val malerService: MalerService,
        private val persongrunnlagService: PersongrunnlagService,
        private val integrasjonClient: IntegrasjonClient,
        private val arbeidsfordelingService: ArbeidsfordelingService,
        private val loggService: LoggService,
        private val journalføringService: JournalføringService
) {

    private val antallBrevSendt: Map<BrevType, Counter> = BrevType.values().map {
        it to Metrics.counter("brev.sendt",
                              "brevmal", it.visningsTekst)
    }.toMap()

    fun hentBrevForVedtak(vedtak: Vedtak): Ressurs<ByteArray> {
        val pdf = vedtak.stønadBrevPdF
                  ?: error("Klarte ikke finne brev for vetak med id ${vedtak.id}")
        return Ressurs.success(pdf)
    }

    fun genererBrevForVedtak(vedtak: Vedtak): ByteArray {
        return Result.runCatching {
            val søker = persongrunnlagService.hentSøker(behandling = vedtak.behandling)
                        ?: error("Finner ikke søker på vedtaket")

            val behandlingResultatType =
                    behandlingResultatService.hentBehandlingResultatTypeFraBehandling(behandling = vedtak.behandling)

            val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = vedtak.behandling.id)
                                           ?: throw Feil(message = "Finner ikke personopplysningsgrunnlag ved generering av vedtaksbrev",
                                                         frontendFeilmelding = "Finner ikke personopplysningsgrunnlag ved generering av vedtaksbrev")

            val headerFelter = DokumentHeaderFelter(fodselsnummer = søker.personIdent.ident,
                                                    navn = søker.navn,
                                                    antallBarn = if (vedtak.behandling.skalBehandlesAutomatisk)
                                                        personopplysningGrunnlag.barna.size else null,
                                                    dokumentDato = LocalDate.now().tilDagMånedÅr(),
                                                    maalform = søker.målform.toString())

            val malMedData = malerService.mapTilVedtakBrevfelter(vedtak,
                                                                 behandlingResultatType
            )
            dokGenKlient.lagPdfForMal(malMedData, headerFelter)
        }
                .fold(
                        onSuccess = { it },
                        onFailure = {
                            throw Feil(message = "Klarte ikke generere vedtaksbrev",
                                       frontendFeilmelding = "Det har skjedd en feil, og brevet er ikke sendt. Prøv igjen, og ta kontakt med brukerstøtte hvis problemet vedvarer.",
                                       httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                                       throwable = it)
                        }
                )
    }

    fun genererManueltBrev(behandling: Behandling,
                           manueltBrevRequest: ManueltBrevRequest): ByteArray =
            Result.runCatching {
                val mottaker =
                        persongrunnlagService.hentPersonPåBehandling(PersonIdent(manueltBrevRequest.mottakerIdent), behandling)
                        ?: error("Finner ikke mottaker på vedtaket")

                val headerFelter = DokumentHeaderFelter(fodselsnummer = mottaker.personIdent.ident,
                                                        navn = mottaker.navn,
                                                        dokumentDato = LocalDate.now().tilDagMånedÅr(),
                                                        maalform = mottaker.målform.toString())
                val malMedData = when (manueltBrevRequest.brevmal) {
                    BrevType.INNHENTE_OPPLYSNINGER -> malerService.mapTilInnhenteOpplysningerBrevfelter(behandling,
                                                                                                        manueltBrevRequest)
                    else -> throw Feil(message = "Brevmal ${manueltBrevRequest.brevmal} er ikke støttet for manuelle brev.",
                                       frontendFeilmelding = "Klarte ikke generere brev. Brevmal ${manueltBrevRequest.brevmal.malId} er ikke støttet.")
                }
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

        val fagsakId = "${behandling.fagsak.id}"
        val generertBrev = genererManueltBrev(behandling, manueltBrevRequest)
        val enhet = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandling.id).behandlendeEnhetId

        val journalpostId = integrasjonClient.journalførManueltBrev(fnr = manueltBrevRequest.mottakerIdent,
                                                                    fagsakId = fagsakId,
                                                                    journalførendeEnhet = enhet,
                                                                    brev = generertBrev,
                                                                    brevType = manueltBrevRequest.brevmal.arkivType)

        journalføringService.lagreJournalPost(behandling, journalpostId)

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
