package no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler

import no.nav.familie.ba.sak.common.erBostNummer
import no.nav.familie.ba.sak.common.erDnummer
import no.nav.familie.ba.sak.common.erFDatnummer
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.Filtreringsregel.Identifikator
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Evaluering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.EvalueringÅrsak
import java.time.temporal.ChronoUnit
import kotlin.math.abs

class Filtreringsregel<in T : FiltreringsreglerFakta>(
    val identifikator: Identifikator,
    private val erOppfylt: (T) -> Boolean,
) {
    fun evaluer(fakta: T): Evaluering {
        val evaluering = if (erOppfylt(fakta)) Evaluering.oppfylt(identifikator.oppfylt) else Evaluering.ikkeOppfylt(identifikator.ikkeOppfylt)
        return evaluering.copy(identifikator = identifikator.name)
    }

    /**
     * Navnet lagres i FILTRERING_RESULTAT.filtreringsregel og leses tilbake med valueOf, så det må ikke endres.
     * Utfallsnavn og beskrivelser lagres i evalueringsaarsaker og begrunnelse.
     */
    enum class Identifikator(
        oppfyltNavn: String,
        oppfyltBeskrivelse: String,
        ikkeOppfyltNavn: String,
        ikkeOppfyltBeskrivelse: String,
    ) {
        MOR_GYLDIG_FNR(
            oppfyltNavn = "MOR_HAR_GYLDIG_FNR",
            oppfyltBeskrivelse = "Mor har gyldig fødselsnummer",
            ikkeOppfyltNavn = "MOR_HAR_UGYLDIG_FNR",
            ikkeOppfyltBeskrivelse = "Mor har ugyldig fødselsnummer",
        ),
        BARN_GYLDIG_FNR(
            oppfyltNavn = "BARN_HAR_GYLDIG_FNR",
            oppfyltBeskrivelse = "Barn har gyldig fødselsnummer",
            ikkeOppfyltNavn = "BARN_HAR_UGYLDIG_FNR",
            ikkeOppfyltBeskrivelse = "Barn har ugyldig fødselsnummer",
        ),
        MOR_LEVER(
            oppfyltNavn = "MOR_LEVER",
            oppfyltBeskrivelse = "Det er ikke registrert dødsdato på mor.",
            ikkeOppfyltNavn = "MOR_LEVER_IKKE",
            ikkeOppfyltBeskrivelse = "Det er registrert dødsdato på mor.",
        ),
        BARN_LEVER(
            oppfyltNavn = "BARNET_LEVER",
            oppfyltBeskrivelse = "Det er ikke registrert dødsdato på barnet.",
            ikkeOppfyltNavn = "BARNET_LEVER_IKKE",
            ikkeOppfyltBeskrivelse = "Det er registrert dødsdato på barnet.",
        ),
        MER_ENN_5_MND_SIDEN_FORRIGE_BARN(
            oppfyltNavn = "MER_ENN_5_MND_SIDEN_FORRIGE_BARN_UTFALL",
            oppfyltBeskrivelse = "Det har gått mer enn fem måneder siden forrige barn ble født.",
            ikkeOppfyltNavn = "MINDRE_ENN_5_MND_SIDEN_FORRIGE_BARN_UTFALL",
            ikkeOppfyltBeskrivelse = "Det har gått mindre enn fem måneder siden forrige barn ble født.",
        ),
        MOR_ER_OVER_18_ÅR(
            oppfyltNavn = "MOR_ER_OVER_18_ÅR",
            oppfyltBeskrivelse = "Mor er over 18 år.",
            ikkeOppfyltNavn = "MOR_ER_UNDER_18_ÅR",
            ikkeOppfyltBeskrivelse = "Mor er under 18 år.",
        ),
        MOR_HAR_IKKE_VERGE(
            oppfyltNavn = "MOR_ER_MYNDIG",
            oppfyltBeskrivelse = "Mor er myndig.",
            ikkeOppfyltNavn = "MOR_ER_UNDER_VERGEMÅL",
            ikkeOppfyltBeskrivelse = "Mor er under vergemål.",
        ),
        MOR_MOTTAR_IKKE_LØPENDE_UTVIDET(
            oppfyltNavn = "MOR_MOTTAR_IKKE_LØPENDE_UTVIDET",
            oppfyltBeskrivelse = "Mor mottar ikke utvidet barnetrygd.",
            ikkeOppfyltNavn = "MOR_MOTTAR_LØPENDE_UTVIDET",
            ikkeOppfyltBeskrivelse = "Mor mottar utvidet barnetrygd.",
        ),
        MOR_HAR_IKKE_LØPENDE_EØS_BARNETRYGD(
            oppfyltNavn = "MOR_HAR_IKKE_LØPENDE_EØS_BARNETRYGD",
            oppfyltBeskrivelse = "Mor har ikke løpende EØS-barnetrygd",
            ikkeOppfyltNavn = "MOR_HAR_LØPENDE_EØS_BARNETRYGD",
            ikkeOppfyltBeskrivelse = "Mor har EØS-barnetrygd",
        ),
        FAGSAK_IKKE_MIGRERT_UT_AV_INFOTRYGD_ETTER_BARN_FØDT(
            oppfyltNavn = "FAGSAK_IKKE_MIGRERT_UT_AV_INFOTRYGD_ETTER_BARN_FØDT",
            oppfyltBeskrivelse = "Fagsaken har ikke blitt migrert fra infotrygd etter barn ble født.",
            ikkeOppfyltNavn = "FAGSAK_MIGRERT_UT_AV_INFOTRYGD_ETTER_BARN_FØDT",
            ikkeOppfyltBeskrivelse = "Fagsaken ble migrert fra infotrygd etter barn ble født.",
        ),
        LØPER_IKKE_BARNETRYGD_FOR_BARNET(
            oppfyltNavn = "LØPER_IKKE_BARNETRYGD_FOR_BARNET",
            oppfyltBeskrivelse = "Det løper ikke barnetrygd for barnet på annen forelder",
            ikkeOppfyltNavn = "LØPER_ALLEREDE_FOR_ANNEN_FORELDER",
            ikkeOppfyltBeskrivelse = "Annen mottaker har barnetrygd for barnet",
        ),
        MOR_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR_VED_FØDSELSDATO(
            oppfyltNavn = "MOR_OPPFYLLER_IKKE_VILKÅR_FOR_UTVIDET_BARNETRYGD_VED_FØDSELSDATO",
            oppfyltBeskrivelse = "Mor oppfyller ikke vilkår for utvidet barnetrygd",
            ikkeOppfyltNavn = "MOR_OPPFYLLER_VILKÅR_FOR_UTVIDET_BARNETRYGD_VED_FØDSELSDATO",
            ikkeOppfyltBeskrivelse = "Mor oppfyller vilkår for utvidet barnetrygd",
        ),
        MOR_HAR_IKKE_OPPHØRT_BARNETRYGD(
            oppfyltNavn = "MOR_HAR_IKKE_OPPHØRT_BARNETRYGD",
            oppfyltBeskrivelse = "Mor har ikke opphørt barnetrygd",
            ikkeOppfyltNavn = "MOR_HAR_OPPHØRT_BARNETRYGD",
            ikkeOppfyltBeskrivelse = "Mor har vedtak om opphørt barnetrygd.",
        ),
        SØKER_GYLDIG_FNR(
            oppfyltNavn = "SØKER_HAR_GYLDIG_FNR",
            oppfyltBeskrivelse = "Søker har gyldig fødselsnummer",
            ikkeOppfyltNavn = "SØKER_HAR_UGYLDIG_FNR",
            ikkeOppfyltBeskrivelse = "Søker har ugyldig fødselsnummer",
        ),
        SØKER_HAR_IKKE_D_NUMMER(
            oppfyltNavn = "SØKER_HAR_IKKE_D_NUMMER",
            oppfyltBeskrivelse = "Søker har ikke d-nummer",
            ikkeOppfyltNavn = "SØKER_HAR_D_NUMMER",
            ikkeOppfyltBeskrivelse = "Søker har d-nummer",
        ),
        SØKER_HAR_IKKE_ADRESSEBESKYTTELSE_GRADERING_6_ELLER_19(
            oppfyltNavn = "SØKER_HAR_IKKE_ADRESSEBESKYTTELSE_GRADERING_6_ELLER_19",
            oppfyltBeskrivelse = "Søker har ikke adressebeskyttelse gradering 6 eller 19",
            ikkeOppfyltNavn = "SØKER_HAR_ADRESSEBESKYTTELSE_GRADERING_6_ELLER_19",
            ikkeOppfyltBeskrivelse = "Søker har adressebeskyttelse gradering 6 eller 19",
        ),
        BARN_HAR_IKKE_D_NUMMER(
            oppfyltNavn = "BARN_HAR_IKKE_D_NUMMER",
            oppfyltBeskrivelse = "Barn har ikke d-nummer",
            ikkeOppfyltNavn = "BARN_HAR_D_NUMMER",
            ikkeOppfyltBeskrivelse = "Barn har d-nummer",
        ),
        BARN_HAR_IKKE_ADRESSEBESKYTTELSE_GRADERING_6_ELLER_19(
            oppfyltNavn = "BARN_HAR_IKKE_ADRESSEBESKYTTELSE_GRADERING_6_ELLER_19",
            oppfyltBeskrivelse = "Barn har ikke adressebeskyttelse gradering 6 eller 19",
            ikkeOppfyltNavn = "BARN_HAR_ADRESSEBESKYTTELSE_GRADERING_6_ELLER_19",
            ikkeOppfyltBeskrivelse = "Barn har adressebeskyttelse gradering 6 eller 19",
        ),
        SØKER_OG_BARN_HAR_FORELDER_BARN_RELASJON(
            oppfyltNavn = "SØKER_OG_BARN_HAR_FORELDER_BARN_RELASJON",
            oppfyltBeskrivelse = "Søker og barn har en forelder/barn-relasjon",
            ikkeOppfyltNavn = "SØKER_OG_BARN_HAR_IKKE_FORELDER_BARN_RELASJON",
            ikkeOppfyltBeskrivelse = "Søker og barn har ikke en forelder/barn-relasjon",
        ),
        SØKER_HAR_AKTIV_NORSK_BOSTEDSADRESSE(
            oppfyltNavn = "SØKER_HAR_AKTIV_NORSK_BOSTEDSADRESSE",
            oppfyltBeskrivelse = "Søker har aktiv norsk bostedsadresse per i dag",
            ikkeOppfyltNavn = "SØKER_HAR_IKKE_AKTIV_NORSK_BOSTEDSADRESSE",
            ikkeOppfyltBeskrivelse = "Søker har ikke aktiv norsk bostedsadresse per i dag",
        ),
        BARN_HAR_AKTIV_NORSK_BOSTEDSADRESSE(
            oppfyltNavn = "BARN_HAR_AKTIV_NORSK_BOSTEDSADRESSE",
            oppfyltBeskrivelse = "Barn har aktiv norsk bostedsadresse per i dag",
            ikkeOppfyltNavn = "BARN_HAR_IKKE_AKTIV_NORSK_BOSTEDSADRESSE",
            ikkeOppfyltBeskrivelse = "Barn har ikke aktiv norsk bostedsadresse per i dag",
        ),
        SØKER_ER_IKKE_UKRAINSK_STATSBORGER(
            oppfyltNavn = "SØKER_ER_IKKE_UKRAINSK_STATSBORGER",
            oppfyltBeskrivelse = "Søker er ikke ukrainsk statsborger",
            ikkeOppfyltNavn = "SØKER_ER_UKRAINSK_STATSBORGER",
            ikkeOppfyltBeskrivelse = "Søker er ukrainsk statsborger",
        ),
        BARN_ER_IKKE_UKRAINSK_STATSBORGER(
            oppfyltNavn = "BARN_ER_IKKE_UKRAINSK_STATSBORGER",
            oppfyltBeskrivelse = "Barn er ikke ukrainsk statsborger",
            ikkeOppfyltNavn = "BARN_ER_UKRAINSK_STATSBORGER",
            ikkeOppfyltBeskrivelse = "Barn er ukrainsk statsborger",
        ),
        SØKER_LEVER(
            oppfyltNavn = "SØKER_LEVER",
            oppfyltBeskrivelse = "Det er ikke registrert dødsdato på søker.",
            ikkeOppfyltNavn = "SØKER_LEVER_IKKE",
            ikkeOppfyltBeskrivelse = "Det er registrert dødsdato på søker.",
        ),
        SØKER_ER_OVER_18_ÅR(
            oppfyltNavn = "SØKER_ER_OVER_18_ÅR",
            oppfyltBeskrivelse = "Søker er over 18 år.",
            ikkeOppfyltNavn = "SØKER_ER_UNDER_18_ÅR",
            ikkeOppfyltBeskrivelse = "Søker er under 18 år.",
        ),
        SØKER_HAR_IKKE_VERGE(
            oppfyltNavn = "SØKER_ER_MYNDIG",
            oppfyltBeskrivelse = "Søker er myndig.",
            ikkeOppfyltNavn = "SØKER_ER_UNDER_VERGEMÅL",
            ikkeOppfyltBeskrivelse = "Søker er under vergemål.",
        ),
        SØKER_MOTTAR_IKKE_LØPENDE_UTVIDET(
            oppfyltNavn = "SØKER_MOTTAR_IKKE_LØPENDE_UTVIDET",
            oppfyltBeskrivelse = "Søker mottar ikke utvidet barnetrygd.",
            ikkeOppfyltNavn = "SØKER_MOTTAR_LØPENDE_UTVIDET",
            ikkeOppfyltBeskrivelse = "Søker mottar utvidet barnetrygd.",
        ),
        SØKER_HAR_IKKE_LØPENDE_EØS_BARNETRYGD(
            oppfyltNavn = "SØKER_HAR_IKKE_LØPENDE_EØS_BARNETRYGD",
            oppfyltBeskrivelse = "Søker har ikke løpende EØS-barnetrygd",
            ikkeOppfyltNavn = "SØKER_HAR_LØPENDE_EØS_BARNETRYGD",
            ikkeOppfyltBeskrivelse = "Søker har EØS-barnetrygd",
        ),
        SØKER_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR(
            oppfyltNavn = "SØKER_OPPFYLLER_IKKE_VILKÅR_FOR_UTVIDET_BARNETRYGD",
            oppfyltBeskrivelse = "Søker oppfyller ikke vilkår for utvidet barnetrygd",
            ikkeOppfyltNavn = "SØKER_OPPFYLLER_VILKÅR_FOR_UTVIDET_BARNETRYGD",
            ikkeOppfyltBeskrivelse = "Søker oppfyller vilkår for utvidet barnetrygd",
        ),
        UTBETALES_IKKE_BARNETRYGD_FOR_BARNET_TIL_ANNEN_MOTTAKER_INNEVÆRENDE_MÅNED(
            oppfyltNavn = "UTBETALES_IKKE_BARNETRYGD_FOR_BARNET_TIL_ANNEN_MOTTAKER_INNEVÆRENDE_MÅNED",
            oppfyltBeskrivelse = "Det utbetales ikke barnetrygd for barnet til annen mottaker i inneværende måned",
            ikkeOppfyltNavn = "UTBETALES_BARNETRYGD_FOR_BARNET_TIL_ANNEN_MOTTAKER_INNEVÆRENDE_MÅNED",
            ikkeOppfyltBeskrivelse = "Det utbetales barnetrygd for barnet til annen mottaker i inneværende måned",
        ),
        SØKER_HAR_IKKE_KRYSSET_PÅ_EØS_SPØRSMÅL_I_SØKNADEN(
            oppfyltNavn = "SØKER_HAR_IKKE_KRYSSET_PÅ_EØS_SPØRSMÅL_I_SØKNADEN",
            oppfyltBeskrivelse = "Søker har ikke krysset på EØS-spørsmål i søknaden",
            ikkeOppfyltNavn = "SØKER_HAR_KRYSSET_PÅ_EØS_SPØRSMÅL_I_SØKNADEN",
            ikkeOppfyltBeskrivelse = "Søker har krysset på EØS-spørsmål i søknaden",
        ),
        SØKER_HAR_IKKE_KRYSSET_FOR_DELT_BOSTED_I_SØKNADEN(
            oppfyltNavn = "SØKER_HAR_IKKE_KRYSSET_FOR_DELT_BOSTED_I_SØKNADEN",
            oppfyltBeskrivelse = "Søker har ikke krysset for delt bosted for noen av barna i søknaden",
            ikkeOppfyltNavn = "SØKER_HAR_KRYSSET_FOR_DELT_BOSTED_I_SØKNADEN",
            ikkeOppfyltBeskrivelse = "Søker har krysset for delt bosted for minst ett av barna i søknaden",
        ),
        SØKER_HAR_IKKE_KRYSSET_FOR_FOSTERHJEM_ELLER_BEREDSKAPSHJEM_I_SØKNADEN(
            oppfyltNavn = "SØKER_HAR_IKKE_KRYSSET_FOR_FOSTERHJEM_ELLER_BEREDSKAPSHJEM_I_SØKNADEN",
            oppfyltBeskrivelse = "Søker har ikke krysset for at noen av barna er i fosterhjem eller beredskapshjem i søknaden",
            ikkeOppfyltNavn = "SØKER_HAR_KRYSSET_FOR_FOSTERHJEM_ELLER_BEREDSKAPSHJEM_I_SØKNADEN",
            ikkeOppfyltBeskrivelse = "Søker har krysset for at minst ett av barna er i fosterhjem eller beredskapshjem i søknaden",
        ),
        SØKNADEN_INNEHOLDER_IKKE_VEDLEGG(
            oppfyltNavn = "SØKNADEN_INNEHOLDER_IKKE_VEDLEGG",
            oppfyltBeskrivelse = "Søknaden inneholder ikke vedlegg",
            ikkeOppfyltNavn = "SØKNADEN_INNEHOLDER_VEDLEGG",
            ikkeOppfyltBeskrivelse = "Søknaden inneholder vedlegg",
        ),
        ;

        val oppfylt: EvalueringÅrsak = Utfall(this, oppfyltNavn, oppfyltBeskrivelse)
        val ikkeOppfylt: EvalueringÅrsak = Utfall(this, ikkeOppfyltNavn, ikkeOppfyltBeskrivelse)

        private data class Utfall(
            private val identifikator: Identifikator,
            private val navn: String,
            private val beskrivelse: String,
        ) : EvalueringÅrsak {
            override fun hentBeskrivelse(): String = beskrivelse

            override fun hentMetrikkBeskrivelse(): String = beskrivelse

            override fun hentIdentifikator(): String = identifikator.name

            override fun hentNavn(): String = navn
        }
    }
}

