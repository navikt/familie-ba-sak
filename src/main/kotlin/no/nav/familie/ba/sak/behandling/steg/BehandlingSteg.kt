package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.Behandling

interface BehandlingSteg<T> {
    fun utførSteg(behandling: Behandling, data: T): Behandling

    fun stegType(): StegType

    fun nesteSteg(behandling: Behandling): StegType
}

val initSteg = StegType.REGISTRERE_PERSONGRUNNLAG
val sisteSteg = StegType.GODKJENNE_VEDTAK

enum class StegType(val tillattFor: List<BehandlerRolle>) {
    REGISTRERE_PERSONGRUNNLAG(tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER)),
    VILKÅRSVURDERING(tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER)),
    VURDER_VEDTAK(tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER)),
    FORESLÅ_VEDTAK(tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER)),
    GODKJENNE_VEDTAK(tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.BESLUTTER)),
}

enum class BehandlerRolle {
    SYSTEM,
    SAKSBEHANDLER,
    BESLUTTER
}