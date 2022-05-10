package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentÅrsak
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.objectMapper
import java.time.LocalDate

interface Brev {

    val mal: Brevmal
    val data: BrevData
}

enum class Brevmal(val erVedtaksbrev: Boolean, val apiNavn: String, val visningsTekst: String) {
    INFORMASJONSBREV_DELT_BOSTED(false, "informasjonsbrevDeltBosted", "Informasjonsbrev delt bosted"),
    INNHENTE_OPPLYSNINGER(false, "innhenteOpplysninger", "Innhente opplysninger"),
    INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED(
        false,
        "innhenteOpplysningerEtterSoknadISED",
        "Innhente opplysninger etter søknad i SED"
    ),

    HENLEGGE_TRUKKET_SØKNAD(false, "henleggeTrukketSoknad", "Henlegge trukket søknad"),
    VARSEL_OM_REVURDERING(false, "varselOmRevurdering", "Varsel om revurdering"),
    VARSEL_OM_REVURDERING_DELT_BOSTED_PARAGRAF_14(
        false,
        "varselOmRevurderingDeltBostedParagrafFjorten",
        "Varsel om revurdering delt bosted § 14"
    ),
    VARSEL_OM_REVURDERING_SAMBOER(
        false,
        "varselOmRevurderingSamboer",
        "Varsel om revurdering samboer"
    ),
    VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED(
        false,
        "varselOmVedtakEtterSoknadISED",
        "Varsel om vedtak etter søknad i SED"
    ),
    VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS(
        false,
        "varselOmRevurderingFraNasjonalTilEOS",
        "Varsel om revurdering fra nasjonal til EØS"
    ),

    SVARTIDSBREV(false, "svartidsbrev", "Svartidsbrev"),
    INFORMASJONSBREV_FØDSEL_MINDREÅRIG(
        false,
        "informasjonsbrevFodselMindreaarig",
        "Informasjonsbrev fødsel mindreårig"
    ),
    INFORMASJONSBREV_FØDSEL_UMYNDIG(false, "informasjonsbrevFodselUmyndig", "Informasjonsbrev fødsel umyndig"),
    INFORMASJONSBREV_KAN_SØKE(false, "informasjonsbrevKanSoke", "Informasjonsbrev kan søke"),
    INFORMASJONSBREV_FØDSEL_GENERELL(false, "informasjonsbrevFodselGenerell", "Informasjonsbrev fødsel generell"),
    INFORMASJONSBREV_KAN_SØKE_EØS(false, "informasjonsbrevKanSokeEOS", "Informasjonsbrev kan søke EØS"),

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
    AUTOVEDTAK_NYFØDT_BARN_FRA_FØR(true, "autovedtakNyfodtBarnFraFor", "Autovedtak nyfødt - barn fra før");

    fun skalGenerereForside(): Boolean =
        when (this) {
            INNHENTE_OPPLYSNINGER,
            VARSEL_OM_REVURDERING,
            VARSEL_OM_REVURDERING_DELT_BOSTED_PARAGRAF_14,
            VARSEL_OM_REVURDERING_SAMBOER -> true

            INFORMASJONSBREV_DELT_BOSTED,
            HENLEGGE_TRUKKET_SØKNAD,
            SVARTIDSBREV,
            INFORMASJONSBREV_FØDSEL_UMYNDIG,
            INFORMASJONSBREV_FØDSEL_MINDREÅRIG,
            INFORMASJONSBREV_KAN_SØKE,
            INFORMASJONSBREV_FØDSEL_GENERELL -> false

            else -> throw Feil("Ikke avgjort om $this skal generere forside")
        }

    fun tilDokumentType(): Dokumenttype =
        when (this) {
            INNHENTE_OPPLYSNINGER -> Dokumenttype.BARNETRYGD_INNHENTE_OPPLYSNINGER
            VARSEL_OM_REVURDERING -> Dokumenttype.BARNETRYGD_VARSEL_OM_REVURDERING
            VARSEL_OM_REVURDERING_DELT_BOSTED_PARAGRAF_14 -> Dokumenttype.BARNETRYGD_VARSEL_OM_REVURDERING_DELT_BOSTED_PARAGRAF_14
            VARSEL_OM_REVURDERING_SAMBOER -> Dokumenttype.BARNETRYGD_VARSEL_OM_REVURDERING_SAMBOER
            INFORMASJONSBREV_DELT_BOSTED -> Dokumenttype.BARNETRYGD_INFORMASJONSBREV_DELT_BOSTED
            HENLEGGE_TRUKKET_SØKNAD -> Dokumenttype.BARNETRYGD_HENLEGGE_TRUKKET_SØKNAD
            SVARTIDSBREV -> Dokumenttype.BARNETRYGD_SVARTIDSBREV
            INFORMASJONSBREV_FØDSEL_UMYNDIG -> Dokumenttype.BARNETRYGD_INFORMASJONSBREV_FØDSEL_UMYNDIG
            INFORMASJONSBREV_FØDSEL_MINDREÅRIG -> Dokumenttype.BARNETRYGD_INFORMASJONSBREV_FØDSEL_MINDREÅRIG
            INFORMASJONSBREV_KAN_SØKE -> Dokumenttype.BARNETRYGD_INFORMASJONSBREV_KAN_SØKE
            INFORMASJONSBREV_FØDSEL_GENERELL -> Dokumenttype.BARNETRYGD_INFORMASJONSBREV_FØDSEL_GENERELL
            else -> throw Feil("Ingen dokumenttype for $this")
        }

    fun setterBehandlingPåVent(): Boolean =
        when (this) {
            INNHENTE_OPPLYSNINGER,
            VARSEL_OM_REVURDERING,
            VARSEL_OM_REVURDERING_DELT_BOSTED_PARAGRAF_14 -> true
            else -> false
        }

    fun ventefristDager(): Long =
        when (this) {
            INNHENTE_OPPLYSNINGER,
            VARSEL_OM_REVURDERING,
            VARSEL_OM_REVURDERING_DELT_BOSTED_PARAGRAF_14 -> 3 * 7
            else -> throw Feil("Ventefrist ikke definert for brevtype $this")
        }

    fun venteårsak() =
        when (this) {
            INNHENTE_OPPLYSNINGER,
            VARSEL_OM_REVURDERING,
            VARSEL_OM_REVURDERING_DELT_BOSTED_PARAGRAF_14 -> SettPåVentÅrsak.AVVENTER_DOKUMENTASJON
            else -> throw Feil("Venteårsak ikke definert for brevtype $this")
        }
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
