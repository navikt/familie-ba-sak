package no.nav.familie.ba.sak.pdl.internal

data class PdlAdressebeskyttelseResponse(val data: Data,
                                         override val errors: List<PdlError>?)
    : PdlBaseResponse(errors) {

    class Data(val person: Person?)
    class Person(val adressebeskyttelse: List<Adressebeskyttelse>)
}





