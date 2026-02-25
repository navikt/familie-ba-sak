package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTANDTYPE
import java.time.LocalDate

fun lagGrSivilstand(
    person: Person = tilfeldigPerson(),
    fraOgMed: LocalDate = LocalDate.now().minusYears(1),
    type: SIVILSTANDTYPE = SIVILSTANDTYPE.GIFT,
) = GrSivilstand(
    fom = fraOgMed,
    type = type,
    person = person,
)
