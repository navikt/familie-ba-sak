package no.nav.familie.ba.sak.brev.domene.maler

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.dokument.DokumentController
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.objectMapper
import java.time.LocalDate

interface Brev {

    val brevType: BrevType
    val brevData: BrevData
}

enum class BrevType(val apiNavn: String, val visningsTekst: String) {
    INNHENTE_OPPLYSNINGER("innhenteOpplysninger", "innhente opplysninger"),
    HENLEGGE_TRUKKET_SØKNAD("henleggeTrukketSøknad", "henlegge trukket søknad");
}

interface BrevData {

    val delmalData: Any
    val flettefelter: Any
    fun toBrevString(): String = objectMapper.writeValueAsString(this)
}

typealias Flettefelt = List<String>

fun flettefelt(flettefeltData: String): Flettefelt = listOf(flettefeltData)
fun flettefelt(flettefeltData: List<String>): Flettefelt = flettefeltData

fun no.nav.familie.ba.sak.dokument.domene.BrevType.tilNyBrevType() = when (this.malId) {
    no.nav.familie.ba.sak.dokument.domene.BrevType.INNHENTE_OPPLYSNINGER.malId -> BrevType.INNHENTE_OPPLYSNINGER
    no.nav.familie.ba.sak.dokument.domene.BrevType.HENLEGGE_TRUKKET_SØKNAD.malId -> BrevType.HENLEGGE_TRUKKET_SØKNAD
    else -> error("Kan ikke mappe brevmal ${this.visningsTekst} til ny brevtype da denne ikke er støttet i ny løsning enda.")
}

fun DokumentController.ManueltBrevRequest.tilBrevmal(enhetNavn: String, mottaker: Person) =
        InnhenteOpplysningerMal(
                brevData = InnhenteOpplysningerData(delmalData = InnhenteOpplysningerData.DelmalData(
                        signatur = SignaturDelmal(
                                enhet = flettefelt(enhetNavn),
                                saksbehandler = flettefelt(SikkerhetContext.hentSaksbehandlerNavn())
                        )
                ),
                                                    flettefelter = InnhenteOpplysningerData.Flettefelter(
                                                            navn = flettefelt(mottaker.navn),
                                                            fodselsnummer = flettefelt(mottaker.personIdent.ident),
                                                            dokumentliste = flettefelt(this.multiselectVerdier),
                                                            dato = flettefelt(LocalDate.now().tilDagMånedÅr())
                                                    ))
        )