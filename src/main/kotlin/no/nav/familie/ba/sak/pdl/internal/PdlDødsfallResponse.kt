package no.nav.familie.ba.sak.pdl.internal

data class PdlDødsfallResponse(val data: Data,
                               override val errors: List<PdlError>?)
    : PdlBaseResponse(errors) {

    class Data(val person: Person?)
    class Person(val doedsfall: List<Doedsfall>)
}

class Doedsfall(val doedsdato: String?)





