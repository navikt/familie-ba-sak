package no.nav.familie.ba.sak.kjerne.fødselshendelse.vilkårsvurdering

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import java.time.LocalDate

data class VilkårsvurderingFakta(
        val person: Person,
        val vurderFra: LocalDate = LocalDate.now(),
)
