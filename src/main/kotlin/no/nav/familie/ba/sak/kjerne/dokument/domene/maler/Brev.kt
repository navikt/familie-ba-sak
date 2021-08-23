package no.nav.familie.ba.sak.kjerne.dokument.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.kjerne.dokument.DokumentController.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.kontrakter.felles.objectMapper
import java.time.LocalDate

interface Brev {

    val mal: Brevmal
    val data: BrevData
}

enum class BrevType {
    ENKEL,
    VEDAK,
}

enum class Brevmal(val brevType: BrevType, val apiNavn: String, val visningsTekst: String) {
    INNHENTE_OPPLYSNINGER(BrevType.ENKEL, "innhenteOpplysninger", "innhente opplysninger"),
    HENLEGGE_TRUKKET_SØKNAD(BrevType.ENKEL, "henleggeTrukketSoknad", "henlegge trukket søknad"),
    VARSEL_OM_REVURDERING(BrevType.ENKEL, "varselOmRevurdering", "varsel om revurdering"),
    DØDSFALL(BrevType.ENKEL, "dodsfall", "Dødsfall"),

    FØRSTEGANGSVEDTAK(BrevType.VEDAK, "forstegangsvedtak", "Førstegangsvedtak"),
    VEDTAK_ENDRING(BrevType.VEDAK, "vedtakEndring", "Vedtak endring"),
    OPPHØRT(BrevType.VEDAK, "opphort", "Opphørt"),
    OPPHØR_MED_ENDRING(BrevType.VEDAK, "opphorMedEndring", "Opphør med endring"),
    AVSLAG(BrevType.VEDAK, "vedtakAvslag", "Avslag"),
    FORTSATT_INNVILGET(BrevType.VEDAK, "vedtakFortsattInnvilget", "Vedtak fortstatt innvilget"),
    AUTOVEDTAK_BARN6_ÅR(BrevType.VEDAK, "autovedtakBarn6År", "Autovedtak - Barn 6 år"),
    AUTOVEDTAK_BARN18_ÅR(BrevType.VEDAK, "autovedtakBarn18År", "Autovedtak - Barn 18 år"),
    AUTOVEDTAK_NYFØDT_FØRSTE_BARN(BrevType.VEDAK, "autovedtakNyfodtForsteBarn", "Autovedtak nyfødt - første barn"),
    AUTOVEDTAK_NYFØDT_BARN_FRA_FØR(BrevType.VEDAK, "autovedtakNyfodtBarnFraFor", "Autovedtak nyfødt - barn fra før"),
}

interface BrevData {

    val delmalData: Any
    val flettefelter: FlettefelterForDokument
    fun toBrevString(): String = objectMapper.writeValueAsString(this)
}

interface FlettefelterForDokument {

    val navn: Flettefelt
    val fodselsnummer: Flettefelt
    val brevOpprettetDato: Flettefelt
}

data class FlettefelterForDokumentImpl(
        override val navn: Flettefelt,
        override val fodselsnummer: Flettefelt,
        override val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
) : FlettefelterForDokument {

    constructor(navn: String,
                fodselsnummer: String) : this(navn = flettefelt(navn),
                                              fodselsnummer = flettefelt(fodselsnummer))
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
                        flettefelter = FlettefelterForDokumentImpl(
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
