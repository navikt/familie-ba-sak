package no.nav.familie.ba.sak.behandling.filtreringsregler

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person

data class Fakta(val søker: Person,
                 val barn: Person)

