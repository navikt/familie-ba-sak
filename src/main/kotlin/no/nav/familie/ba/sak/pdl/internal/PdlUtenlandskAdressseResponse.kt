package no.nav.familie.ba.sak.pdl.internal

data class PdlUtenlandskAdressseResponse(val data: Data?,
                                         override val errors: List<PdlError>?)
    : PdlBaseResponse(errors) {

    class Data(val person: Person?)
    class Person(val bostedsadresse: List<Bostedsadresse>)
    class Bostedsadresse(val utenlandskAdresse: UtenlandskAdresse?)
    class UtenlandskAdresse(val adressenavnNummer: String?,
                            val bygningEtasjeLeilighet: String?,
                            val postboksNummerNavn: String?,
                            val postkode: String?,
                            val bySted: String?,
                            val regionDistriktOmraade: String?,
                            val landkode: String)
}
