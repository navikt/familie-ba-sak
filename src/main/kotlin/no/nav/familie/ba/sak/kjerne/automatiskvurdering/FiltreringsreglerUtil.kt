package no.nav.familie.ba.sak.kjerne.automatiskvurdering

import no.nav.familie.ba.sak.common.erFraInneværendeEllerForrigeMåned
import no.nav.familie.ba.sak.common.erFraInneværendeMåned
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import java.time.LocalDate

enum class FiltreringsreglerResultat(val beskrivelse: String) {
    MOR_IKKE_GYLDIG_FNR("Fødselshendelse: Mor ikke gyldig fødselsnummer"),
    BARN_IKKE_GYLDIG_FNR("Fødselshendelse: Barnet ikke gyldig fødselsnummer"),
    MOR_ER_DØD("Fødselshendelse: Registrert dødsdato på mor"),
    DØDT_BARN("Fødselshendelse: Registrert dødsdato på barnet"),
    MINDRE_ENN_5_MND_SIDEN_FORRIGE_BARN("Fødselshendelse: Mor har barn med mindre enn fem måneders mellomrom"),
    MOR_ER_IKKE_OVER_18("Fødselshendelse: Mor under 18 år"),
    MOR_HAR_VERGE("Fødselshendelse: Mor er umyndig"),
    KREVER_ETTERBETALING("Fødselshendelse: Saken krever etterbetaling"),
    GODKJENT("")
}

fun evaluerFiltreringsregler(
        mor: Person,
        barnaFraHendelse: List<Person>,
        restenAvBarna: List<PersonInfo>,
        morLever: Boolean,
        barnaLever: Boolean,
        morHarVerge: Boolean,
): FiltreringsreglerResultat {

    val erMorFnrGyldig = (!erBostNummer(mor.personIdent.ident) && !erFDatnummer(mor.personIdent.ident))
    val erbarnFnrGyldig = barnaFraHendelse.all { (!erBostNummer(it.personIdent.ident) && !erFDatnummer(it.personIdent.ident)) }
    val erMorOver18 = mor.fødselsdato.plusYears(18).isBefore(LocalDate.now())
    val erMindreEnn5MndSidenForrigeBarn = mindreEnn5MndSidenForrigeBarn(barnaFraHendelse, restenAvBarna)
    val innebærerBarnasFødselsdatoEtterbetaling =
            innebærerBarnasFødselsdatoEtterbetaling(barnaFraHendelse.map { it.fødselsdato })

    return when {
        !erMorFnrGyldig -> FiltreringsreglerResultat.MOR_IKKE_GYLDIG_FNR
        !erbarnFnrGyldig -> FiltreringsreglerResultat.BARN_IKKE_GYLDIG_FNR
        !morLever -> FiltreringsreglerResultat.MOR_ER_DØD
        !barnaLever -> FiltreringsreglerResultat.DØDT_BARN
        erMindreEnn5MndSidenForrigeBarn -> FiltreringsreglerResultat.MINDRE_ENN_5_MND_SIDEN_FORRIGE_BARN
        !erMorOver18 -> FiltreringsreglerResultat.MOR_ER_IKKE_OVER_18
        morHarVerge -> FiltreringsreglerResultat.MOR_HAR_VERGE
        innebærerBarnasFødselsdatoEtterbetaling -> FiltreringsreglerResultat.KREVER_ETTERBETALING
        else -> FiltreringsreglerResultat.GODKJENT
    }
}

internal fun mindreEnn5MndSidenForrigeBarn(barnaFraHendelse: List<Person>, restenAvBarna: List<PersonInfo>): Boolean {
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

internal fun innebærerBarnasFødselsdatoEtterbetaling(barnasFødselsdatoer: List<LocalDate>): Boolean {
    val dagensDato = LocalDate.now()
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