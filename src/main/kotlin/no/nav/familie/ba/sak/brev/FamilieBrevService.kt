package no.nav.familie.ba.sak.brev

import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.brev.domene.maler.toBrevmal
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.dokument.DokumentController
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class FamilieBrevService(val familieBrevKlient: FamilieBrevKlient,
                         val persongrunnlagService: PersongrunnlagService,
                         val arbeidsfordelingService: ArbeidsfordelingService
) {

    fun genererBrev(behandling: Behandling,
                    manueltBrevRequest: DokumentController.ManueltBrevRequest): ByteArray {
        Result.runCatching {
            val mottaker =
                    persongrunnlagService.hentPersonPåBehandling(PersonIdent(manueltBrevRequest.mottakerIdent), behandling)
                    ?: error("Finner ikke mottaker på vedtaket")

            val (enhetNavn, målform) = hentMålformOgEnhetNavn(behandling)

            val brevDokument = manueltBrevRequest.toBrevmal(enhetNavn, mottaker)
            return familieBrevKlient.genererBrev(målform.tilSanityFormat(),
                                                 "innhenteOpplysninger",
                                                 brevDokument)
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
    }

    private fun hentMålformOgEnhetNavn(behandling: Behandling): Pair<String, Målform> {
        return Pair(arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandling.id).behandlendeEnhetNavn,
                    persongrunnlagService.hentSøker(behandling.id)?.målform ?: Målform.NB)
    }
}


enum class DelmalType(val apiNavn: String, val visningsTekst: String) {
    INNHENTE_OPPLYSNINGER("innhenteOpplysninger", "Innhente opplysninger"),
    SIGNATUR("signatur", "Signatur");
}