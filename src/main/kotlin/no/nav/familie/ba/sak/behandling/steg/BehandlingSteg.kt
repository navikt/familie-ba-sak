package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.Behandling

interface BehandlingSteg<T> {
    fun utførSteg(behandling: Behandling, data: T): Behandling

    fun stegType(): StegType

    fun nesteSteg(behandling: Behandling): StegType
}

val initSteg = StegType.REGISTRERE_PERSONGRUNNLAG
val sisteSteg = StegType.BEHANDLING_AVSLUTTET

enum class StegType(val tillattFor: List<BehandlerRolle>, val beskrivelse: String) {
    REGISTRERE_PERSONGRUNNLAG(tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
                              beskrivelse = "Registrere persongrunnlag"),
    VILKÅRSVURDERING(tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER), beskrivelse = "Vilkårsvurdering"),
    SEND_TIL_BESLUTTER(tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
                       beskrivelse = "Send til beslutter"),
    GODKJENNE_VEDTAK(tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.BESLUTTER), beskrivelse = "Godkjenne vedtak"),
    FERDIGSTILLE_BEHANDLING(tillattFor = listOf(BehandlerRolle.SYSTEM), beskrivelse = "Ferdigstille behandling"),
    BEHANDLING_AVSLUTTET(tillattFor = emptyList(), beskrivelse = "Behandlingen er avsluttet og kan ikke gjenåpnes")
}

enum class BehandlerRolle(val nivå: Int) {
    SYSTEM(4),
    BESLUTTER(3),
    SAKSBEHANDLER(2),
    VEILEDER(1),
    UKJENT(0)
}