package no.nav.familie.ba.skatteetaten.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import javax.validation.constraints.DecimalMax
import javax.validation.constraints.DecimalMin
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size
import javax.validation.Valid

/**
 * Representerer en tidsperiode gitt ved en fra-og-med-måned og en valgfri til-og-med-måned, og som i tillegg inneholder en opplysning ang. maks delingsprosent i perioden
 * @param fraMaaned Første måned i perioden.
 * @param maxDelingsprosent For perioder som løper i nytt fagsystem, vil \"maxDelingsprosent\" alltid være \"50\" eller \"100\". \"usikker\" tilsvarer kode \"3\" i gammelt fagsystem
 * @param tomMaaned Den siste måneden i perioden
 */
data class Periode(

    @get:NotNull  
    @field:JsonProperty("fraMaaned") val fraMaaned: kotlin.String,

    @get:NotNull  
    @field:JsonProperty("maxDelingsprosent") val maxDelingsprosent: Periode.MaxDelingsprosent,

    @field:JsonProperty("tomMaaned") val tomMaaned: kotlin.String? = null
) {

    /**
    * For perioder som løper i nytt fagsystem, vil \"maxDelingsprosent\" alltid være \"50\" eller \"100\". \"usikker\" tilsvarer kode \"3\" i gammelt fagsystem
    * Values: _100,_50,usikker
    */
    enum class MaxDelingsprosent(val value: kotlin.String) {
    
        @JsonProperty("100") _100("100"),
    
        @JsonProperty("50") _50("50"),
    
        @JsonProperty("usikker") usikker("usikker");
    
    }

}

