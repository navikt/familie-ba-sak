package no.nav.familie.ba.sak.behandling.fødselshendelse.filtreringsregler

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.pdl.internal.PersonInfo

data class Fakta(val mor: Person,
                 val barn: Person,
                 val restenAvBarna: List<PersonInfo>,
                 val morLever: Boolean,
                 val barnetLever: Boolean,
                 val morHarVerge: Boolean)

