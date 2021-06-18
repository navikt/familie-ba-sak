package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import java.time.LocalDate
import java.time.LocalDateTime

data class RestPerson(
        val type: PersonType,
        val fødselsdato: LocalDate?,
        val personIdent: String,
        val navn: String,
        val kjønn: Kjønn,
        val registerhistorikk: RestRegisterhistorikk? = null,
        val målform: Målform
)

fun Person.tilRestPerson() = RestPerson(
        type = this.type,
        fødselsdato = this.fødselsdato,
        personIdent = this.personIdent.ident,
        navn = this.navn,
        kjønn = this.kjønn,
        registerhistorikk = this.tilRestRegisterhistorikk(),
        målform = this.målform
)

fun Person.tilRestRegisterhistorikk() = RestRegisterhistorikk(
        hentetTidspunkt = this.personopplysningGrunnlag.opprettetTidspunkt,
        oppholdstillatelse = opphold.map { it.tilRestRegisteropplysning() },
        statsborgerskap = statsborgerskap.map { it.tilRestRegisteropplysning() },
        bostedsadresse = this.bostedsadresser.map { it.tilRestRegisteropplysning() },
        sivilstand = this.sivilstander.map { it.tilRestRegisteropplysning() },
)

data class RestRegisterhistorikk(
        val hentetTidspunkt: LocalDateTime,
        val sivilstand: List<RestRegisteropplysning>? = emptyList(),
        val oppholdstillatelse: List<RestRegisteropplysning>? = emptyList(),
        val statsborgerskap: List<RestRegisteropplysning>? = emptyList(),
        val bostedsadresse: List<RestRegisteropplysning>? = emptyList(),
)

data class RestRegisteropplysning(
        val fom: LocalDate?,
        val tom: LocalDate?,
        var verdi: String,
)