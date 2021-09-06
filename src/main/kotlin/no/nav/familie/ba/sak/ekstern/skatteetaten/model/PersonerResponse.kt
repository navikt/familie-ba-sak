package no.nav.familie.ba.skatteetaten.model

import com.fasterxml.jackson.annotation.JsonProperty
import javax.validation.Valid

/**
 *
 * @param brukere
 */
data class PersonerResponse(

    @field:Valid
    @field:JsonProperty("brukere") val brukere: List<Person> = emptyList()
) {

}

