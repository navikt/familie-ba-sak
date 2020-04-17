package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingType

interface BehandlingSteg<T> {
    fun utførSteg(behandling: Behandling, data: T): Behandling

    fun stegType(): StegType
}

fun initSteg(behandlingType: BehandlingType?): StegType {
    return if (behandlingType == BehandlingType.MIGRERING_FRA_INFOTRYGD) {
        StegType.REGISTRERE_PERSONGRUNNLAG
    } else {
        StegType.REGISTRERE_SØKNAD
    }
}

val sisteSteg = StegType.BEHANDLING_AVSLUTTET

enum class StegType(val rekkefølge: Int, val tillattFor: List<BehandlerRolle>, val beskrivelse: String) {
    REGISTRERE_SØKNAD(
            rekkefølge = 1,
            tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
            beskrivelse = "Registrere søknad"),
    REGISTRERE_PERSONGRUNNLAG(
            rekkefølge = 1,
            tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
            beskrivelse = "Registrere persongrunnlag"),
    VILKÅRSVURDERING(
            rekkefølge = 2,
            tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
            beskrivelse = "Vilkårsvurdering"),
    SEND_TIL_BESLUTTER(
            rekkefølge = 3,
            tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
            beskrivelse = "Send til beslutter"),
    BESLUTTE_VEDTAK(
            rekkefølge = 4,
            tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.BESLUTTER),
            beskrivelse = "Godkjenne vedtak"),
    FERDIGSTILLE_BEHANDLING(
            rekkefølge = 5,
            tillattFor = listOf(BehandlerRolle.SYSTEM), beskrivelse = "Ferdigstille behandling"),
    BEHANDLING_AVSLUTTET(
            rekkefølge = 6,
            tillattFor = emptyList(),
            beskrivelse = "Behandlingen er avsluttet og kan ikke gjenåpnes");

    fun hentNesteSteg(behandlingType: BehandlingType): StegType {
        return when(behandlingType) {
            BehandlingType.MIGRERING_FRA_INFOTRYGD -> when (this) {
                REGISTRERE_PERSONGRUNNLAG -> VILKÅRSVURDERING
                VILKÅRSVURDERING -> SEND_TIL_BESLUTTER
                SEND_TIL_BESLUTTER -> BESLUTTE_VEDTAK
                BESLUTTE_VEDTAK -> FERDIGSTILLE_BEHANDLING
                FERDIGSTILLE_BEHANDLING -> BEHANDLING_AVSLUTTET
                BEHANDLING_AVSLUTTET -> BEHANDLING_AVSLUTTET
                else -> error("Ikke godkjent steg for behandlingstype")
            }
            else -> when (this) {
                REGISTRERE_SØKNAD -> REGISTRERE_PERSONGRUNNLAG
                REGISTRERE_PERSONGRUNNLAG -> VILKÅRSVURDERING
                VILKÅRSVURDERING -> SEND_TIL_BESLUTTER
                SEND_TIL_BESLUTTER -> BESLUTTE_VEDTAK
                BESLUTTE_VEDTAK -> FERDIGSTILLE_BEHANDLING
                FERDIGSTILLE_BEHANDLING -> BEHANDLING_AVSLUTTET
                BEHANDLING_AVSLUTTET -> BEHANDLING_AVSLUTTET
            }
        }
    }
}

enum class BehandlerRolle(val nivå: Int) {
    SYSTEM(4),
    BESLUTTER(3),
    SAKSBEHANDLER(2),
    VEILEDER(1),
    UKJENT(0)
}