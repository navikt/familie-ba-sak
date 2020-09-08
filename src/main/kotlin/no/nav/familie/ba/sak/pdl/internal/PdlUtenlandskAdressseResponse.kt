package no.nav.familie.ba.sak.pdl.internal

data class PdlUtenlandskAdressseResponse(val data: Data?,
                                         val errors: List<PdlError>?) {

    fun harFeil(): Boolean {
        return errors != null && errors.isNotEmpty()
    }

    fun errorMessages(): String {
        return errors?.joinToString { it -> it.message } ?: ""
    }

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
