package no.nav.familie.ba.sak.integrasjoner.pdl.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

open class PdlBaseResponse<T>(
    val data: T,
    open val errors: List<PdlError>?
) {

    fun harFeil(): Boolean {
        return errors != null && errors!!.isNotEmpty()
    }

    fun errorMessages(): String {
        return errors?.joinToString { it -> it.message } ?: ""
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlError(
    val message: String,
    val extensions: PdlExtensions?
)

data class PdlExtensions(val code: String?) {

    fun notFound() = code == "not_found"
}
