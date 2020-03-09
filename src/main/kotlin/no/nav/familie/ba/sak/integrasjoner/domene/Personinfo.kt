package no.nav.familie.ba.sak.integrasjoner.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Kjønn
import java.time.LocalDate

@JsonIgnoreProperties
data class Personinfo(
        var fødselsdato: LocalDate,
        val geografiskTilknytning: String? = null,
        val diskresjonskode: String? = null,
        val navn: String = "",
        val kjønn: Kjønn = Kjønn.UKJENT
)
