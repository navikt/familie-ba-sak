package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import java.time.LocalDate

/**
 * NB: Bør ikke brukes internt, men kun ut mot eksterne tjenester siden klassen
 * inneholder aktiv personIdent og ikke aktørId.
 */
data class PersonDto(
    val type: PersonType,
    val fødselsdato: LocalDate?,
    val personIdent: String,
    val navn: String,
    val kjønn: Kjønn,
    val registerhistorikk: RegisterhistorikkDto? = null,
    val målform: Målform,
    val dødsfallDato: LocalDate? = null,
    val erManueltLagtTilISøknad: Boolean? = null,
    val harFalskIdentitet: Boolean? = false,
)

fun Person.tilPersonDto(erManueltLagtTilISøknad: Boolean? = null): PersonDto =
    PersonDto(
        type = this.type,
        fødselsdato = this.fødselsdato,
        personIdent = this.aktør.aktivFødselsnummer(),
        navn = this.navn,
        kjønn = this.kjønn,
        registerhistorikk = this.tilRegisterhistorikkDto(),
        målform = this.målform,
        dødsfallDato = this.dødsfall?.dødsfallDato,
        erManueltLagtTilISøknad = erManueltLagtTilISøknad,
        harFalskIdentitet = this.harFalskIdentitet,
    )
