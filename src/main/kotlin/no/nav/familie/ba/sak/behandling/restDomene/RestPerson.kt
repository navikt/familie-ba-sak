package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.common.Utils.storForbokstav
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import java.time.LocalDate

data class RestPerson(
        val type: PersonType,
        val fødselsdato: LocalDate?,
        val personIdent: String,
        val navn: String,
        val kjønn: Kjønn,
        val sivilstand: List<RestSivilstand>? = emptyList(),
        val oppholdstillatelse: List<RestOpphold>? = emptyList(),
        val statsborgerskap: List<RestStatsborgerskap>? = emptyList(),
        val bostedsadresse: List<RestBostedsadresse>? = emptyList(),
        val målform: Målform
)

fun Person.tilRestPerson() = RestPerson(
        type = this.type,
        fødselsdato = this.fødselsdato,
        personIdent = this.personIdent.ident,
        navn = this.navn,
        kjønn = this.kjønn,
        sivilstand = listOf(RestSivilstand(fom = null, tom = null, sivilstand = this.sivilstand.name.storForbokstav())), // TODO: Kommer historisk data
        oppholdstillatelse = opphold.map { it.tilRestOpphold() },
        statsborgerskap = statsborgerskap.map { it.tilRestStatsborgerskap() },
        bostedsadresse = this.bostedsadresse?.let { listOf(it.tilRestBostedsadresse()) }, // TODO: Kommer historisk data
        målform = this.målform
)

interface RestHistoriskOpplysning {

    val fom: LocalDate?
    val tom: LocalDate?
}

data class RestOpphold(
        override val fom: LocalDate?,
        override val tom: LocalDate?,
        val oppholdstillatelse: String) : RestHistoriskOpplysning

data class RestStatsborgerskap(
        override val fom: LocalDate?,
        override val tom: LocalDate?,
        val landkode: String
) : RestHistoriskOpplysning

data class RestBostedsadresse(
        override val fom: LocalDate?,
        override val tom: LocalDate?,
        val bostedsadresse: String
) : RestHistoriskOpplysning

data class RestSivilstand(
        override val fom: LocalDate?,
        override val tom: LocalDate?,
        val sivilstand: String
) : RestHistoriskOpplysning