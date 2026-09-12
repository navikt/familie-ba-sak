package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Evaluering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.EvalueringÅrsak
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.Filtreringsregel.Identifikator
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import java.time.temporal.ChronoUnit
import kotlin.math.abs

class Filtreringsregel<T : FiltreringsreglerFakta>(
    val identifikator: Identifikator,
    private val erOppfylt: (T) -> Boolean,
) {
    fun evaluer(fakta: T): Evaluering = identifikator.tilEvaluering(erOppfylt(fakta))

    enum class Identifikator(
        private val oppfylt: Utfall,
        private val ikkeOppfylt: Utfall,
    ) {
        MOR_GYLDIG_FNR(
            Utfall("MOR_HAR_GYLDIG_FNR", "Mor har gyldig fødselsnummer"),
            Utfall("MOR_HAR_UGYLDIG_FNR", "Mor har ugyldig fødselsnummer"),
        ),
        BARN_GYLDIG_FNR(
            Utfall("BARN_HAR_GYLDIG_FNR", "Barn har gyldig fødselsnummer"),
            Utfall("BARN_HAR_UGYLDIG_FNR", "Barn har ugyldig fødselsnummer"),
        ),
        MOR_LEVER(
            Utfall("MOR_LEVER", "Det er ikke registrert dødsdato på mor."),
            Utfall("MOR_LEVER_IKKE", "Det er registrert dødsdato på mor."),
        ),
        BARN_LEVER(
            Utfall("BARNET_LEVER", "Det er ikke registrert dødsdato på barnet."),
            Utfall("BARNET_LEVER_IKKE", "Det er registrert dødsdato på barnet."),
        ),
        MER_ENN_5_MND_SIDEN_FORRIGE_BARN(
            Utfall("MER_ENN_5_MND_SIDEN_FORRIGE_BARN_UTFALL", "Det har gått mer enn fem måneder siden forrige barn ble født."),
            Utfall("MINDRE_ENN_5_MND_SIDEN_FORRIGE_BARN_UTFALL", "Det har gått mindre enn fem måneder siden forrige barn ble født."),
        ),
        MOR_ER_OVER_18_ÅR(
            Utfall("MOR_ER_OVER_18_ÅR", "Mor er over 18 år."),
            Utfall("MOR_ER_UNDER_18_ÅR", "Mor er under 18 år."),
        ),
        MOR_HAR_IKKE_VERGE(
            Utfall("MOR_ER_MYNDIG", "Mor er myndig."),
            Utfall("MOR_ER_UNDER_VERGEMÅL", "Mor er under vergemål."),
        ),
        MOR_MOTTAR_IKKE_LØPENDE_UTVIDET(
            Utfall("MOR_MOTTAR_IKKE_LØPENDE_UTVIDET", "Mor mottar ikke utvidet barnetrygd."),
            Utfall("MOR_MOTTAR_LØPENDE_UTVIDET", "Mor mottar utvidet barnetrygd."),
        ),
        MOR_HAR_IKKE_LØPENDE_EØS_BARNETRYGD(
            Utfall("MOR_HAR_IKKE_LØPENDE_EØS_BARNETRYGD", "Mor har ikke løpende EØS-barnetrygd"),
            Utfall("MOR_HAR_LØPENDE_EØS_BARNETRYGD", "Mor har EØS-barnetrygd"),
        ),
        FAGSAK_IKKE_MIGRERT_UT_AV_INFOTRYGD_ETTER_BARN_FØDT(
            Utfall("FAGSAK_IKKE_MIGRERT_UT_AV_INFOTRYGD_ETTER_BARN_FØDT", "Fagsaken har ikke blitt migrert fra infotrygd etter barn ble født."),
            Utfall("FAGSAK_MIGRERT_UT_AV_INFOTRYGD_ETTER_BARN_FØDT", "Fagsaken ble migrert fra infotrygd etter barn ble født."),
        ),
        LØPER_IKKE_BARNETRYGD_FOR_BARNET(
            Utfall("LØPER_IKKE_BARNETRYGD_FOR_BARNET", "Det løper ikke barnetrygd for barnet på annen forelder"),
            Utfall("LØPER_ALLEREDE_FOR_ANNEN_FORELDER", "Annen mottaker har barnetrygd for barnet"),
        ),
        MOR_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR_VED_FØDSELSDATO(
            Utfall("MOR_OPPFYLLER_IKKE_VILKÅR_FOR_UTVIDET_BARNETRYGD_VED_FØDSELSDATO", "Mor oppfyller ikke vilkår for utvidet barnetrygd"),
            Utfall("MOR_OPPFYLLER_VILKÅR_FOR_UTVIDET_BARNETRYGD_VED_FØDSELSDATO", "Mor oppfyller vilkår for utvidet barnetrygd"),
        ),
        MOR_HAR_IKKE_OPPHØRT_BARNETRYGD(
            Utfall("MOR_HAR_IKKE_OPPHØRT_BARNETRYGD", "Mor har ikke opphørt barnetrygd"),
            Utfall("MOR_HAR_OPPHØRT_BARNETRYGD", "Mor har vedtak om opphørt barnetrygd."),
        ),
        ;

        fun tilEvaluering(erOppfylt: Boolean): Evaluering = (if (erOppfylt) Evaluering.oppfylt(oppfylt) else Evaluering.ikkeOppfylt(ikkeOppfylt)).copy(identifikator = name)

        fun tilIkkeVurdertEvaluering(): Evaluering =
            Evaluering(
                resultat = Resultat.IKKE_VURDERT,
                evalueringÅrsaker = emptyList(),
                begrunnelse = "Ikke vurdert",
                identifikator = name,
            )

        /**
         * [navn] lagres i FILTRERING_RESULTAT.evalueringsaarsaker og [beskrivelse] i FILTRERING_RESULTAT.begrunnelse, og må ikke endres.
         */
        private class Utfall(
            private val navn: String,
            private val beskrivelse: String,
        ) : EvalueringÅrsak {
            override fun hentBeskrivelse(): String = beskrivelse

            override fun hentMetrikkBeskrivelse(): String = beskrivelse

            override fun hentIdentifikator(): String = entries.single { it.oppfylt === this || it.ikkeOppfylt === this }.name

            override fun hentNavn(): String = navn

            override fun toString(): String = navn
        }
    }
}

