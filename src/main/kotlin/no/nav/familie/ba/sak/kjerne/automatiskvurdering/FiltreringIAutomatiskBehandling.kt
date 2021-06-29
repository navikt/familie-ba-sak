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

fun evaluerData(mor: Person,
                barnaFraHendelse: List<Person>,
                restenAvBarna: List<PersonInfo>,
                morLever: Boolean,
                barnaLever: Boolean,
                morHarIkkeVerge: Boolean,
                dagensDato: LocalDate = LocalDate.now()
): FiltreringsreglerResultat {
    
    val morFnr = gyldigFnr(mor.personIdent.ident)
    val barnFnr =
            barnaFraHendelse.all { gyldigFnr(it.personIdent.ident) }

    val morOver18 = mor.fødselsdato.plusYears(18).isBefore(dagensDato)

    val barnIkkeMindreEnnFemMndMellomrom = barnaFraHendelse.all { barnFraHendelse ->
        restenAvBarna.all {
            barnFraHendelse.fødselsdato.isAfter(it.fødselsdato.plusMonths(5)) ||
            barnFraHendelse.fødselsdato.isBefore(it.fødselsdato.plusDays(6))
        }
    }


    return when {
        !morFnr -> FiltreringsreglerResultat.MOR_IKKE_GYLDIG_FNR

        !barnFnr -> FiltreringsreglerResultat.BARN_IKKE_GYLDIG_FNR

        !morLever -> FiltreringsreglerResultat.MOR_ER_DØD

        !barnaLever -> FiltreringsreglerResultat.DØDT_BARN

        !barnIkkeMindreEnnFemMndMellomrom -> FiltreringsreglerResultat.MINDRE_ENN_5_MND_SIDEN_FORRIGE_BARN

        !morOver18 -> FiltreringsreglerResultat.MOR_ER_IKKE_OVER_18

        !morHarIkkeVerge -> FiltreringsreglerResultat.MOR_HAR_VERGE
        else -> FiltreringsreglerResultat.GODKJENT
    }
}

internal fun gyldigFnr(fnr: String): Boolean {
    var k1 = 11 -
             (3 * Character.getNumericValue(fnr[0]) +
              7 * Character.getNumericValue(fnr[1]) +
              6 * Character.getNumericValue(fnr[2]) +
              1 * Character.getNumericValue(fnr[3]) +
              8 * Character.getNumericValue(fnr[4]) +
              9 * Character.getNumericValue(fnr[5]) +
              4 * Character.getNumericValue(fnr[6]) +
              5 * Character.getNumericValue(fnr[7]) +
              2 * Character.getNumericValue(fnr[8])) % 11
    if (k1 == 11) {
        k1 = 0
    }

    var k2 = 11 -
             (5 * Character.getNumericValue(fnr[0]) +
              4 * Character.getNumericValue(fnr[1]) +
              3 * Character.getNumericValue(fnr[2]) +
              2 * Character.getNumericValue(fnr[3]) +
              7 * Character.getNumericValue(fnr[4]) +
              6 * Character.getNumericValue(fnr[5]) +
              5 * Character.getNumericValue(fnr[6]) +
              4 * Character.getNumericValue(fnr[7]) +
              3 * Character.getNumericValue(fnr[8]) +
              2 * k1) % 11
    if (k2 == 11) {
        k2 = 0
    }
    return (k1 == Character.getNumericValue(fnr[9]) && k2 == Character.getNumericValue(fnr[10]))
}
