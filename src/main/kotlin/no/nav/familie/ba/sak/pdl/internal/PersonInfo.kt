package no.nav.familie.ba.sak.pdl.internal

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.kontrakter.felles.personinfo.Bostedsadresse
import no.nav.familie.kontrakter.felles.personinfo.SIVILSTAND
import java.time.LocalDate

data class PersonInfo(
        val fødselsdato: LocalDate,
        val navn: String? = null,
        @JsonDeserialize(using = KjonnDeserializer::class)
        val kjønn: Kjønn? = null,
        val familierelasjoner: Set<Familierelasjon> = emptySet(),
        val adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING? = null,
        val bostedsadresse: Bostedsadresse? = null,
        val sivilstand: SIVILSTAND? = null
)

data class Familierelasjon(
        val personIdent: Personident,
        val relasjonsrolle: FAMILIERELASJONSROLLE,
        val navn: String? = null,
        val fødselsdato: LocalDate? = null
)

data class Personident(
        val id: String
)

data class DødsfallData(val erDød: Boolean,
                        val dødsdato: String?)

data class VergeData(val harVerge: Boolean)

class KjonnDeserializer : StdDeserializer<Kjønn>(Kjønn::class.java) {
    override fun deserialize(jp: JsonParser?, p1: DeserializationContext?): Kjønn {
        val node: JsonNode = jp!!.codec.readTree(jp)
        return when (val kjønn = node.asText()) {
            "M" -> Kjønn.MANN
            "K" -> Kjønn.KVINNE
            else -> Kjønn.valueOf(kjønn)
        }
    }
}

