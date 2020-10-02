package no.nav.familie.ba.sak.pdl.internal

import java.time.LocalDateTime

data class PdlBostedsadresseperioderResponse (val data: Data,
                                              override val errors: List<PdlError>?)
    : PdlBaseResponse(errors) {

    class Data(val person: Person?)
    class Person(val bostedsadresse: List<Bostedsadresseperiode>)
}

class Bostedsadresseperiode(
        val gyldigFraOgMed : LocalDateTime?,
        val gyldigTilOgMed : LocalDateTime?
)