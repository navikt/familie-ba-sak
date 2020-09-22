package no.nav.familie.ba.sak.behandling.fødselshendelse.filtreringsregler

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.pdl.internal.PersonInfo
import java.time.LocalDate

data class Fakta(val mor: Person,
                 val barnaFraHendelse: List<Person>,
                 val restenAvBarna: List<PersonInfo>,
                 val morLever: Boolean,
                 val barnetLever: Boolean,
                 val morHarVerge: Boolean,
                 val dagensDato: LocalDate = LocalDate.now())
