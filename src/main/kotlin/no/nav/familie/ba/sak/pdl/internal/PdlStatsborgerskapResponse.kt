package no.nav.familie.ba.sak.pdl.internal

import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap

data class PdlStatsborgerskapResponse(val data: Data,
                                      val errors: List<PdlError>?) {

    fun harFeil(): Boolean {
        return errors != null && errors.isNotEmpty()
    }

    fun errorMessages(): String {
        return errors?.joinToString { it -> it.message } ?: ""
    }

    class Data(val person: Person?)
    class Person(val statsborgerskap: List<Statsborgerskap>)
}





