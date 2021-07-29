package no.nav.familie.ba.sak.kjerne.automatiskvurdering.filtreringsregler

import no.nav.familie.ba.sak.common.erFraInneværendeEllerForrigeMåned
import no.nav.familie.ba.sak.common.erFraInneværendeMåned
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.kjerne.fødselshendelse.filtreringsregler.utfall.FiltreringsregelIkkeOppfylt
import no.nav.familie.ba.sak.kjerne.fødselshendelse.filtreringsregler.utfall.FiltreringsregelIkkeOppfyltNy
import no.nav.familie.ba.sak.kjerne.fødselshendelse.filtreringsregler.utfall.FiltreringsregelOppfyltNy
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Evaluering
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import java.time.LocalDate

enum class FiltreringsreglerResultat(val vurder: Fakta.() -> Evaluering) {
    MOR_GYLDIG_FNR(vurder = { morHarGyldigFnr(this) }),
    BARN_GYLDIG_FNR(vurder = { barnHarGyldigFnr(this) }),
    MOR_ER_OVER_18_ÅR(vurder = { morErOver18år(this) }),
    MOR_LEVER(vurder = { morLever(this) }),
    BARN_LEVER(vurder = { barnLever(this) }),
    MOR_HAR_IKKE_VERGE(vurder = { morHarIkkeVerge(this) }),
    MER_ENN_5_MND_SIDEN_FORRIGE_BARN(vurder = { merEnn5mndEllerMindreEnnFemDagerSidenForrigeBarn(this) }),
    BARNETS_FØDSELSDATO_TRIGGER_IKKE_ETTERBETALING(vurder = { barnetsFødselsdatoInnebærerIkkeEtterbetaling(this) }),
}

fun evaluerFiltreringsregler(fakta: Fakta) = FiltreringsreglerResultat.values().map { it.vurder(fakta) }

fun morHarGyldigFnr(fakta: Fakta): Evaluering {
    val erMorFnrGyldig = (!erBostNummer(fakta.mor.personIdent.ident) && !erFDatnummer(fakta.mor.personIdent.ident))

    return if (erMorFnrGyldig) Evaluering.oppfylt(FiltreringsregelOppfyltNy.MOR_HAR_GYLDIG_FNR) else Evaluering.ikkeOppfylt(
            FiltreringsregelIkkeOppfyltNy.MOR_HAR_UGYLDIG_FNR)
}

fun barnHarGyldigFnr(fakta: Fakta): Evaluering {
    val erbarnFnrGyldig =
            fakta.barnaFraHendelse.all { (!erBostNummer(it.personIdent.ident) && !erFDatnummer(it.personIdent.ident)) }

    return if (erbarnFnrGyldig) Evaluering.oppfylt(FiltreringsregelOppfyltNy.BARN_HAR_GYLDIG_FNR) else Evaluering.ikkeOppfylt(
            FiltreringsregelIkkeOppfyltNy.BARN_HAR_UGYLDIG_FNR)
}

fun morErOver18år(fakta: Fakta): Evaluering = if (fakta.mor.hentAlder() > 18) Evaluering.oppfylt(
        FiltreringsregelOppfyltNy.MOR_ER_OVER_18_ÅR) else Evaluering.ikkeOppfylt(FiltreringsregelIkkeOppfyltNy.MOR_ER_UNDER_18_ÅR)

fun morLever(fakta: Fakta): Evaluering = if (fakta.morLever) Evaluering.oppfylt(FiltreringsregelOppfyltNy.MOR_LEVER) else Evaluering.ikkeOppfylt(
        FiltreringsregelIkkeOppfyltNy.MOR_LEVER_IKKE)

fun barnLever(fakta: Fakta): Evaluering = if (fakta.barnaLever) Evaluering.oppfylt(FiltreringsregelOppfyltNy.BARNET_LEVER) else Evaluering.ikkeOppfylt(
        FiltreringsregelIkkeOppfyltNy.BARNET_LEVER_IKKE)

fun morHarIkkeVerge(fakta: Fakta): Evaluering = if (!fakta.morHarVerge) Evaluering.oppfylt(FiltreringsregelOppfyltNy.MOR_ER_MYNDIG) else Evaluering.ikkeOppfylt(
        FiltreringsregelIkkeOppfylt.MOR_ER_UMYNDIG)

fun merEnn5mndEllerMindreEnnFemDagerSidenForrigeBarn(fakta: Fakta): Evaluering {
    return when (fakta.barnaFraHendelse.all { barnFraHendelse ->
        fakta.restenAvBarna.all {
            barnFraHendelse.fødselsdato.isAfter(it.fødselsdato.plusMonths(5)) ||
            barnFraHendelse.fødselsdato.isBefore(it.fødselsdato.plusDays(6))
        }
    }) {
        true -> Evaluering.oppfylt(FiltreringsregelOppfyltNy.MER_ENN_5_MND_SIDEN_FORRIGE_BARN_UTFALL)
        false -> Evaluering.ikkeOppfylt(FiltreringsregelIkkeOppfyltNy.MINDRE_ENN_5_MND_SIDEN_FORRIGE_BARN_UTFALL)
    }
}

fun barnetsFødselsdatoInnebærerIkkeEtterbetaling(fakta: Fakta): Evaluering {
    val innebærerBarnasFødselsdatoEtterbetaling =
            innebærerBarnasFødselsdatoEtterbetaling(fakta.barnaFraHendelse.map { it.fødselsdato }, fakta.dagensDato)

    return if (!innebærerBarnasFødselsdatoEtterbetaling) Evaluering.oppfylt(FiltreringsregelOppfyltNy.SAKEN_MEDFØRER_IKKE_ETTERBETALING) else Evaluering.ikkeOppfylt(
            FiltreringsregelIkkeOppfyltNy.SAKEN_MEDFØRER_ETTERBETALING)
}

fun mindreEnn5MndSidenForrigeBarn(barnaFraHendelse: List<Person>, restenAvBarna: List<PersonInfo>): Boolean {
    return !barnaFraHendelse.all { barnFraHendelse ->
        restenAvBarna.all {
            barnFraHendelse.fødselsdato.isAfter(it.fødselsdato.plusMonths(5)) ||
            barnFraHendelse.fødselsdato.isBefore(it.fødselsdato.plusDays(6))
        }
    }
}

internal fun erFDatnummer(personIdent: String): Boolean {
    return personIdent.substring(6).toInt() == 0
}

internal fun erBostNummer(personIdent: String): Boolean {
    return personIdent.substring(2, 3).toInt() > 1
}

internal fun innebærerBarnasFødselsdatoEtterbetaling(barnasFødselsdatoer: List<LocalDate>, dagensDato: LocalDate): Boolean {
    val dagensDatoErFør21Imåneden = dagensDato.dayOfMonth < 21
    val barnErFødtFørForrigeMåned = barnasFødselsdatoer.any {
        !it.erFraInneværendeEllerForrigeMåned(dagensDato)
    }
    val barnErFødtFørDenneMåneden = barnasFødselsdatoer.any {
        !it.erFraInneværendeMåned(dagensDato)
    }

    return when {
        dagensDatoErFør21Imåneden && barnErFødtFørForrigeMåned -> true
        !dagensDatoErFør21Imåneden && barnErFødtFørDenneMåneden -> true
        else -> false
    }
}