private val morGyldigFnr =
    Filtreringsregel<FiltreringsreglerFakta>(Identifikator.MOR_GYLDIG_FNR) {
        erGyldigFnr(it.søker.aktør.aktivFødselsnummer())
    }
private val søkerGyldigFnr =
    Filtreringsregel<FiltreringsreglerFakta>(Identifikator.SØKER_GYLDIG_FNR) {
        erGyldigFnr(it.søker.aktør.aktivFødselsnummer())
    }

private val barnGyldigFnr =
    Filtreringsregel<FiltreringsreglerFakta>(Identifikator.BARN_GYLDIG_FNR) { fakta ->
        fakta.barnaSomSkalVurderes.all { erGyldigFnr(it.aktør.aktivFødselsnummer()) }
    }

private val morLever =
    Filtreringsregel<FiltreringsreglerFakta>(Identifikator.MOR_LEVER) { it.søkerLever }

private val søkerLever =
    Filtreringsregel<FiltreringsreglerFakta>(Identifikator.SØKER_LEVER) { it.søkerLever }

private val barnLever =
    Filtreringsregel<FiltreringsreglerFakta>(Identifikator.BARN_LEVER) { it.barnaLever }

private val merEnn5MndEllerMindreEnnFemDagerSidenForrigeBarn =
    Filtreringsregel<FiltreringsreglerFaktaFødselshendelse>(Identifikator.MER_ENN_5_MND_SIDEN_FORRIGE_BARN) { fakta ->
        fakta.barnaSomSkalVurderes.all { barnFraHendelse ->
            fakta.restenAvBarna.all {
                abs(ChronoUnit.MONTHS.between(barnFraHendelse.fødselsdato, it.fødselsdato)) > 5 ||
                    abs(ChronoUnit.DAYS.between(barnFraHendelse.fødselsdato, it.fødselsdato)) <= 6
            }
        }
    }

