package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.opphold.GrOpphold
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import java.time.LocalDate

fun lagGrOpphold(
    person: Person = tilfeldigPerson(),
    gyldigFraOgMed: LocalDate? = LocalDate.now(),
    gyldigTilOgMed: LocalDate? = LocalDate.now().plusYears(1),
    type: OPPHOLDSTILLATELSE = OPPHOLDSTILLATELSE.MIDLERTIDIG,
) = GrOpphold(
    gyldigPeriode = DatoIntervallEntitet(gyldigFraOgMed, gyldigTilOgMed),
    type = type,
    person = person,
)
