package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.arbeidsforhold.GrArbeidsforhold
import java.time.LocalDate

fun lagGrArbeidsforhold(
    person: Person = tilfeldigPerson(),
    fraOgMed: LocalDate? = LocalDate.now(),
    tilOgMed: LocalDate? = null,
    arbeidsgiverId: String = "123123123",
    arbeidsgiverType: String = "Organisasjon",
) = GrArbeidsforhold(
    periode = DatoIntervallEntitet(fraOgMed, tilOgMed),
    arbeidsgiverId = arbeidsgiverId,
    arbeidsgiverType = arbeidsgiverType,
    person = person,
)
