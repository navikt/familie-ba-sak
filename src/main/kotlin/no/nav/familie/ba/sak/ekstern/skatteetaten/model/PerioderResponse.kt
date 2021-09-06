package no.nav.familie.ba.skatteetaten.model

import com.fasterxml.jackson.annotation.JsonProperty
import javax.validation.Valid

/**
 *
 * @param brukere
 */
data class PerioderResponse(

    @field:Valid
    @field:JsonProperty("brukere") val brukere: List<Perioder> = emptyList()
) {

}

