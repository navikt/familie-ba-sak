package no.nav.familie.ba.sak.pdl.internal

data class PdlVergeResponse(val data: Data,
                            val errors: List<PdlError>?) {

    fun harFeil(): Boolean {
        return errors != null && errors.isNotEmpty()
    }

    fun errorMessages(): String {
        return errors?.joinToString { it -> it.message } ?: ""
    }

    class Data(val person: Person?)
    class Person(val vergemaalEllerFremtidsfullmakt: List<VergemaalEllerFremtidsfullmakt>)
}

class VergemaalEllerFremtidsfullmakt(val type: String?)





