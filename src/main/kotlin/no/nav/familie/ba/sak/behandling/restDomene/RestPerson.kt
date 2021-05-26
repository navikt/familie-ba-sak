package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.common.Utils.storForbokstav
import java.time.LocalDate

data class RestPerson(
        val type: PersonType,
        val fødselsdato: LocalDate?,
        val personIdent: String,
        val navn: String,
        val kjønn: Kjønn,
        val registerHistorikk: RestRegisterHistorikk? = null,
        val målform: Målform
)

fun Person.tilRestPerson() = RestPerson(
        type = this.type,
        fødselsdato = this.fødselsdato,
        personIdent = this.personIdent.ident,
        navn = this.navn,
        kjønn = this.kjønn,
        registerHistorikk = this.tilRestRegisterHistorikk(),
        målform = this.målform
)

fun Person.tilRestRegisterHistorikk() = RestRegisterHistorikk(
        sivilstand = listOf(RestRegisterOpplysning(fom = null,
                                                   tom = null,
                                                   verdi = this.sivilstand.name.storForbokstav())), // TODO: Kommer historisk data
        oppholdstillatelse = opphold.map { it.tilRestRegisterOpplysning() },
        statsborgerskap = statsborgerskap.map { it.tilRestRegisterOpplysning() },
        bostedsadresse = this.bostedsadresse?.let { listOf(it.tilRestRegisterOpplysning()) }, // TODO: Kommer historisk data)
)

data class RestRegisterHistorikk(

        val sivilstand: List<RestRegisterOpplysning>? = emptyList(),
        val oppholdstillatelse: List<RestRegisterOpplysning>? = emptyList(),
        val statsborgerskap: List<RestRegisterOpplysning>? = emptyList(),
        val bostedsadresse: List<RestRegisterOpplysning>? = emptyList(),
)

data class RestRegisterOpplysning(
        val fom: LocalDate?,
        val tom: LocalDate?,
        val verdi: String,
)