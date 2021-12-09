package no.nav.familie.ba.sak.kjerne.vedtak.domene

import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.ekstern.restDomene.RestPerson
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import java.time.LocalDate

data class BegrunnelsePerson(
    val personIdent: String,
    val fødselsdato: LocalDate,
    val type: PersonType
)

fun Person.tilBegrunnelsePerson() = BegrunnelsePerson(
    personIdent = this.aktør.aktivIdent(),
    fødselsdato = this.fødselsdato,
    type = this.type
)

fun List<BegrunnelsePerson>.tilBarnasFødselsdatoer(): String =
    Utils.slåSammen(
        this
            .filter { it.type == PersonType.BARN }
            .sortedBy { person ->
                person.fødselsdato
            }
            .map { person ->
                person.fødselsdato.tilKortString() ?: ""
            }
    )

fun BegrunnelsePerson.tilRestPersonTilTester() = RestPerson(
    personIdent = this.personIdent,
    fødselsdato = this.fødselsdato,
    type = this.type,
    navn = "Mock Mockersen",
    kjønn = Kjønn.KVINNE,
    målform = Målform.NB
)
