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
                                                    dokumentDato = LocalDate.now().tilDagMånedÅr())

            val malMedData = malerService.mapTilVedtakBrevfelter(vedtak,
                                                                 behandlingResultatType
            )
            dokGenKlient.lagPdfForMal(malMedData, headerFelter)
        }
                .fold(
                        onSuccess = { it },
                        onFailure = {
                            throw Feil(message = "Klarte ikke generere vedtaksbrev",
                                       frontendFeilmelding = "Noe gikk galt ved generering av vedtaksbrev og systemansvarlige er varslet. Prøv igjen senere, men hvis problemet vedvarer kontakt brukerstøtte",
                                       httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                                       throwable = it)
                        }
                )
    }

    fun genererManueltBrev(behandling: Behandling,
                           brevmal: BrevType,
                           manueltBrevRequest: ManueltBrevRequest): ByteArray =
            Result.runCatching {

                val søker = persongrunnlagService.hentSøker(behandling)
                            ?: error("Finner ikke søker på vedtaket")
                val headerFelter = DokumentHeaderFelter(fodselsnummer = søker.personIdent.ident,
                                                        navn = søker.navn,
                                                        dokumentDato = LocalDate.now().tilDagMånedÅr())
                val malMedData = when (brevmal) {
                    BrevType.INNHENTE_OPPLYSNINGER -> malerService.mapTilInnhenteOpplysningerBrevfelter(behandling,
                                                                                                        manueltBrevRequest)
                    else -> throw Feil(message = "Brevmal $brevmal er ikke støttet for manuelle brev.",
                                       frontendFeilmelding = "Klarte ikke generere brev. Brevmal ${brevmal.malId} er ikke støttet.")
                }
                dokGenKlient.lagPdfForMal(malMedData, headerFelter)
            }.fold(
                    onSuccess = { it },
                    onFailure = {
                        if (it is Feil) {
                            throw it
                        } else throw Feil(message = "Klarte ikke generere brev for innhente opplysninger",
                                          frontendFeilmelding = "Noe gikk galt ved generering av brev for å innhente opplysninger og systemansvarlige er varslet. Prøv igjen senere, men hvis problemet vedvarer kontakt brukerstøtte",
                                          httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                                          throwable = it)
                    }
            )


    fun sendManueltBrev(behandling: Behandling,
                        brevmal: BrevType,
                        manueltBrevRequest: ManueltBrevRequest): Ressurs<String> {

        val fnr = behandling.fagsak.hentAktivIdent().ident
        val fagsakId = "${behandling.fagsak.id}"
        val generertBrev = genererManueltBrev(behandling, brevmal, manueltBrevRequest)
        val enhet = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandling.id).behandlendeEnhetId

        val journalføringsId = integrasjonClient.journalførManueltBrev(fnr = fnr,
                                                                       fagsakId = fagsakId,
                                                                       journalførendeEnhet = enhet,
                                                                       brev = generertBrev,
                                                                       brevType = brevmal.arkivType)

        journalføringService.lagreJournalPost(behandling, journalføringsId)
        val distribuertBrevRessurs = integrasjonClient.distribuerBrev(journalføringsId)

        loggService.opprettDistribuertBrevLogg(behandlingId = behandling.id,
                                               tekst = "Brev for ${brevmal.visningsTekst} er sendt til bruker",
                                               rolle = BehandlerRolle.SAKSBEHANDLER)
        antallBrevSendt[brevmal]?.increment()

        return distribuertBrevRessurs
    }

    fun sendVedtaksbrev(journalpostId: String,
                        behandlingId: Long) {

        integrasjonClient.distribuerBrev(journalpostId)
        loggService.opprettDistribuertBrevLogg(behandlingId = behandlingId,
                                               tekst = "Vedtaksbrev er sendt til bruker",
                                               rolle = BehandlerRolle.SYSTEM)
        antallBrevSendt[BrevType.VEDTAK]?.increment()
    }
}
