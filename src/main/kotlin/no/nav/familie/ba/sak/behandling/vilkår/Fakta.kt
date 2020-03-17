package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import java.time.LocalDate
import java.time.Period

data class Fakta(val personForVurdering: Person) {
    val alder = Period.between(personForVurdering.fødselsdato, LocalDate.now()).years
}