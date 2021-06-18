package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.kontrakter.felles.objectMapper

data class FaktaTilVilkårsvurdering(val personForVurdering: Person) {

    val alder = personForVurdering.hentAlder()
    fun toJson(): String =
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)
}