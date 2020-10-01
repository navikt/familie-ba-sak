package no.nav.familie.ba.sak.pdl.internal

open class PdlBaseResponse(open val errors: List<PdlError>?) {
    fun harFeil(): Boolean {
        return errors != null && errors!!.isNotEmpty()
    }

    fun errorMessages(): String {
        return errors?.joinToString { it -> it.message } ?: ""
    }
}