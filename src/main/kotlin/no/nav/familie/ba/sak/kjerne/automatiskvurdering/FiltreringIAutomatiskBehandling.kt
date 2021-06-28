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
        val morFnr = mor.personIdent.ident.takeLast(5) != "00000" && mor.personIdent.ident.takeLast(5) != "00001"
        val barnFnr =
            barnaFraHendelse.all { it.personIdent.ident.takeLast(5) != "00000" && it.personIdent.ident.takeLast(5) != "00001" }

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
}