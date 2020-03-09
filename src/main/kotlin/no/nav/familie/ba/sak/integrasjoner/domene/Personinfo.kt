package no.nav.familie.ba.sak.integrasjoner.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

@JsonIgnoreProperties
data class Personinfo(
        var fødselsdato: LocalDate,
        val geografiskTilknytning: String? = null,
        val diskresjonskode: String? = null,
        val navn: String? = null,
        val kjønn: String? = null,
        val familierelasjoner: Set<Familierelasjoner> = emptySet()
)

data class Familierelasjoner(
        val personIdent: Personident,
        val relasjonsrolle: String
)

data class Personident(
        val id: String
)