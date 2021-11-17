package no.nav.familie.ba.sak.kjerne.fødselshendelse.filtreringsregler

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import java.time.LocalDate

data class FiltreringsreglerFakta(
    val mor: Person,
    val barnaFraHendelse: List<Person>,
    val restenAvBarna: List<PersonInfo>,
    val morLever: Boolean,
    val barnaLever: Boolean,
    val morHarVerge: Boolean,
    @JsonIgnore val dagensDato: LocalDate = LocalDate.now()
)
