package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.regelsett

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.Filtreringsregel
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.FiltreringsregelEvaluering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.FiltreringsreglerFaktaFødselshendelse

val REGELSETT_FØDSELSHENDELSE: List<Regelsett<FiltreringsreglerFaktaFødselshendelse>> =
    listOf(
        Regelsett(Filtreringsregel.MOR_GYLDIG_FNR) {
            FiltreringsregelEvaluering.harSøkerGyldigFnr(it)
        },
        Regelsett(Filtreringsregel.BARN_GYLDIG_FNR) {
            FiltreringsregelEvaluering.barnHarGyldigFnr(it)
        },
        Regelsett(Filtreringsregel.MOR_LEVER) {
            FiltreringsregelEvaluering.søkerLever(it)
        },
        Regelsett(Filtreringsregel.BARN_LEVER) {
            FiltreringsregelEvaluering.barnLever(it)
        },
        Regelsett(Filtreringsregel.MER_ENN_5_MND_SIDEN_FORRIGE_BARN) {
            FiltreringsregelEvaluering.merEnn5mndEllerMindreEnnFemDagerSidenForrigeBarn(it)
        },
        Regelsett(Filtreringsregel.MOR_ER_OVER_18_ÅR) {
            FiltreringsregelEvaluering.erSøkerOver18år(it)
        },
        Regelsett(Filtreringsregel.MOR_HAR_IKKE_VERGE) {
            FiltreringsregelEvaluering.søkerHarIkkeVerge(it)
        },
        Regelsett(Filtreringsregel.MOR_MOTTAR_IKKE_LØPENDE_UTVIDET) {
            FiltreringsregelEvaluering.søkerMottarIkkeLøpendeUtvidet(it)
        },
        Regelsett(Filtreringsregel.MOR_HAR_IKKE_LØPENDE_EØS_BARNETRYGD) {
            FiltreringsregelEvaluering.søkerHarIkkeLøpendeEøsBarnetrygd(it)
        },
        Regelsett(Filtreringsregel.FAGSAK_IKKE_MIGRERT_UT_AV_INFOTRYGD_ETTER_BARN_FØDT) {
            FiltreringsregelEvaluering.fagsakIkkeMigrertEtterBarnBleFødt(it)
        },
        Regelsett(Filtreringsregel.LØPER_IKKE_BARNETRYGD_FOR_BARNET) {
            FiltreringsregelEvaluering.løperIkkeBarnetrygdPåAnnenForelder(it)
        },
        Regelsett(Filtreringsregel.MOR_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR_VED_FØDSELSDATO) {
            FiltreringsregelEvaluering.søkerOppfyllerIkkeVilkårForUtvidetBarnetrygd(it)
        },
        Regelsett(Filtreringsregel.MOR_HAR_IKKE_OPPHØRT_BARNETRYGD) {
            FiltreringsregelEvaluering.morHarIkkeOpphørtBarnetrygd(it)
        },
    )