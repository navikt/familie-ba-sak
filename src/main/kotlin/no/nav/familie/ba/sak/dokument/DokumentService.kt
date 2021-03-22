package no.nav.familie.ba.sak.dokument

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.annenvurdering.AnnenVurderingType
import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.steg.BehandlerRolle
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingService
import no.nav.familie.ba.sak.brev.BrevKlient
import no.nav.familie.ba.sak.brev.BrevService
import no.nav.familie.ba.sak.brev.domene.maler.Brev
import no.nav.familie.ba.sak.brev.domene.maler.tilBrevmal
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.dokument.DokumentController.ManueltBrevRequest
import no.nav.familie.ba.sak.dokument.domene.BrevType
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.journalføring.JournalføringService
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.dokarkiv.Førsteside
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class DokumentService(
        private val persongrunnlagService: PersongrunnlagService,
        private val integrasjonClient: IntegrasjonClient,
        private val arbeidsfordelingService: ArbeidsfordelingService,
        private val loggService: LoggService,
        private val journalføringService: JournalføringService,
        private val brevKlient: BrevKlient,
        private val brevService: BrevService,
        private val vilkårsvurderingService: VilkårsvurderingService
) {

    private val antallBrevSendt: Map<BrevType, Counter> = BrevType.values().map {
        it to Metrics.counter("brev.sendt",
                              "brevmal", it.visningsTekst)
    }.toMap()

    fun hentBrevForVedtak(vedtak: Vedtak): Ressurs<ByteArray> {
        val pdf = vedtak.stønadBrevPdF ?: throw Feil("Klarte ikke finne brev for vetak med id ${vedtak.id}")
        return Ressurs.success(pdf)
    }

    fun genererBrevForVedtak(vedtak: Vedtak): ByteArray {
        try {
            if (!vedtak.behandling.skalBehandlesAutomatisk && vedtak.behandling.steg > StegType.BESLUTTE_VEDTAK) {
                throw Feil("Ikke tillatt å generere brev etter at behandlingen er sendt fra beslutter")
            }

            val målform = persongrunnlagService.hentSøkersMålform(vedtak.behandling.id)
            val vedtaksbrev = brevService.hentVedtaksbrevData(vedtak)
            return brevKlient.genererBrev(målform.tilSanityFormat(), vedtaksbrev)
        } catch (funksjonellFeil: FunksjonellFeil) {
            throw funksjonellFeil
        } catch (feil: Throwable) {
            throw Feil(message = "Klarte ikke generere vedtaksbrev: ${feil.message}",
                       frontendFeilmelding = "Det har skjedd en feil, og brevet er ikke sendt. Prøv igjen, og ta kontakt med brukerstøtte hvis problemet vedvarer.",
                       httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                       throwable = feil)
        }
    }

    fun genererManueltBrev(behandling: Behandling,
                           manueltBrevRequest: ManueltBrevRequest,
                           erForhåndsvisning: Boolean = false): ByteArray =
            genererManueltBrevMedFamilieBrev(behandling = behandling,
                                             manueltBrevRequest = manueltBrevRequest,
                                             erForhåndsvisning = erForhåndsvisning)


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

        if (manueltBrevRequest.brevmal == BrevType.INNHENTE_OPPLYSNINGER ||
            manueltBrevRequest.brevmal == BrevType.VARSEL_OM_REVURDERING) {
            vilkårsvurderingService.opprettOglagreBlankAnnenVurdering(annenVurderingType = AnnenVurderingType.OPPLYSNINGSPLIKT,
                                                                      behandlingId = behandling.id)
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

}
