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

enum class Brevmal(val erVedtaksbrev: Boolean, val apiNavn: String, val visningsTekst: String) {
    INNHENTE_OPPLYSNINGER(false, "innhenteOpplysninger", "innhente opplysninger"),
    HENLEGGE_TRUKKET_SØKNAD(false, "henleggeTrukketSoknad", "henlegge trukket søknad"),
    VARSEL_OM_REVURDERING(false, "varselOmRevurdering", "varsel om revurdering"),
    DØDSFALL(false, "dodsfall", "Dødsfall"),

    VEDTAK_FØRSTEGANGSVEDTAK(true, "forstegangsvedtak", "Førstegangsvedtak"),
    VEDTAK_ENDRING(true, "vedtakEndring", "Vedtak endring"),
    VEDTAK_OPPHØRT(true, "opphort", "Opphørt"),
    VEDTAK_OPPHØR_MED_ENDRING(true, "opphorMedEndring", "Opphør med endring"),
    VEDTAK_AVSLAG(true, "vedtakAvslag", "Avslag"),
    VEDTAK_FORTSATT_INNVILGET(true, "vedtakFortsattInnvilget", "Vedtak fortstatt innvilget"),
    AUTOVEDTAK_BARN6_ÅR(true, "autovedtakBarn6År", "Autovedtak - Barn 6 år"),
    AUTOVEDTAK_BARN18_ÅR(true, "autovedtakBarn18År", "Autovedtak - Barn 18 år"),
    AUTOVEDTAK_NYFØDT_FØRSTE_BARN(true, "autovedtakNyfodtForsteBarn", "Autovedtak nyfødt - første barn"),
    AUTOVEDTAK_NYFØDT_BARN_FRA_FØR(true, "autovedtakNyfodtBarnFraFor", "Autovedtak nyfødt - barn fra før"),
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
