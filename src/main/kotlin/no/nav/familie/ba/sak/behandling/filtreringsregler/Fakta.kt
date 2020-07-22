package no.nav.familie.ba.sak.behandling.filtreringsregler

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.integrasjoner.domene.Familierelasjoner
import no.nav.familie.ba.sak.integrasjoner.domene.Personinfo
import no.nav.familie.ba.sak.pdl.internal.PersonInfo

data class Fakta(val mor: Person,
                 val barn: Person,
                 val restenAvBarna: List<PersonInfo>,
                 val morLever: Boolean,
                 val barnetLever: Boolean,
                 val morHarVerge: Boolean)

