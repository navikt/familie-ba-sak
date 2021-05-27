package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.common.Utils.storForbokstav
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
        sivilstand = listOf(RestRegisteropplysning(fom = null,
                                                   tom = null,
                                                   verdi = this.sivilstand.name.storForbokstav())), // TODO: Kommer historisk data
        oppholdstillatelse = opphold.map { it.tilRestRegisteropplysning() },
        statsborgerskap = statsborgerskap.map { it.tilRestRegisteropplysning() },
        bostedsadresse = this.bostedsadresse?.let { listOf(it.tilRestRegisteropplysning()) }, // TODO: Kommer historisk data)
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
        val verdi: String,
)