package no.nav.familie.ba.sak.integrasjoner.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import java.time.LocalDate

@JsonIgnoreProperties
data class Personinfo(
        var fødselsdato: LocalDate,
        val adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING? = null,
        val navn: String? = null,
        @JsonDeserialize(using = KjonnDeserializer::class)
        val kjønn: Kjønn? = null,
        val familierelasjoner: Set<Familierelasjoner> = emptySet(),
        val bostedsadresse: Bostedsadresse? = null,
        val sivilstand: SIVILSTAND? = null
)

data class Familierelasjoner(
        val personIdent: Personident,
        val relasjonsrolle: FAMILIERELASJONSROLLE,
        val navn: String? = null,
        val fødselsdato: LocalDate? = null
)

data class Bostedsadresse(
        val vegadresse: Vegadresse? = null,
        val matrikkeladresse: Matrikkeladresse? = null,
        val ukjentBosted: UkjentBosted? = null
)

data class Vegadresse(
        val matrikkelId: String?,
        val husnummer: String?,
        val husbokstav: String?,
        val bruksenhetsnummer: String?,
        val adressenavn: String?,
        val kommunenummer: String?,
        val tilleggsnavn: String?,
        val postnummer: String?
)

data class UkjentBosted(
        val bostedskommune: String
)

data class Matrikkeladresse(
        val matrikkelId: String?,
        val bruksenhetsnummer: String?,
        val tilleggsnavn: String?,
        val postnummer: String?,
        val kommunenummer: String?
)

data class Personident(
        val id: String
)

data class Ident(
        val ident: String
)

enum class ADRESSEBESKYTTELSEGRADERING {
    STRENGT_FORTROLIG_UTLAND, // Kode 19
    FORTROLIG, // Kode 7
    STRENGT_FORTROLIG, // Kode 6
    UGRADERT
}

data class IdentInformasjon(val ident: String,
                            val historisk: Boolean,
                            val gruppe: String)

enum class FAMILIERELASJONSROLLE { BARN, FAR, MEDMOR, MOR }

enum class SIVILSTAND {
    UOPPGITT,
    UGIFT,
    GIFT,
    ENKE_ELLER_ENKEMANN,
    SKILT,
    SEPARERT,
    REGISTRERT_PARTNER,
    SEPARERT_PARTNER,
    SKILT_PARTNER,
    GJENLEVENDE_PARTNER
}

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