private val morErOver18År =
    Filtreringsregel<FiltreringsreglerFakta>(Identifikator.MOR_ER_OVER_18_ÅR) { it.søker.hentAlder() >= 18 }

private val søkerErOver18År =
    Filtreringsregel<FiltreringsreglerFakta>(Identifikator.SØKER_ER_OVER_18_ÅR) { it.søker.hentAlder() >= 18 }

private val morHarIkkeVerge =
    Filtreringsregel<FiltreringsreglerFakta>(Identifikator.MOR_HAR_IKKE_VERGE) { !it.søkerHarVerge }

private val søkerHarIkkeVerge =
    Filtreringsregel<FiltreringsreglerFakta>(Identifikator.SØKER_HAR_IKKE_VERGE) { !it.søkerHarVerge }

private val morMottarIkkeLøpendeUtvidet =
    Filtreringsregel<FiltreringsreglerFakta>(Identifikator.MOR_MOTTAR_IKKE_LØPENDE_UTVIDET) { !it.søkerMottarLøpendeUtvidet }

private val morHarIkkeLøpendeEøsBarnetrygd =
    Filtreringsregel<FiltreringsreglerFakta>(Identifikator.MOR_HAR_IKKE_LØPENDE_EØS_BARNETRYGD) { !it.søkerMottarEøsBarnetrygd }

