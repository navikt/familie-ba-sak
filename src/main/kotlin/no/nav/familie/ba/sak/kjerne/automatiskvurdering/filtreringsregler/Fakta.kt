package no.nav.familie.ba.sak.kjerne.automatiskvurdering.filtreringsregler

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.kontrakter.felles.objectMapper
import java.time.LocalDate

data class Fakta(val mor: Person,
                 val barnaFraHendelse: List<Person>,
                 val restenAvBarna: List<PersonInfo>,
                 val morLever: Boolean,
                 val barnaLever: Boolean,
                 val morHarVerge: Boolean,
                 @JsonIgnore val dagensDato: LocalDate = LocalDate.now()) {

    fun toJson(): String =
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)
}

