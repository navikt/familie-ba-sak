package no.nav.familie.ba.skatteetaten.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime
import javax.validation.constraints.NotNull

/**
 *
 * @param ident Person identifikator
 * @param sisteVedtakPaaIdent Tidspunkt for siste vedtak (systemtidspunkt)
 */
data class Person(

    @get:NotNull
    @field:JsonProperty("ident") val ident: String,

    @get:NotNull
    @field:JsonProperty("sisteVedtakPaaIdent") val sisteVedtakPaaIdent: OffsetDateTime
) {

}

