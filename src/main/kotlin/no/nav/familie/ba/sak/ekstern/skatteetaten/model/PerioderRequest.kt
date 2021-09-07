package no.nav.familie.ba.skatteetaten.model

import com.fasterxml.jackson.annotation.JsonProperty
import javax.validation.constraints.NotNull

/**
 *
 * @param identer Liste over fødselsnumre det ønskes opplysninger om.
 */
data class PerioderRequest(


    @get:NotNull
    @field:JsonProperty("aar") val aar: String,

    @get:NotNull
    @field:JsonProperty("identer") val identer: List<String>
)