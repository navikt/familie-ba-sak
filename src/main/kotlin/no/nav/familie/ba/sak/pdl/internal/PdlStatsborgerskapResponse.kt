package no.nav.familie.ba.sak.pdl.internal

import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap

data class PdlStatsborgerskapResponse(val data: Data,
                                      override val errors: List<PdlError>?)
    : PdlBaseResponse(errors) {

    class Data(val person: Person?)
    class Person(val statsborgerskap: List<Statsborgerskap>)
}





