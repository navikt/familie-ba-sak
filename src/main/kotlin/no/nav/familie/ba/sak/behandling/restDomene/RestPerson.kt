package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import java.time.LocalDate

data class RestPerson(
        val type: PersonType,
        val fødselsdato: LocalDate?,
        val personIdent: String,
        val navn: String,
        val kjønn: Kjønn,
        val målform: Målform
)

fun Person.tilRestPerson() = RestPerson(
        type = this.type,
        fødselsdato = this.fødselsdato,
        personIdent = this.personIdent.ident,
        navn = this.navn,
        kjønn = this.kjønn,
        målform = this.målform
)