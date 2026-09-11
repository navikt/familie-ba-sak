package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Evaluering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.utfall.FiltreringsregelIkkeOppfylt
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.utfall.FiltreringsregelOppfylt
import java.time.temporal.ChronoUnit
import kotlin.math.abs

object FiltreringsregelEvaluering {
    fun harSøkerGyldigFnr(fakta: FiltreringsreglerFakta): Evaluering {
        val harSøkerGyldigFnr =
            (!erBostNummer(fakta.søker.aktør.aktivFødselsnummer()) && !erFDatnummer(fakta.søker.aktør.aktivFødselsnummer()))

        return if (harSøkerGyldigFnr) {
            Evaluering.oppfylt(FiltreringsregelOppfylt.MOR_HAR_GYLDIG_FNR)
        } else {
            Evaluering.ikkeOppfylt(
                FiltreringsregelIkkeOppfylt.MOR_HAR_UGYLDIG_FNR,
            )
        }
    }

    fun barnHarGyldigFnr(fakta: FiltreringsreglerFakta): Evaluering {
        val erbarnFnrGyldig =
            fakta.barnaSomSkalVurderes.all { (!erBostNummer(it.aktør.aktivFødselsnummer()) && !erFDatnummer(it.aktør.aktivFødselsnummer())) }

        return if (erbarnFnrGyldig) {
            Evaluering.oppfylt(FiltreringsregelOppfylt.BARN_HAR_GYLDIG_FNR)
        } else {
            Evaluering.ikkeOppfylt(
                FiltreringsregelIkkeOppfylt.BARN_HAR_UGYLDIG_FNR,
            )
        }
    }

    fun erSøkerOver18år(fakta: FiltreringsreglerFakta): Evaluering =
        if (fakta.søker.hentAlder() >= 18) {
            Evaluering.oppfylt(
                FiltreringsregelOppfylt.MOR_ER_OVER_18_ÅR,
            )
        } else {
            Evaluering.ikkeOppfylt(FiltreringsregelIkkeOppfylt.MOR_ER_UNDER_18_ÅR)
        }

    fun søkerLever(fakta: FiltreringsreglerFakta): Evaluering =
        if (fakta.søkerLever) {
            Evaluering.oppfylt(FiltreringsregelOppfylt.MOR_LEVER)
        } else {
            Evaluering.ikkeOppfylt(
                FiltreringsregelIkkeOppfylt.MOR_LEVER_IKKE,
            )
        }

    fun barnLever(fakta: FiltreringsreglerFakta): Evaluering =
        if (fakta.barnaLever) {
            Evaluering.oppfylt(FiltreringsregelOppfylt.BARNET_LEVER)
        } else {
            Evaluering.ikkeOppfylt(
                FiltreringsregelIkkeOppfylt.BARNET_LEVER_IKKE,
            )
        }

    fun søkerHarIkkeVerge(fakta: FiltreringsreglerFakta): Evaluering =
        if (!fakta.søkerHarVerge) {
            Evaluering.oppfylt(
                FiltreringsregelOppfylt.MOR_ER_MYNDIG,
            )
        } else {
            Evaluering.ikkeOppfylt(
                FiltreringsregelIkkeOppfylt.MOR_ER_UNDER_VERGEMÅL,
            )
        }

    fun søkerMottarIkkeLøpendeUtvidet(fakta: FiltreringsreglerFakta): Evaluering =
        if (!fakta.søkerMottarLøpendeUtvidet) {
            Evaluering.oppfylt(
                FiltreringsregelOppfylt.MOR_MOTTAR_IKKE_LØPENDE_UTVIDET,
            )
        } else {
            Evaluering.ikkeOppfylt(
                FiltreringsregelIkkeOppfylt.MOR_MOTTAR_LØPENDE_UTVIDET,
            )
        }

