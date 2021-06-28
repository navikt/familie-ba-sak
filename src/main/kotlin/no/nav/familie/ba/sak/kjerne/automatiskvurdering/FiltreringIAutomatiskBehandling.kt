package no.nav.familie.ba.sak.kjerne.automatiskvurdering

import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import java.time.LocalDate

data class FiltreringIAutomatiskBehandling(
    private val mor: Person,
    private val barnaFraHendelse: List<Person>,
    private val restenAvBarna: List<PersonInfo>,
    private val morLever: Boolean,
    private val barnaLever: Boolean,
    private val morHarIkkeVerge: Boolean,
    private val dagensDato: LocalDate = LocalDate.now()
) {

    fun evaluerData(): Pair<Boolean, String> {
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
            !morFnr -> Pair(false, "Fødselshendelse: Mor ikke gyldig fødselsnummer")

            !barnFnr -> Pair(false, "Fødselshendelse: Barnet ikke gyldig fødselsnummer")

            !morLever -> Pair(false, "Fødselshendelse: Registrert dødsdato på mor")

            !barnaLever -> Pair(false, "Fødselshendelse: Registrert dødsdato på barnet")

            !barnIkkeMindreEnnFemMndMellomrom -> Pair(
                false,
                "Fødselshendelse: Mor har barn med mindre enn fem måneders mellomrom"
            )

            !morOver18 -> Pair(false, "Fødselshendelse: Mor under 18 år")

            !morHarIkkeVerge -> Pair(false, "Fødselshendelse: Mor er umyndig")
            else -> Pair(true, "Blir sendt til BA-SAK")
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
}