private val søkerMottarIkkeLøpendeUtvidet =
    Filtreringsregel<FiltreringsreglerFakta>(Identifikator.SØKER_MOTTAR_IKKE_LØPENDE_UTVIDET) { !it.søkerMottarLøpendeUtvidet }

private val søkerHarIkkeLøpendeEøsBarnetrygd =
    Filtreringsregel<FiltreringsreglerFakta>(Identifikator.SØKER_HAR_IKKE_LØPENDE_EØS_BARNETRYGD) { !it.søkerMottarEøsBarnetrygd }

private val fagsakIkkeMigrertUtAvInfotrygdEtterBarnFødt =
    Filtreringsregel<FiltreringsreglerFaktaFødselshendelse>(Identifikator.FAGSAK_IKKE_MIGRERT_UT_AV_INFOTRYGD_ETTER_BARN_FØDT) {
        !it.erFagsakenMigrertEtterBarnFødt
    }

private val løperIkkeBarnetrygdForBarnet =
    Filtreringsregel<FiltreringsreglerFaktaFødselshendelse>(Identifikator.LØPER_IKKE_BARNETRYGD_FOR_BARNET) { !it.løperBarnetrygdForBarnetPåAnnenForelder }

private val utbetalesIkkeBarnetrygdForBarnetTilAnnenMottakerIInneværendeMåned =
    Filtreringsregel<FiltreringsreglerFaktaSøknad>(Identifikator.UTBETALES_IKKE_BARNETRYGD_FOR_BARNET_TIL_ANNEN_MOTTAKER_INNEVÆRENDE_MÅNED) {
        !it.utbetalesBarnetrygdForBarnetTilAnnenMottakerIInneværendeMåned
    }

