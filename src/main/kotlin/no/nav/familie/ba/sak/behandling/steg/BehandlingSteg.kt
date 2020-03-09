package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.Behandling

interface BehandlingSteg<T> {
    fun utførSteg(behandling: Behandling, data: T): Behandling

    fun stegType(): StegType

    fun nesteSteg(behandling: Behandling): StegType
}

val initSteg = StegType.REGISTRERE_PERSONGRUNNLAG
val sisteSteg = StegType.GODKJENNE_VEDTAK

enum class StegType(val tillattFor: List<BehandlerRolle>, val beskrivelse: String) {
    REGISTRERE_PERSONGRUNNLAG(tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
                              beskrivelse = "Registrere persongrunnlag"),
    VILKÅRSVURDERING(tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER), beskrivelse = "Vilkårsvurdering"),
    FATTE_VEDTAK(tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER), beskrivelse = "Fatte vedtak"),
    GODKJENNE_VEDTAK(tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.BESLUTTER), beskrivelse = "Godkjenne vedtak"),
}

enum class BehandlerRolle {
    SYSTEM,
    SAKSBEHANDLER,
    BESLUTTER
}