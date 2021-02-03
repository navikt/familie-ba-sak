package no.nav.familie.ba.sak.brev.domene.maler

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.dokument.DokumentController.ManueltBrevRequest
import no.nav.familie.kontrakter.felles.objectMapper

interface Brev {

    val brevType: BrevType
    val brevData: BrevData
}

enum class BrevType(val apiNavn: String, val visningsTekst: String) {
    INNHENTE_OPPLYSNINGER("innhenteOpplysninger", "innhente opplysninger"),
    HENLEGGE_TRUKKET_SØKNAD("henleggeTrukketSoknad", "henlegge trukket søknad"),
    VARSEL_OM_REVURDERING("varselOmRevurdering", "varsel om revurdering"),
}

interface BrevData {

    val delmalData: Any
    val flettefelter: Any
    fun toBrevString(): String = objectMapper.writeValueAsString(this)
}

typealias Flettefelt = List<String>

fun flettefelt(flettefeltData: String): Flettefelt = listOf(flettefeltData)
fun flettefelt(flettefeltData: List<String>): Flettefelt = flettefeltData


fun ManueltBrevRequest.tilBrevmal(enhetNavn: String, mottaker: Person) = when (this.brevmal.malId) {
    no.nav.familie.ba.sak.dokument.domene.BrevType.INNHENTE_OPPLYSNINGER.malId ->
        InnhenteOpplysningerBrev(
                brevData = InnhenteOpplysningerData(
                        delmalData = InnhenteOpplysningerData.DelmalData(signatur = SignaturDelmal(enhet = enhetNavn)),
                        flettefelter = InnhenteOpplysningerData.Flettefelter(
                                navn = mottaker.navn,
                                fodselsnummer = mottaker.personIdent.ident,
                                dokumentliste = this.multiselectVerdier,
                        ))
        )
    no.nav.familie.ba.sak.dokument.domene.BrevType.HENLEGGE_TRUKKET_SØKNAD.malId ->
        HenleggeTrukketSøknadBrev(
                brevData = HenleggeTrukketSøknadData(
                        delmalData = HenleggeTrukketSøknadData.DelmalData(signatur = SignaturDelmal(enhet = enhetNavn)),
                        flettefelter = HenleggeTrukketSøknadData.Flettefelter(
                                navn = mottaker.navn,
                                fodselsnummer = mottaker.personIdent.ident,
                        ))
        )
    no.nav.familie.ba.sak.dokument.domene.BrevType.VARSEL_OM_REVURDERING.malId ->
        VarselOmRevurderingBrev(
                brevData = VarselOmRevurderingData(
                        delmalData = VarselOmRevurderingData.DelmalData(signatur = SignaturDelmal(enhet = enhetNavn)),
                        flettefelter = VarselOmRevurderingData.Flettefelter(
                                navn = mottaker.navn,
                                fodselsnummer = mottaker.personIdent.ident,
                                varselÅrsaker = this.multiselectVerdier,
                        ))
        )
    else -> error("Kan ikke mappe brevmal for ${this.brevmal.visningsTekst} til ny brevtype da denne ikke er støttet i ny løsning enda.")
}