private val søkerHarIkkeKryssetPåEøsSpørsmålISøknaden =
    Filtreringsregel<FiltreringsreglerFaktaSøknad>(Identifikator.SØKER_HAR_IKKE_KRYSSET_PÅ_EØS_SPØRSMÅL_I_SØKNADEN) {
        !it.søkerHarKryssetPåEøsSpørsmålISøknaden
    }

private val søkerHarIkkeKryssetForDeltBostedISøknaden =
    Filtreringsregel<FiltreringsreglerFaktaSøknad>(Identifikator.SØKER_HAR_IKKE_KRYSSET_FOR_DELT_BOSTED_I_SØKNADEN) {
        !it.søkerHarKryssetForDeltBostedISøknaden
    }

private val søkerHarIkkeKryssetForFosterhjemEllerBeredskapshjemISøknaden =
    Filtreringsregel<FiltreringsreglerFaktaSøknad>(Identifikator.SØKER_HAR_IKKE_KRYSSET_FOR_FOSTERHJEM_ELLER_BEREDSKAPSHJEM_I_SØKNADEN) {
        !it.søkerHarKryssetForFosterhjemEllerBeredskapshjemISøknaden
    }

private val søknadenInneholderIkkeVedlegg =
    Filtreringsregel<FiltreringsreglerFaktaSøknad>(Identifikator.SØKNADEN_INNEHOLDER_IKKE_VEDLEGG) {
        !it.søknadenInneholderVedlegg
    }

