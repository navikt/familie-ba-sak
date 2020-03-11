package no.nav.familie.ba.sak.integrasjoner.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Kjønn
import java.time.LocalDate

@JsonIgnoreProperties
data class Personinfo(
        var fødselsdato: LocalDate,
        val geografiskTilknytning: String? = null,
        val diskresjonskode: String? = null,
        val navn: String? = null,
        val kjønn: KJØNN? = null,
        val familierelasjoner: Set<Familierelasjoner> = emptySet()
)

data class Familierelasjoner(
        val personIdent: Personident,
        val relasjonsrolle: FAMILIERELASJONSROLLE
)

data class Personident(
        val id: String
)

enum class KJØNN {MANN, KVINNE, UKJENT}

enum class FAMILIERELASJONSROLLE { BARN, FAR, MEDMOR, MOR}