    fun søkerOppfyllerIkkeVilkårForUtvidetBarnetrygd(fakta: FiltreringsreglerFakta): Evaluering =
        if (!fakta.søkerOppfyllerVilkårForUtvidetBarnetrygd) {
            Evaluering.oppfylt(FiltreringsregelOppfylt.MOR_OPPFYLLER_IKKE_VILKÅR_FOR_UTVIDET_BARNETRYGD_VED_FØDSELSDATO)
        } else {
            Evaluering.ikkeOppfylt(FiltreringsregelIkkeOppfylt.MOR_OPPFYLLER_VILKÅR_FOR_UTVIDET_BARNETRYGD_VED_FØDSELSDATO)
        }

    fun søkerHarIkkeLøpendeEøsBarnetrygd(fakta: FiltreringsreglerFakta): Evaluering =
        if (!fakta.søkerMottarEøsBarnetrygd) {
            Evaluering.oppfylt(
                FiltreringsregelOppfylt.MOR_HAR_IKKE_LØPENDE_EØS_BARNETRYGD,
            )
        } else {
            Evaluering.ikkeOppfylt(
                FiltreringsregelIkkeOppfylt.MOR_HAR_LØPENDE_EØS_BARNETRYGD,
            )
        }

    fun fagsakIkkeMigrertEtterBarnBleFødt(fakta: FiltreringsreglerFaktaFødselshendelse): Evaluering =
        if (!fakta.erFagsakenMigrertEtterBarnFødt) {
            Evaluering.oppfylt(
                FiltreringsregelOppfylt.FAGSAK_IKKE_MIGRERT_UT_AV_INFOTRYGD_ETTER_BARN_FØDT,
            )
        } else {
            Evaluering.ikkeOppfylt(
                FiltreringsregelIkkeOppfylt.FAGSAK_MIGRERT_UT_AV_INFOTRYGD_ETTER_BARN_FØDT,
            )
        }

    fun løperIkkeBarnetrygdPåAnnenForelder(fakta: FiltreringsreglerFakta): Evaluering =
        if (!fakta.løperBarnetrygdForBarnetPåAnnenForelder) {
            Evaluering.oppfylt(
                FiltreringsregelOppfylt.LØPER_IKKE_BARNETRYGD_FOR_BARNET,
            )
        } else {
            Evaluering.ikkeOppfylt(
                FiltreringsregelIkkeOppfylt.LØPER_ALLEREDE_FOR_ANNEN_FORELDER,
            )
        }

    fun morHarIkkeOpphørtBarnetrygd(fakta: FiltreringsreglerFaktaFødselshendelse): Evaluering =
        if (fakta.morHarIkkeOpphørtBarnetrygd) {
            Evaluering.oppfylt(FiltreringsregelOppfylt.MOR_HAR_IKKE_OPPHØRT_BARNETRYGD)
        } else {
            Evaluering.ikkeOppfylt(FiltreringsregelIkkeOppfylt.MOR_HAR_OPPHØRT_BARNETRYGD)
        }

    fun merEnn5mndEllerMindreEnnFemDagerSidenForrigeBarn(fakta: FiltreringsreglerFaktaFødselshendelse): Evaluering =
        when (
            fakta.barnaSomSkalVurderes.all { barnFraHendelse ->
                fakta.restenAvBarna.all {
                    abs(ChronoUnit.MONTHS.between(barnFraHendelse.fødselsdato, it.fødselsdato)) > 5 ||
                        abs(ChronoUnit.DAYS.between(barnFraHendelse.fødselsdato, it.fødselsdato)) <= 6
                }
            }
        ) {
            true -> Evaluering.oppfylt(FiltreringsregelOppfylt.MER_ENN_5_MND_SIDEN_FORRIGE_BARN_UTFALL)
            false -> Evaluering.ikkeOppfylt(FiltreringsregelIkkeOppfylt.MINDRE_ENN_5_MND_SIDEN_FORRIGE_BARN_UTFALL)
        }
}

internal fun erFDatnummer(personIdent: String): Boolean = personIdent.substring(6).toInt() == 0

/**
 * BOST-nr har måned mellom 21 og 32
 */
internal fun erBostNummer(personIdent: String): Boolean {
    personIdent.substring(2, 4).toInt().also { måned ->
        return måned in 21..32
    }
}
