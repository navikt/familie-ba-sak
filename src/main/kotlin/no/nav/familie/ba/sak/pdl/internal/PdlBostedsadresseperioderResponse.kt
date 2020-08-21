package no.nav.familie.ba.sak.pdl.internal

import java.time.LocalDate

data class PdlBostedsadresseperioderResponse (val data: Data,
                                              val errors: List<PdlError>?) {

    fun harFeil(): Boolean {
        return errors != null && errors.isNotEmpty()
    }

    fun errorMessages(): String {
        return errors?.joinToString { it -> it.message } ?: ""
    }

    class Data(val person: Person?)
    class Person(val bostedsadresse: List<Bostedsadresseperiode>)
}

class Bostedsadresseperiode(
        val gyldigFraOgMed : LocalDate?,
        val gyldigTilOgMed : LocalDate?
)