package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Evaluering

/**
 * Et regelsett for en konkret undertype av [FiltreringsreglerFakta]. Ved å knytte hvert regelsett til
 * en spesifikk fakta-type sikrer kompilatoren at en regel aldri kan bli forsøkt vurdert med fakta den
 * ikke er skrevet for, uten behov for runtime-sjekker (f.eks. `when`/`throw Feil(...)`) inne i regelen.
 */
sealed interface Regelsett<T : FiltreringsreglerFakta> {
    val name: String
    val vurder: (T) -> Evaluering
}

enum class FiltreringsregelFødselshendelse(
    override val vurder: (FiltreringsreglerFaktaFødselshendelse) -> Evaluering,
) : Regelsett<FiltreringsreglerFaktaFødselshendelse> {
    MOR_GYLDIG_FNR(vurder = { FiltreringsregelEvaluering.harSøkerGyldigFnr(it) }),
    BARN_GYLDIG_FNR(vurder = { FiltreringsregelEvaluering.barnHarGyldigFnr(it) }),
    MOR_LEVER(vurder = { FiltreringsregelEvaluering.søkerLever(it) }),
    BARN_LEVER(vurder = { FiltreringsregelEvaluering.barnLever(it) }),
    MER_ENN_5_MND_SIDEN_FORRIGE_BARN(vurder = {
        FiltreringsregelEvaluering.merEnn5mndEllerMindreEnnFemDagerSidenForrigeBarn(it)
    }),
    MOR_ER_OVER_18_ÅR(vurder = { FiltreringsregelEvaluering.erSøkerOver18år(it) }),
    MOR_HAR_IKKE_VERGE(vurder = { FiltreringsregelEvaluering.søkerHarIkkeVerge(it) }),
    MOR_MOTTAR_IKKE_LØPENDE_UTVIDET(vurder = { FiltreringsregelEvaluering.søkerMottarIkkeLøpendeUtvidet(it) }),
    MOR_HAR_IKKE_LØPENDE_EØS_BARNETRYGD(vurder = { FiltreringsregelEvaluering.søkerHarIkkeLøpendeEøsBarnetrygd(it) }),
    FAGSAK_IKKE_MIGRERT_UT_AV_INFOTRYGD_ETTER_BARN_FØDT(vurder = {
        FiltreringsregelEvaluering.fagsakIkkeMigrertEtterBarnBleFødt(it)
    }),
    LØPER_IKKE_BARNETRYGD_FOR_BARNET(vurder = { FiltreringsregelEvaluering.løperIkkeBarnetrygdPåAnnenForelder(it) }),
    MOR_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR_VED_FØDSELSDATO(vurder = {
        FiltreringsregelEvaluering.søkerOppfyllerIkkeVilkårForUtvidetBarnetrygd(it)
    }),
    MOR_HAR_IKKE_OPPHØRT_BARNETRYGD(vurder = { FiltreringsregelEvaluering.morHarIkkeOpphørtBarnetrygd(it) }),
    ;

    init {
        // Sikrer at hvert regelsett-navn faktisk finnes som identifikator, se Filtreringsregel.kt.
        Filtreringsregel.valueOf(name)
    }
}

enum class FiltreringsregelSøknad(
    override val vurder: (FiltreringsreglerFaktaSøknad) -> Evaluering,
) : Regelsett<FiltreringsreglerFaktaSøknad> {
    MOR_GYLDIG_FNR(vurder = { FiltreringsregelEvaluering.harSøkerGyldigFnr(it) }),
    BARN_GYLDIG_FNR(vurder = { FiltreringsregelEvaluering.barnHarGyldigFnr(it) }),
    MOR_LEVER(vurder = { FiltreringsregelEvaluering.søkerLever(it) }),
    BARN_LEVER(vurder = { FiltreringsregelEvaluering.barnLever(it) }),
    MOR_ER_OVER_18_ÅR(vurder = { FiltreringsregelEvaluering.erSøkerOver18år(it) }),
    MOR_HAR_IKKE_VERGE(vurder = { FiltreringsregelEvaluering.søkerHarIkkeVerge(it) }),
    MOR_MOTTAR_IKKE_LØPENDE_UTVIDET(vurder = { FiltreringsregelEvaluering.søkerMottarIkkeLøpendeUtvidet(it) }),
    MOR_HAR_IKKE_LØPENDE_EØS_BARNETRYGD(vurder = { FiltreringsregelEvaluering.søkerHarIkkeLøpendeEøsBarnetrygd(it) }),
    LØPER_IKKE_BARNETRYGD_FOR_BARNET(vurder = { FiltreringsregelEvaluering.løperIkkeBarnetrygdPåAnnenForelder(it) }),
    MOR_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR_VED_FØDSELSDATO(vurder = {
        FiltreringsregelEvaluering.søkerOppfyllerIkkeVilkårForUtvidetBarnetrygd(it)
    }),
    ;

    init {
        Filtreringsregel.valueOf(name)
    }
}
