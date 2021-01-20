package no.nav.familie.ba.sak.brev

import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.dokument.DokumentController
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class FamilieBrevService(val familieBrevKlient: FamilieBrevKlient,
                         val persongrunnlagService: PersongrunnlagService,
                         val arbeidsfordelingService: ArbeidsfordelingService
) {

    fun genererBrevTest(målform: String, malnavn: String): ByteArray {
        val testbody = "{\n" +
                       "    \"flettefelter\": {\n" +
                       "        \"navn\": [\"Navn Navnesen\"],\n" +
                       "        \"fodselsnummer\": [\"1123456789\"],\n" +
                       "        \"dokumentListe\": [\n" +
                       "            \"Oppholdstillatelse\",\n" +
                       "            \"Dokumenteksempel\",\n" +
                       "            \"Tredje dokument\"\n" +
                       "        ]\n" +
                       "    },\n" +
                       "    \"delmalData\": {\n" +
                       "        \"signatur\": {\n" +
                       "            \"ENHET\": [\"Enhet eksempel her\"],\n" +
                       "            \"SAKSBEHANDLER1\": [\"Navn Navnesen\"]\n" +
                       "        }\n" +
                       "    }\n" +
                       "}"

        return familieBrevKlient.genererBrev(målform, malnavn, testbody)
    }

    fun genererBrev(behandling: Behandling,
                    manueltBrevRequest: DokumentController.ManueltBrevRequest): ByteArray {
        Result.runCatching {
            val mottaker =
                    persongrunnlagService.hentPersonPåBehandling(PersonIdent(manueltBrevRequest.mottakerIdent), behandling)
                    ?: error("Finner ikke mottaker på vedtaket")

            val (enhetNavn, målform) = hentMålformOgEnhetNavn(behandling)

            val brevDokument = lagBrevDokument(enhetNavn, mottaker, manueltBrevRequest)
            return familieBrevKlient.genererBrev(målform.tilSanityFormat(), "innhenteOpplysninger", brevDokument)
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

    private fun lagBrevDokument(enhetNavn: String,
                                mottaker: Person,
                                manueltBrevRequest: DokumentController.ManueltBrevRequest) =
            BrevDokument(
                    delmalData = mapOf(Pair("signatur",
                                            mapOf(Pair("ENHET", listOf(enhetNavn)),
                                                  Pair("SAKSBEHANDLER1", listOf(SikkerhetContext.hentSaksbehandlerNavn()))))),
                    flettefelter = mapOf(
                            Pair("navn", listOf(mottaker.navn)),
                            Pair("fodselsnummer", listOf(mottaker.personIdent.ident)),
                            Pair("dokumentListe", manueltBrevRequest.multiselectVerdier)
                    ))

    private fun hentMålformOgEnhetNavn(behandling: Behandling): Pair<String, Målform> {
        return Pair(arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandling.id).behandlendeEnhetNavn,
                    persongrunnlagService.hentSøker(behandling.id)?.målform ?: Målform.NB)
    }

}


data class InnhenteOpplysningerMal(
        val navn: Flettefelt,
        val fodselsnummer: Flettefelt,
        val dokumentListe: Flettefelt,
        val signaturDelmal: SignaturDelmal,
)

data class SignaturDelmal(
        val enhet: Flettefelt,
        val saksbehandler1: Flettefelt
) : Delmal

interface Delmal

data class BrevDokument(
        val delmalData: Map<String, Flettefelter>,
        val flettefelter: Flettefelter
)

typealias Flettefelter = Map<String, Flettefelt>

typealias Flettefelt = List<String>


enum class DelmalType(val apiNavn: String, val visningsTekst: String) {
    INNHENTE_OPPLYSNINGER("innhenteOpplysninger", "Innhente opplysninger"),
    SIGNATUR("signatur", "Signatur");
}