package no.nav.familie.ba.sak.behandling.domene.vilkår

import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlag

data class Fakta(
        val personopplysningGrunnlag: PersonopplysningGrunnlag
) {
    val barn = personopplysningGrunnlag.barna
}