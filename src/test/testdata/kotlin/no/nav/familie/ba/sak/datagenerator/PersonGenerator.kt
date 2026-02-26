package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Dødsfall
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonEnkel
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.bostedsadresse.GrBostedsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.deltbosted.GrDeltBosted
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.oppholdsadresse.GrOppholdsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.arbeidsforhold.GrArbeidsforhold
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.opphold.GrOpphold
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTANDTYPE
import java.time.LocalDate
import kotlin.random.Random

fun tilfeldigPerson(
    fødselsdato: LocalDate = LocalDate.now(),
    personType: PersonType = PersonType.BARN,
    kjønn: Kjønn = Kjønn.MANN,
    aktør: Aktør = randomAktør(),
    personId: Long = Random.nextLong(10000000),
    dødsfall: Dødsfall? = null,
) = Person(
    id = personId,
    aktør = aktør,
    fødselsdato = fødselsdato,
    type = personType,
    personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 0),
    navn = "",
    kjønn = kjønn,
    målform = Målform.NB,
    dødsfall = dødsfall,
).apply { sivilstander = mutableListOf(GrSivilstand(type = SIVILSTANDTYPE.UGIFT, person = this)) }

fun Person.tilPersonEnkel() = PersonEnkel(this.type, this.aktør, this.fødselsdato, this.dødsfall?.dødsfallDato, this.målform)

fun tilfeldigSøker(
    fødselsdato: LocalDate = LocalDate.now(),
    personType: PersonType = PersonType.SØKER,
    kjønn: Kjønn = Kjønn.MANN,
    aktør: Aktør = randomAktør(),
) = Person(
    id = Random.nextLong(10000000),
    aktør = aktør,
    fødselsdato = fødselsdato,
    type = personType,
    personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 0),
    navn = "",
    kjønn = kjønn,
    målform = Målform.NB,
).apply { sivilstander = mutableListOf(GrSivilstand(type = SIVILSTANDTYPE.UGIFT, person = this)) }

fun lagPerson(
    id: Long = 0,
    type: PersonType = PersonType.SØKER,
    fødselsdato: LocalDate = LocalDate.now().minusYears(19),
    navn: String = PersonType.SØKER.name,
    kjønn: Kjønn = Kjønn.KVINNE,
    målform: Målform = Målform.NB,
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    aktør: Aktør = lagAktør(),
    harFalskIdentitet: Boolean = false,
    bostedsadresser: (person: Person) -> List<GrBostedsadresse> = { listOf() },
    oppholdsadresser: (person: Person) -> List<GrOppholdsadresse> = { listOf() },
    deltBosted: (person: Person) -> List<GrDeltBosted> = { listOf() },
    statsborgerskap: (person: Person) -> List<GrStatsborgerskap> = { listOf() },
    opphold: (person: Person) -> List<GrOpphold> = { listOf() },
    arbeidsforhold: (person: Person) -> List<GrArbeidsforhold> = { listOf() },
    sivilstander: (person: Person) -> List<GrSivilstand> = { listOf() },
    dødsfall: (person: Person) -> Dødsfall? = { null },
): Person {
    val person =
        Person(
            id = id,
            type = type,
            fødselsdato = fødselsdato,
            navn = navn,
            kjønn = kjønn,
            målform = målform,
            personopplysningGrunnlag = personopplysningGrunnlag,
            aktør = aktør,
            harFalskIdentitet = harFalskIdentitet,
        )
    person.dødsfall = dødsfall(person)
    person.bostedsadresser.addAll(bostedsadresser(person))
    person.oppholdsadresser.addAll(oppholdsadresser(person))
    person.deltBosted.addAll(deltBosted(person))
    person.statsborgerskap.addAll(statsborgerskap(person))
    person.opphold.addAll(opphold(person))
    person.arbeidsforhold.addAll(arbeidsforhold(person))
    person.sivilstander.addAll(sivilstander(person))
    return person
}

fun lagPerson(
    personIdent: PersonIdent = PersonIdent(randomFnr()),
    aktør: Aktør = lagAktør(personIdent.ident),
    type: PersonType = PersonType.SØKER,
    navn: String = PersonType.SØKER.name,
    personopplysningGrunnlag: PersonopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 0),
    fødselsdato: LocalDate = LocalDate.now().minusYears(19),
    kjønn: Kjønn = Kjønn.KVINNE,
    dødsfall: Dødsfall? = null,
    id: Long = 0,
) = Person(
    aktør = aktør,
    type = type,
    personopplysningGrunnlag = personopplysningGrunnlag,
    fødselsdato = fødselsdato,
    navn = navn,
    kjønn = kjønn,
    dødsfall = dødsfall,
    id = id,
)

fun lagPersonEnkel(
    personType: PersonType,
    aktør: Aktør = randomAktør(),
    dødsfallDato: LocalDate? = null,
    fødselsdato: LocalDate =
        if (personType == PersonType.SØKER) {
            LocalDate.now().minusYears(34)
        } else {
            LocalDate.now().minusYears(4)
        },
    målform: Målform = Målform.NB,
): PersonEnkel =
    PersonEnkel(
        type = personType,
        aktør = aktør,
        dødsfallDato = dødsfallDato,
        fødselsdato = fødselsdato,
        målform = målform,
    )