private val morHarIkkeOppfyltUtvidetVilkårVedFødselsdato =
    Filtreringsregel<FiltreringsreglerFakta>(Identifikator.MOR_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR_VED_FØDSELSDATO) {
        !it.søkerOppfyllerVilkårForUtvidetBarnetrygd
    }

private val søkerHarIkkeOppfyltUtvidetVilkår =
    Filtreringsregel<FiltreringsreglerFakta>(Identifikator.SØKER_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR) {
        !it.søkerOppfyllerVilkårForUtvidetBarnetrygd
    }

private val morHarIkkeOpphørtBarnetrygd =
    Filtreringsregel<FiltreringsreglerFaktaFødselshendelse>(Identifikator.MOR_HAR_IKKE_OPPHØRT_BARNETRYGD) { it.morHarIkkeOpphørtBarnetrygd }

private fun erGyldigFnr(personIdent: String): Boolean = !erBostNummer(personIdent) && !erFDatnummer(personIdent)

private val søkerHarIkkeDNummer =
    Filtreringsregel<FiltreringsreglerFakta>(Identifikator.SØKER_HAR_IKKE_D_NUMMER) {
        !erDnummer(it.søker.aktør.aktivFødselsnummer())
    }

private val barnHarIkkeDNummer =
    Filtreringsregel<FiltreringsreglerFakta>(Identifikator.BARN_HAR_IKKE_D_NUMMER) { fakta ->
        fakta.barnaSomSkalVurderes.all { !erDnummer(it.aktør.aktivFødselsnummer()) }
    }

