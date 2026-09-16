package no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler

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
        SØKER_HAR_HAR_IKKE_D_NUMMER(
            oppfyltNavn = "SØKER_HAR_IKKE_D_NUMMER",
            oppfyltBeskrivelse = "Søker har ikke d-nummer",
            ikkeOppfyltNavn = "SØKER_HAR_D_NUMMER",
            ikkeOppfyltBeskrivelse = "Søker har d-nummer",
        ),
        BARN_HAR_HAR_IKKE_D_NUMMER(
            oppfyltNavn = "BARN_HAR_IKKE_D_NUMMER",
            oppfyltBeskrivelse = "Barn har ikke d-nummer",
            ikkeOppfyltNavn = "BARN_HAR_D_NUMMER",
            ikkeOppfyltBeskrivelse = "Barn har d-nummer",
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
            oppfyltNavn = "Søker_ER_MYNDIG",
            oppfyltBeskrivelse = "Søker er myndig.",
            ikkeOppfyltNavn = "SØKER_ER_UNDER_VERGEMÅL",
            ikkeOppfyltBeskrivelse = "Søker er under vergemål.",
        ),
        SØKER_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR(
            oppfyltNavn = "SØKER_OPPFYLLER_IKKE_VILKÅR_FOR_UTVIDET_BARNETRYGD",
            oppfyltBeskrivelse = "Søker oppfyller ikke vilkår for utvidet barnetrygd",
            ikkeOppfyltNavn = "SØKER_OPPFYLLER_VILKÅR_FOR_UTVIDET_BARNETRYGD_VED",
            ikkeOppfyltBeskrivelse = "Søker oppfyller vilkår for utvidet barnetrygd",
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

private val fagsakIkkeMigrertUtAvInfotrygdEtterBarnFødt =
    Filtreringsregel<FiltreringsreglerFaktaFødselshendelse>(Identifikator.FAGSAK_IKKE_MIGRERT_UT_AV_INFOTRYGD_ETTER_BARN_FØDT) {
        !it.erFagsakenMigrertEtterBarnFødt
    }

private val løperIkkeBarnetrygdForBarnet =
    Filtreringsregel<FiltreringsreglerFakta>(Identifikator.LØPER_IKKE_BARNETRYGD_FOR_BARNET) { !it.løperBarnetrygdForBarnetPåAnnenForelder }

private val morHarIkkeOppfyltUtvidetVilkårVedFødselsdato =
    Filtreringsregel<FiltreringsreglerFakta>(Identifikator.MOR_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR_VED_FØDSELSDATO) {
        !it.søkerOppfyllerVilkårForUtvidetBarnetrygd
    }

private val morHarIkkeOpphørtBarnetrygd =
    Filtreringsregel<FiltreringsreglerFaktaFødselshendelse>(Identifikator.MOR_HAR_IKKE_OPPHØRT_BARNETRYGD) { it.morHarIkkeOpphørtBarnetrygd }

private fun erGyldigFnr(personIdent: String): Boolean = !erBostNummer(personIdent) && !erFDatnummer(personIdent)

private fun erFDatnummer(personIdent: String): Boolean = personIdent.substring(6).toInt() == 0

private fun erIkkeDNummer(personIdent: String): Boolean = personIdent.substring(0, 1).toInt() != 4

private val søkerHarIkkeDNummer =
    Filtreringsregel<FiltreringsreglerFakta>(Identifikator.SØKER_HAR_HAR_IKKE_D_NUMMER) {
        erIkkeDNummer(it.søker.aktør.aktivFødselsnummer())
    }

private val barnHarIkkeDNummer =
    Filtreringsregel<FiltreringsreglerFakta>(Identifikator.BARN_GYLDIG_FNR) { fakta ->
        fakta.barnaSomSkalVurderes.all { erIkkeDNummer(it.aktør.aktivFødselsnummer()) }
    }

/**
 * BOST-nr har måned mellom 21 og 32
 */
private fun erBostNummer(personIdent: String): Boolean = personIdent.substring(2, 4).toInt() in 21..32

val FILTRERINGSREGLER_SØKNAD: List<Filtreringsregel<FiltreringsreglerFaktaSøknad>> =
    listOf(
        søkerHarIkkeDNummer,
        barnHarIkkeDNummer,
        søkerGyldigFnr,
        barnGyldigFnr,
        søkerLever,
        barnLever,
        søkerErOver18År,
        søkerHarIkkeVerge,
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
