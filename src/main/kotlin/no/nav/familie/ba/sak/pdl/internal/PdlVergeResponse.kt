package no.nav.familie.ba.sak.pdl.internal

data class PdlVergeResponse(val data: Data,
                            override val errors: List<PdlError>?)
    : PdlBaseResponse(errors) {

    class Data(val person: Person?)
    class Person(val vergemaalEllerFremtidsfullmakt: List<VergemaalEllerFremtidsfullmakt>)
}

class VergemaalEllerFremtidsfullmakt(val type: String?)