private val søkerHarIkkeAdressebeskyttelseGradering6Eller19 =
    Filtreringsregel<FiltreringsreglerFaktaSøknad>(Identifikator.SØKER_HAR_IKKE_ADRESSEBESKYTTELSE_GRADERING_6_ELLER_19) {
        !it.søkerHarAdressebeskyttelseGradering6Eller19
    }

private val barnHarIkkeAdressebeskyttelseGradering6Eller19 =
    Filtreringsregel<FiltreringsreglerFaktaSøknad>(Identifikator.BARN_HAR_IKKE_ADRESSEBESKYTTELSE_GRADERING_6_ELLER_19) {
        !it.barnHarAdressebeskyttelseGradering6Eller19
    }

private val søkerOgBarnHarForelderBarnRelasjon =
    Filtreringsregel<FiltreringsreglerFaktaSøknad>(Identifikator.SØKER_OG_BARN_HAR_FORELDER_BARN_RELASJON) {
        it.søkerOgBarnHarForelderBarnRelasjon
    }

private val søkerHarAktivNorskBostedsadresse =
    Filtreringsregel<FiltreringsreglerFaktaSøknad>(Identifikator.SØKER_HAR_AKTIV_NORSK_BOSTEDSADRESSE) {
        it.søkerHarAktivNorskBostedsadresse
    }

