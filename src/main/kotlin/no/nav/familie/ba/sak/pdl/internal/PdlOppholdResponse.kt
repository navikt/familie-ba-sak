package no.nav.familie.ba.sak.pdl.internal

import no.nav.familie.kontrakter.felles.personopplysning.Opphold

data class PdlOppholdResponse(val data: Data?,
                              override val errors: List<PdlError>?)
    : PdlBaseResponse(errors) {

    class Data(val person: Person?)
    class Person(val opphold: List<Opphold>?)
}