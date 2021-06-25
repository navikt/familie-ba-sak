package no.nav.familie.ba.sak.kjerne.automatiskvurdering

import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import java.time.LocalDate

data class FiltreringIAutomatiskBehandling(
    private val mor: Person,
    private val barnaFraHendelse: List<Person>,
    private val restenAvBarna: List<PersonInfo>,
    private val morLever: Boolean,
    private val barnLever: Boolean,
    private val morHarIkkeVerge: Boolean,
    private val dagensDato: LocalDate = LocalDate.now()
) {

    fun evaluerData(): Pair<Boolean, String?> {
        //val morFnr
        //val barnFnr
        val morOver18 = mor.fødselsdato.plusYears(18).isBefore(dagensDato)
        val barnIkkeMindreEnnFemMndMellomrom = barnaFraHendelse.all { barnFraHendelse ->
            restenAvBarna.all {
                barnFraHendelse.fødselsdato.isAfter(it.fødselsdato.plusMonths(5)) ||
                        barnFraHendelse.fødselsdato.isBefore(it.fødselsdato.plusDays(6))
            }
        }
        if (søkerPassererFiltering()) {
            return Pair(true, null)
        }
    }

    fun søkerPassererFiltering(): Boolean {
        return (morLever && barnLever && barnMindreEnnFemMndMellomrom && morOver18 && morHarIkkeVerge && morFnr && barnFnr)
    }

    fun hentBegrunnelseFraFiltrering(): String {
        return when {
            !morFnr -> "Fødselshendelse: Mor ikke gyldig fødselsnummer"
            !barnFnr -> "Fødselshendelse: Barnet ikke gyldig fødselsnummer"
            !morLever -> "Fødselshendelse: Registrert dødsdato på mor"
            !barnLever -> "Fødselshendelse: Registrert dødsdato på barnet"
            !barnMindreEnnFemMndMellomrom -> "Fødselshendelse: Mor har barn med mindre enn fem måneders mellomrom"
            !morOver18 -> "Fødselshendelse: Mor under 18 år"
            !morHarIkkeVerge -> "Fødselshendelse: Mor er umyndig"
            else -> "Saken skal behandles i BA-SAK"
        }
    }