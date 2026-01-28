package no.nav.familie.ba.sak.integrasjoner.pdl.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.Adresser
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.DeltBosted
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import no.nav.familie.kontrakter.felles.personopplysning.Oppholdsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.annotation.JsonDeserialize
import tools.jackson.databind.deser.std.StdDeserializer
import java.time.LocalDate
import java.time.Period

sealed class PdlPersonInfo {
    data class Person(
        val personInfo: PersonInfo,
    ) : PdlPersonInfo()

    data class FalskPerson(
        val falskIdentitetPersonInfo: FalskIdentitetPersonInfo,
    ) : PdlPersonInfo()

    fun personInfoBase(): PersonInfoBase =
        when (this) {
            is Person -> this.personInfo
            is FalskPerson -> this.falskIdentitetPersonInfo
        }

    fun erBarn(): Boolean = this.personInfoBase().erBarn()
}

interface PersonInfoBase {
    val fødselsdato: LocalDate?
    val navn: String?
    val kjønn: Kjønn
    val adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING?
    val erEgenAnsatt: Boolean?
    val forelderBarnRelasjon: Set<ForelderBarnRelasjon>

    fun erBarn(): Boolean = Period.between(fødselsdato, LocalDate.now()).years < 18
}

data class FalskIdentitetPersonInfo(
    override val navn: String? = "Ukjent navn",
    override val fødselsdato: LocalDate? = null,
    override val kjønn: Kjønn = Kjønn.UKJENT,
    val adresser: Adresser? = null,
) : PersonInfoBase {
    override val adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING? = null
    override val erEgenAnsatt: Boolean? = null
    override val forelderBarnRelasjon: Set<ForelderBarnRelasjon> = emptySet()
}

data class PersonInfo(
    override val fødselsdato: LocalDate,
    override val navn: String? = null,
    @JsonDeserialize(using = KjonnDeserializer::class)
    override val kjønn: Kjønn = Kjønn.UKJENT,
    // Observer at ForelderBarnRelasjon og ForelderBarnRelasjonMaskert ikke er en PDL-objekt.
    override val forelderBarnRelasjon: Set<ForelderBarnRelasjon> = emptySet(),
    val forelderBarnRelasjonMaskert: Set<ForelderBarnRelasjonMaskert> = emptySet(),
    override val adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING? = null,
    val bostedsadresser: List<Bostedsadresse> = emptyList(),
    val oppholdsadresser: List<Oppholdsadresse> = emptyList(),
    val deltBosted: List<DeltBosted> = emptyList(),
    val sivilstander: List<Sivilstand> = emptyList(),
    val opphold: List<Opphold>? = emptyList(),
    val statsborgerskap: List<Statsborgerskap>? = emptyList(),
    val dødsfall: DødsfallData? = null,
    val kontaktinformasjonForDoedsbo: PdlKontaktinformasjonForDødsbo? = null,
    override val erEgenAnsatt: Boolean? = null,
) : PersonInfoBase {
    fun eldsteBarnsFødselsdato(): LocalDate? =
        forelderBarnRelasjon
            .filter { it.fødselsdato != null && it.relasjonsrolle == FORELDERBARNRELASJONROLLE.BARN }
            .minOfOrNull { it.fødselsdato!! }
}

fun List<Bostedsadresse>.filtrerUtKunNorskeBostedsadresser() = this.filter { it.vegadresse != null || it.matrikkeladresse != null || it.ukjentBosted != null }

data class ForelderBarnRelasjon(
    val aktør: Aktør,
    val relasjonsrolle: FORELDERBARNRELASJONROLLE,
    override val navn: String? = null,
    override val fødselsdato: LocalDate? = null,
    override val adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING? = null,
    override val kjønn: Kjønn = Kjønn.UKJENT,
    override val erEgenAnsatt: Boolean? = null,
) : PersonInfoBase {
    override fun toString(): String = "ForelderBarnRelasjon(personIdent=XXX, relasjonsrolle=$relasjonsrolle, navn=XXX, fødselsdato=$fødselsdato)"

    override val forelderBarnRelasjon: Set<ForelderBarnRelasjon> = emptySet()

    fun toSecureString(): String = "ForelderBarnRelasjon(personIdent=${aktør.aktivFødselsnummer()}, relasjonsrolle=$relasjonsrolle, navn=XXX, fødselsdato=$fødselsdato)"
}

data class ForelderBarnRelasjonMaskert(
    val relasjonsrolle: FORELDERBARNRELASJONROLLE,
    val adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING,
) {
    override fun toString(): String = "ForelderBarnRelasjonMaskert(relasjonsrolle=$relasjonsrolle)"
}

data class Personident(
    val id: String,
)

data class DødsfallData(
    val erDød: Boolean,
    val dødsdato: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlKontaktinformasjonForDødsbo(
    val adresse: PdlKontaktinformasjonForDødsboAdresse,
)

data class PdlKontaktinformasjonForDødsboAdresse(
    val adresselinje1: String,
    val poststedsnavn: String,
    val postnummer: String,
)

data class VergeData(
    val harVerge: Boolean,
)

class KjonnDeserializer : StdDeserializer<Kjønn>(Kjønn::class.java) {
    override fun deserialize(
        jp: JsonParser,
        p1: DeserializationContext,
    ): Kjønn {
        val node: JsonNode = jp.readValueAsTree()
        return when (val kjønn = node.asString()) {
            "M" -> Kjønn.MANN
            "K" -> Kjønn.KVINNE
            else -> Kjønn.valueOf(kjønn)
        }
    }
}
