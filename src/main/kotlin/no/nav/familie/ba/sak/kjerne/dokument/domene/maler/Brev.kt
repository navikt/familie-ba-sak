package no.nav.familie.ba.sak.kjerne.dokument.domene.maler

import no.nav.familie.ba.sak.kjerne.dokument.DokumentController.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.kontrakter.felles.objectMapper

interface Brev {

    val type: BrevType
    val data: BrevData
}

interface BrevType {

    val apiNavn: String
    val visningsTekst: String
}

enum class EnkelBrevtype(override val apiNavn: String, override val visningsTekst: String) : BrevType {
    INNHENTE_OPPLYSNINGER("innhenteOpplysninger", "innhente opplysninger"),
    HENLEGGE_TRUKKET_SØKNAD("henleggeTrukketSoknad", "henlegge trukket søknad"),
    VARSEL_OM_REVURDERING("varselOmRevurdering", "varsel om revurdering"),
    DØDSFALL("dodsfall", "Dødsfall"),
}

enum class Vedtaksbrevtype(override val apiNavn: String, override val visningsTekst: String) : BrevType {
    FØRSTEGANGSVEDTAK("forstegangsvedtak", "Førstegangsvedtak"),
    AUTOVEDTAK_NYFØDT_FØRSTE_BARN("autovedtakNyfoodtForsteBarn", "Autovedtak nyfødt - første barn"),
    VEDTAK_ENDRING("vedtakEndring", "Vedtak endring"),
    OPPHØRT("opphort", "Opphørt"),
    OPPHØR_MED_ENDRING("opphorMedEndring", "Opphør med endring"),
    AVSLAG("vedtakAvslag", "Avslag"),
    FORTSATT_INNVILGET("vedtakFortsattInnvilget", "Vedtak fortstatt innvilget"),
    AUTOVEDTAK_BARN6_ÅR("autovedtakBarn6År", "Autovedtak - Barn 6 år"),
    AUTOVEDTAK_BARN18_ÅR("autovedtakBarn18År", "Autovedtak - Barn 18 år"),
}

interface BrevData {

    val delmalData: Any
    val flettefelter: Any
    fun toBrevString(): String = objectMapper.writeValueAsString(this)
}

typealias Flettefelt = List<String>?

fun flettefelt(flettefeltData: String?): Flettefelt = if (flettefeltData != null) listOf(flettefeltData) else null
fun flettefelt(flettefeltData: List<String>): Flettefelt = flettefeltData


fun ManueltBrevRequest.tilBrevmal(enhetNavn: String, mottaker: Person) = when (this.brevmal.malId) {
    no.nav.familie.ba.sak.kjerne.dokument.domene.BrevType.INNHENTE_OPPLYSNINGER.malId ->
        InnhenteOpplysningerBrev(
                data = InnhenteOpplysningerData(
                        delmalData = InnhenteOpplysningerData.DelmalData(signatur = SignaturDelmal(enhet = enhetNavn)),
                        flettefelter = InnhenteOpplysningerData.Flettefelter(
                                navn = mottaker.navn,
                                fodselsnummer = mottaker.personIdent.ident,
                                dokumentliste = this.multiselectVerdier,
                        ))
        )
    no.nav.familie.ba.sak.kjerne.dokument.domene.BrevType.HENLEGGE_TRUKKET_SØKNAD.malId ->
        HenleggeTrukketSøknadBrev(
                data = HenleggeTrukketSøknadData(
                        delmalData = HenleggeTrukketSøknadData.DelmalData(signatur = SignaturDelmal(enhet = enhetNavn)),
                        flettefelter = HenleggeTrukketSøknadData.Flettefelter(
                                navn = mottaker.navn,
                                fodselsnummer = mottaker.personIdent.ident,
                        ))
        )
    no.nav.familie.ba.sak.kjerne.dokument.domene.BrevType.VARSEL_OM_REVURDERING.malId ->
        VarselOmRevurderingBrev(
                data = VarselOmRevurderingData(
                        delmalData = VarselOmRevurderingData.DelmalData(signatur = SignaturDelmal(enhet = enhetNavn)),
                        flettefelter = VarselOmRevurderingData.Flettefelter(
                                navn = mottaker.navn,
                                fodselsnummer = mottaker.personIdent.ident,
                                varselÅrsaker = this.multiselectVerdier,
                        ))
        )
    else -> error("Kan ikke mappe brevmal for ${
        this.brevmal.visningsTekst
    } til ny brevtype da denne ikke er støttet i ny løsning enda.")
}
