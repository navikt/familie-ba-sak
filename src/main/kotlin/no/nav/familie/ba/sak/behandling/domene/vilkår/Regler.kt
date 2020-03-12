package no.nav.familie.ba.sak.behandling.domene.vilkår

import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Person
import no.nav.nare.core.evaluations.Evaluering
import java.time.LocalDate


internal fun sjekkOmBosattINorge(person: Person): Evaluering {
    return if (person.fødselsdato.isBefore(LocalDate.now().plusYears(100)))
        Evaluering.ja("Person bosatt i Norge")
    else Evaluering.nei("Person ikke bosatt i Norge")
}