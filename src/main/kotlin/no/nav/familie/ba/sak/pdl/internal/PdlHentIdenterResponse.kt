package no.nav.familie.ba.sak.pdl.internal

data class PdlHentIdenterResponse(val data: Data,
                                  val errors: List<PdlError>?) {

    fun harFeil(): Boolean {
        return errors != null && errors.isNotEmpty()
    }

    fun errorMessages(): String {
        return errors?.joinToString { it -> it.message } ?: ""
    }
}
data class Data(val pdlIdenter: PdlIdenter?)

data class PdlIdenter(val identer: List<IdentInformasjon>)

data class IdentInformasjon(val ident: String,
                            val historisk: Boolean,
                            val gruppe: String)
