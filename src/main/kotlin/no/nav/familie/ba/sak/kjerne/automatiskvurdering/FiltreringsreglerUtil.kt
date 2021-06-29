package no.nav.familie.ba.sak.kjerne.automatiskvurdering

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
    GODKJENT("")

}

fun evaluerFiltreringsregler(mor: Person,
                             barnaFraHendelse: List<Person>,
                             restenAvBarna: List<PersonInfo>,
                             morLever: Boolean,
                             barnaLever: Boolean,
                             morHarVerge: Boolean,
                             dagensDato: LocalDate = LocalDate.now()
): FiltreringsreglerResultat {

    val erMorFnrGyldig = gyldigFnr(mor.personIdent.ident)
    val erbarnFnrGyldig = barnaFraHendelse.all { gyldigFnr(it.personIdent.ident) }
    val erMorOver18 = mor.fødselsdato.plusYears(18).isBefore(dagensDato)
    val erMindreEnn5MndSidenForrigeBarn = mindreEnn5MndSidenForrigeBarn(barnaFraHendelse, restenAvBarna)

    return when {
        !erMorFnrGyldig -> FiltreringsreglerResultat.MOR_IKKE_GYLDIG_FNR
        !erbarnFnrGyldig -> FiltreringsreglerResultat.BARN_IKKE_GYLDIG_FNR
        !morLever -> FiltreringsreglerResultat.MOR_ER_DØD
        !barnaLever -> FiltreringsreglerResultat.DØDT_BARN
        erMindreEnn5MndSidenForrigeBarn -> FiltreringsreglerResultat.MINDRE_ENN_5_MND_SIDEN_FORRIGE_BARN
        !erMorOver18 -> FiltreringsreglerResultat.MOR_ER_IKKE_OVER_18
        morHarVerge -> FiltreringsreglerResultat.MOR_HAR_VERGE
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

internal fun gyldigFnr(fnr: String): Boolean {
    var nestSisteSiffer = 11 -
                          (3 * Character.getNumericValue(fnr[0]) +
                           7 * Character.getNumericValue(fnr[1]) +
                           6 * Character.getNumericValue(fnr[2]) +
                           1 * Character.getNumericValue(fnr[3]) +
                           8 * Character.getNumericValue(fnr[4]) +
                           9 * Character.getNumericValue(fnr[5]) +
                           4 * Character.getNumericValue(fnr[6]) +
                           5 * Character.getNumericValue(fnr[7]) +
                           2 * Character.getNumericValue(fnr[8])) % 11
    if (nestSisteSiffer == 11) {
        nestSisteSiffer = 0
    }

    var sisteSiffer = 11 -
                      (5 * Character.getNumericValue(fnr[0]) +
                       4 * Character.getNumericValue(fnr[1]) +
                       3 * Character.getNumericValue(fnr[2]) +
                       2 * Character.getNumericValue(fnr[3]) +
                       7 * Character.getNumericValue(fnr[4]) +
                       6 * Character.getNumericValue(fnr[5]) +
                       5 * Character.getNumericValue(fnr[6]) +
                       4 * Character.getNumericValue(fnr[7]) +
                       3 * Character.getNumericValue(fnr[8]) +
                       2 * nestSisteSiffer) % 11
    if (sisteSiffer == 11) {
        sisteSiffer = 0
    }
    return (nestSisteSiffer == Character.getNumericValue(fnr[9]) && sisteSiffer == Character.getNumericValue(fnr[10]))
}
