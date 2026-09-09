package no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Evaluering

data class Filtreringsregel<T : FiltreringsreglerFakta>(
    val identifikator: Identifikator,
    val evaluer: (T) -> Evaluering,
) {
    enum class Identifikator {
        MOR_GYLDIG_FNR,
        BARN_GYLDIG_FNR,
        MOR_LEVER,
        BARN_LEVER,
        MER_ENN_5_MND_SIDEN_FORRIGE_BARN,
        MOR_ER_OVER_18_ÅR,
        MOR_HAR_IKKE_VERGE,
        MOR_MOTTAR_IKKE_LØPENDE_UTVIDET,
        MOR_HAR_IKKE_LØPENDE_EØS_BARNETRYGD,
        FAGSAK_IKKE_MIGRERT_UT_AV_INFOTRYGD_ETTER_BARN_FØDT,
        LØPER_IKKE_BARNETRYGD_FOR_BARNET,
        MOR_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR_VED_FØDSELSDATO,
        MOR_HAR_IKKE_OPPHØRT_BARNETRYGD,
    }
}

val FILTRERINGSREGLER_SØKNAD: List<Filtreringsregel<FiltreringsreglerFaktaSøknad>> =
    listOf(
        Filtreringsregel(Filtreringsregel.Identifikator.MOR_GYLDIG_FNR) {
            FiltreringsregelEvaluering.harSøkerGyldigFnr(it)
        },
        Filtreringsregel(Filtreringsregel.Identifikator.BARN_GYLDIG_FNR) {
            FiltreringsregelEvaluering.barnHarGyldigFnr(it)
        },
        Filtreringsregel(Filtreringsregel.Identifikator.MOR_LEVER) {
            FiltreringsregelEvaluering.søkerLever(it)
        },
        Filtreringsregel(Filtreringsregel.Identifikator.BARN_LEVER) {
            FiltreringsregelEvaluering.barnLever(it)
        },
        Filtreringsregel(Filtreringsregel.Identifikator.MOR_ER_OVER_18_ÅR) {
            FiltreringsregelEvaluering.erSøkerOver18år(it)
        },
        Filtreringsregel(Filtreringsregel.Identifikator.MOR_HAR_IKKE_VERGE) {
            FiltreringsregelEvaluering.søkerHarIkkeVerge(it)
        },
        Filtreringsregel(Filtreringsregel.Identifikator.MOR_MOTTAR_IKKE_LØPENDE_UTVIDET) {
            FiltreringsregelEvaluering.søkerMottarIkkeLøpendeUtvidet(it)
        },
        Filtreringsregel(Filtreringsregel.Identifikator.MOR_HAR_IKKE_LØPENDE_EØS_BARNETRYGD) {
            FiltreringsregelEvaluering.søkerHarIkkeLøpendeEøsBarnetrygd(it)
        },
        Filtreringsregel(Filtreringsregel.Identifikator.LØPER_IKKE_BARNETRYGD_FOR_BARNET) {
            FiltreringsregelEvaluering.løperIkkeBarnetrygdPåAnnenForelder(it)
        },
        Filtreringsregel(Filtreringsregel.Identifikator.MOR_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR_VED_FØDSELSDATO) {
            FiltreringsregelEvaluering.søkerOppfyllerIkkeVilkårForUtvidetBarnetrygd(it)
        },
    )

val FILTRERINGSREGLER_FØDSELSHENDELSE: List<Filtreringsregel<FiltreringsreglerFaktaFødselshendelse>> =
    listOf(
        Filtreringsregel(Filtreringsregel.Identifikator.MOR_GYLDIG_FNR) {
            FiltreringsregelEvaluering.harSøkerGyldigFnr(it)
        },
        Filtreringsregel(Filtreringsregel.Identifikator.BARN_GYLDIG_FNR) {
            FiltreringsregelEvaluering.barnHarGyldigFnr(it)
        },
        Filtreringsregel(Filtreringsregel.Identifikator.MOR_LEVER) {
            FiltreringsregelEvaluering.søkerLever(it)
        },
        Filtreringsregel(Filtreringsregel.Identifikator.BARN_LEVER) {
            FiltreringsregelEvaluering.barnLever(it)
        },
        Filtreringsregel(Filtreringsregel.Identifikator.MER_ENN_5_MND_SIDEN_FORRIGE_BARN) {
            FiltreringsregelEvaluering.merEnn5mndEllerMindreEnnFemDagerSidenForrigeBarn(it)
        },
        Filtreringsregel(Filtreringsregel.Identifikator.MOR_ER_OVER_18_ÅR) {
            FiltreringsregelEvaluering.erSøkerOver18år(it)
        },
        Filtreringsregel(Filtreringsregel.Identifikator.MOR_HAR_IKKE_VERGE) {
            FiltreringsregelEvaluering.søkerHarIkkeVerge(it)
        },
        Filtreringsregel(Filtreringsregel.Identifikator.MOR_MOTTAR_IKKE_LØPENDE_UTVIDET) {
            FiltreringsregelEvaluering.søkerMottarIkkeLøpendeUtvidet(it)
        },
        Filtreringsregel(Filtreringsregel.Identifikator.MOR_HAR_IKKE_LØPENDE_EØS_BARNETRYGD) {
            FiltreringsregelEvaluering.søkerHarIkkeLøpendeEøsBarnetrygd(it)
        },
        Filtreringsregel(Filtreringsregel.Identifikator.FAGSAK_IKKE_MIGRERT_UT_AV_INFOTRYGD_ETTER_BARN_FØDT) {
            FiltreringsregelEvaluering.fagsakIkkeMigrertEtterBarnBleFødt(it)
        },
        Filtreringsregel(Filtreringsregel.Identifikator.LØPER_IKKE_BARNETRYGD_FOR_BARNET) {
            FiltreringsregelEvaluering.løperIkkeBarnetrygdPåAnnenForelder(it)
        },
        Filtreringsregel(Filtreringsregel.Identifikator.MOR_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR_VED_FØDSELSDATO) {
            FiltreringsregelEvaluering.søkerOppfyllerIkkeVilkårForUtvidetBarnetrygd(it)
        },
        Filtreringsregel(Filtreringsregel.Identifikator.MOR_HAR_IKKE_OPPHØRT_BARNETRYGD) {
            FiltreringsregelEvaluering.morHarIkkeOpphørtBarnetrygd(it)
        },
    )
