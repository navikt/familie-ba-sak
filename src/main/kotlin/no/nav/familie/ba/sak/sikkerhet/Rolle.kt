package no.nav.familie.ba.sak.sikkerhet

enum class Rolle {
    VEILEDER,
    FORVALTER,
    SAKSBEHANDLER,
    BESLUTTER,
    TEAMFAMILIE_APPLIKASJON,
    PROSESSERING,
    KLAGE_APPLIKASJON,
    PENSJON_APPLIKASJON,
    BISYS_APPLIKASJON,
    ;

    fun authority(): String = "ROLE_$name"

    companion object {
        fun rollerMedInternTilgang() = setOf(VEILEDER.name, FORVALTER.name, SAKSBEHANDLER.name, BESLUTTER.name, TEAMFAMILIE_APPLIKASJON.name).toTypedArray()
    }
}