val FILTRERINGSREGLER_SØKNAD: List<Filtreringsregel<FiltreringsreglerFaktaSøknad>> =
    listOf(
        Filtreringsregel(Identifikator.MOR_GYLDIG_FNR) { it.søker.harGyldigFnr() },
        Filtreringsregel(Identifikator.BARN_GYLDIG_FNR) { it.barnaSomSkalVurderes.all(Person::harGyldigFnr) },
        Filtreringsregel(Identifikator.MOR_LEVER) { it.søkerLever },
        Filtreringsregel(Identifikator.BARN_LEVER) { it.barnaLever },
        Filtreringsregel(Identifikator.MOR_ER_OVER_18_ÅR) { it.søker.hentAlder() >= 18 },
        Filtreringsregel(Identifikator.MOR_HAR_IKKE_VERGE) { !it.søkerHarVerge },
        Filtreringsregel(Identifikator.MOR_MOTTAR_IKKE_LØPENDE_UTVIDET) { !it.søkerMottarLøpendeUtvidet },
        Filtreringsregel(Identifikator.MOR_HAR_IKKE_LØPENDE_EØS_BARNETRYGD) { !it.søkerMottarEøsBarnetrygd },
        Filtreringsregel(Identifikator.LØPER_IKKE_BARNETRYGD_FOR_BARNET) { !it.løperBarnetrygdForBarnetPåAnnenForelder },
        Filtreringsregel(Identifikator.MOR_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR_VED_FØDSELSDATO) { !it.søkerOppfyllerVilkårForUtvidetBarnetrygd },
    )

val FILTRERINGSREGLER_FØDSELSHENDELSE: List<Filtreringsregel<FiltreringsreglerFaktaFødselshendelse>> =
    listOf(
        Filtreringsregel(Identifikator.MOR_GYLDIG_FNR) { it.søker.harGyldigFnr() },
        Filtreringsregel(Identifikator.BARN_GYLDIG_FNR) { it.barnaSomSkalVurderes.all(Person::harGyldigFnr) },
        Filtreringsregel(Identifikator.MOR_LEVER) { it.søkerLever },
        Filtreringsregel(Identifikator.BARN_LEVER) { it.barnaLever },
        Filtreringsregel(Identifikator.MER_ENN_5_MND_SIDEN_FORRIGE_BARN) { it.erMerEnn5MndEllerUnderEnUkeSidenForrigeBarn() },
        Filtreringsregel(Identifikator.MOR_ER_OVER_18_ÅR) { it.søker.hentAlder() >= 18 },
        Filtreringsregel(Identifikator.MOR_HAR_IKKE_VERGE) { !it.søkerHarVerge },
        Filtreringsregel(Identifikator.MOR_MOTTAR_IKKE_LØPENDE_UTVIDET) { !it.søkerMottarLøpendeUtvidet },
        Filtreringsregel(Identifikator.MOR_HAR_IKKE_LØPENDE_EØS_BARNETRYGD) { !it.søkerMottarEøsBarnetrygd },
        Filtreringsregel(Identifikator.FAGSAK_IKKE_MIGRERT_UT_AV_INFOTRYGD_ETTER_BARN_FØDT) { !it.erFagsakenMigrertEtterBarnFødt },
        Filtreringsregel(Identifikator.LØPER_IKKE_BARNETRYGD_FOR_BARNET) { !it.løperBarnetrygdForBarnetPåAnnenForelder },
        Filtreringsregel(Identifikator.MOR_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR_VED_FØDSELSDATO) { !it.søkerOppfyllerVilkårForUtvidetBarnetrygd },
        Filtreringsregel(Identifikator.MOR_HAR_IKKE_OPPHØRT_BARNETRYGD) { it.morHarIkkeOpphørtBarnetrygd },
    )

/**
 * FDAT-nummer har 00000 som individ- og kontrollsiffer, BOST-nummer har måned 21–32.
 */
private fun Person.harGyldigFnr(): Boolean {
    val fødselsnummer = aktør.aktivFødselsnummer()
    return fødselsnummer.substring(6).toInt() != 0 && fødselsnummer.substring(2, 4).toInt() !in 21..32
}

private fun FiltreringsreglerFaktaFødselshendelse.erMerEnn5MndEllerUnderEnUkeSidenForrigeBarn(): Boolean =
    barnaSomSkalVurderes.all { barnFraHendelse ->
        restenAvBarna.all {
            abs(ChronoUnit.MONTHS.between(barnFraHendelse.fødselsdato, it.fødselsdato)) > 5 ||
                abs(ChronoUnit.DAYS.between(barnFraHendelse.fødselsdato, it.fødselsdato)) <= 6
        }
    }
