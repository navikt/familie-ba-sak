package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.kontrakter.felles.objectMapper
import java.time.LocalDate

interface Brev {

    val mal: Brevmal
    val data: BrevData
}

enum class Brevmal(val erVedtaksbrev: Boolean, val apiNavn: String, val visningsTekst: String) {
    INFORMASJONSBREV_DELT_BOSTED(false, "informasjonsbrevDeltBosted", "informasjonsbrev delt bosted"),
    INNHENTE_OPPLYSNINGER(false, "innhenteOpplysninger", "innhente opplysninger"),
    HENLEGGE_TRUKKET_SØKNAD(false, "henleggeTrukketSoknad", "henlegge trukket søknad"),
    VARSEL_OM_REVURDERING(false, "varselOmRevurdering", "varsel om revurdering"),
    SVARTIDSBREV(false, "svartidsbrev", "Svartidsbrev"),
    INFORMASJONSBREV_FØDSEL_MINDREÅRIG(
        false,
        "informasjonsbrevFodselMindreaarig",
        "Informasjonsbrev fødsel mindreårig"
    ),
    INFORMASJONSBREV_FØDSEL_UMYNDIG(false, "informasjonsbrevFodselUmyndig", "Informasjonsbrev fødsel umyndig"),
    INFORMASJONSBREV_KAN_SØKE(false, "informasjonsbrevKanSoke", "Informasjonsbrev kan søke"),

    VEDTAK_FØRSTEGANGSVEDTAK(true, "forstegangsvedtak", "Førstegangsvedtak"),
    VEDTAK_ENDRING(true, "vedtakEndring", "Vedtak endring"),
    VEDTAK_OPPHØRT(true, "opphort", "Opphørt"),
    VEDTAK_OPPHØR_MED_ENDRING(true, "opphorMedEndring", "Opphør med endring"),
    VEDTAK_AVSLAG(true, "vedtakAvslag", "Avslag"),
    VEDTAK_FORTSATT_INNVILGET(true, "vedtakFortsattInnvilget", "Vedtak fortstatt innvilget"),
    VEDTAK_KORREKSJON_VEDTAKSBREV(true, "korrigertVedtakEgenBrevmal", "Korrigere vedtak med egen brevmal"),
    VEDTAK_OPPHØR_DØDSFALL(true, "dodsfall", "Dødsfall"),

    @Deprecated(
        "Brukes ikke lenger. Må ha denne for å kunne få gjennom tasker med gammelt enum-navn." +
            "Kan fjernes når de har kjørt."
    )
    DØDSFALL(true, "dodsfall", "Dødsfall"),

    AUTOVEDTAK_BARN_6_OG_18_ÅR_OG_SMÅBARNSTILLEGG(
        true,
        "autovedtakBarn6AarOg18AarOgSmaabarnstillegg",
        "Autovedtak - Barn 6 og 18 år og småbarnstillegg"
    ),
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

    constructor(
        navn: String,
        fodselsnummer: String
    ) : this(
        navn = flettefelt(navn),
        fodselsnummer = flettefelt(fodselsnummer)
    )
}

typealias Flettefelt = List<String>?

fun flettefelt(flettefeltData: String?): Flettefelt = if (flettefeltData != null) listOf(flettefeltData) else null
fun flettefelt(flettefeltData: List<String>): Flettefelt = flettefeltData