private val barnHarAktivNorskBostedsadresse =
    Filtreringsregel<FiltreringsreglerFaktaSøknad>(Identifikator.BARN_HAR_AKTIV_NORSK_BOSTEDSADRESSE) {
        it.barnHarAktivNorskBostedsadresse
    }

private val søkerErIkkeUkrainskStatsborger =
    Filtreringsregel<FiltreringsreglerFaktaSøknad>(Identifikator.SØKER_ER_IKKE_UKRAINSK_STATSBORGER) {
        !it.søkerHarUkrainskStatsborgerskap
    }

private val barnErIkkeUkrainskStatsborger =
    Filtreringsregel<FiltreringsreglerFaktaSøknad>(Identifikator.BARN_ER_IKKE_UKRAINSK_STATSBORGER) {
        !it.barnHarUkrainskStatsborgerskap
    }

val FILTRERINGSREGLER_SØKNAD: List<Filtreringsregel<FiltreringsreglerFaktaSøknad>> =
    listOf(
        søkerHarIkkeDNummer,
        barnHarIkkeDNummer,
        søkerHarIkkeAdressebeskyttelseGradering6Eller19,
        barnHarIkkeAdressebeskyttelseGradering6Eller19,
        søkerGyldigFnr,
        barnGyldigFnr,
        søkerLever,
        barnLever,
        søkerErOver18År,
        søkerHarIkkeVerge,
        søkerMottarIkkeLøpendeUtvidet,
        søkerHarIkkeLøpendeEøsBarnetrygd,
        utbetalesIkkeBarnetrygdForBarnetTilAnnenMottakerIInneværendeMåned,
        søkerHarIkkeKryssetPåEøsSpørsmålISøknaden,
        søkerHarIkkeKryssetForDeltBostedISøknaden,
        søkerHarIkkeKryssetForFosterhjemEllerBeredskapshjemISøknaden,
        søknadenInneholderIkkeVedlegg,
        søkerHarAktivNorskBostedsadresse,
        barnHarAktivNorskBostedsadresse,
        søkerOgBarnHarForelderBarnRelasjon,
        søkerErIkkeUkrainskStatsborger,
        barnErIkkeUkrainskStatsborger,
        søkerHarIkkeOppfyltUtvidetVilkår,
    )

val FILTRERINGSREGLER_FØDSELSHENDELSE: List<Filtreringsregel<FiltreringsreglerFaktaFødselshendelse>> =
    listOf(
        morGyldigFnr,
        barnGyldigFnr,
        morLever,
        barnLever,
        merEnn5MndEllerMindreEnnFemDagerSidenForrigeBarn,
        morErOver18År,
        morHarIkkeVerge,
        morMottarIkkeLøpendeUtvidet,
        morHarIkkeLøpendeEøsBarnetrygd,
        fagsakIkkeMigrertUtAvInfotrygdEtterBarnFødt,
        løperIkkeBarnetrygdForBarnet,
        morHarIkkeOppfyltUtvidetVilkårVedFødselsdato,
        morHarIkkeOpphørtBarnetrygd,
    )
