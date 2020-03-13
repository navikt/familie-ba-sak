package no.nav.familie.ba.sak.integrasjoner.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Kjønn
import java.time.LocalDate

@JsonIgnoreProperties
data class Personinfo(
        var fødselsdato: LocalDate,
        val geografiskTilknytning: String? = null,
        val diskresjonskode: String? = null,
        val navn: String? = null,
        @JsonDeserialize(using = KjonnDeserializer::class) val kjønn: Kjønn? = null,
        val familierelasjoner: Set<Familierelasjoner> = emptySet()
)

data class Familierelasjoner(
        val personIdent: Personident,
        val relasjonsrolle: String
)

data class Personident(
        val id: String
)

class KjonnDeserializer: StdDeserializer<Kjønn>(Kjønn::class.java) {
    override fun deserialize(jp: JsonParser?, p1: DeserializationContext?): Kjønn {
        val node : JsonNode = jp!!.getCodec().readTree(jp)
        val kjoenn = node.asText()
        return when(kjoenn) {
            "M" -> Kjønn.MANN
            "K" -> Kjønn.KVINNE
            else -> Kjønn.valueOf(kjoenn)
        }
    }

}