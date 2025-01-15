import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Dødsfall
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonEnkel
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
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
