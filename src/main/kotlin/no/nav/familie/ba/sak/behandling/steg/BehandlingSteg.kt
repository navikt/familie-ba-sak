package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType

interface BehandlingSteg<T> {
    fun utførStegOgAngiNeste(behandling: Behandling, data: T): StegType

    fun stegType(): StegType

    fun hentNesteStegForNormalFlyt(behandling: Behandling): StegType {
        return behandling.steg.hentNesteSteg(behandlingType = behandling.type)
    }
}

fun initSteg(behandlingType: BehandlingType?): StegType {
    return if (behandlingType == BehandlingType.MIGRERING_FRA_INFOTRYGD) {
        StegType.REGISTRERE_PERSONGRUNNLAG
    } else {
        StegType.REGISTRERE_SØKNAD
    }
}

val sisteSteg = StegType.BEHANDLING_AVSLUTTET

enum class StegType(private val rekkefølge: Int, val tillattFor: List<BehandlerRolle>, private val gyldigIKombinasjonMedStatus: List<BehandlingStatus>) {
    REGISTRERE_SØKNAD(
            rekkefølge = 1,
            tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.OPPRETTET, BehandlingStatus.UNDERKJENT_AV_BESLUTTER)),
    REGISTRERE_PERSONGRUNNLAG(
            rekkefølge = 1,
            tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.OPPRETTET, BehandlingStatus.UNDERKJENT_AV_BESLUTTER)),
    VILKÅRSVURDERING(
            rekkefølge = 2,
            tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.OPPRETTET, BehandlingStatus.UNDERKJENT_AV_BESLUTTER)),
    SEND_TIL_BESLUTTER(
            rekkefølge = 3,
            tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.OPPRETTET, BehandlingStatus.UNDERKJENT_AV_BESLUTTER)),
    BESLUTTE_VEDTAK(
            rekkefølge = 4,
            tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.BESLUTTER),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.SENDT_TIL_BESLUTTER)),
    FERDIGSTILLE_BEHANDLING(
            rekkefølge = 5,
            tillattFor = listOf(BehandlerRolle.SYSTEM),
            gyldigIKombinasjonMedStatus = listOf(
                    BehandlingStatus.GODKJENT,
                    BehandlingStatus.SENDT_TIL_IVERKSETTING,
                    BehandlingStatus.IVERKSATT)),
    BEHANDLING_AVSLUTTET(
            rekkefølge = 6,
            tillattFor = emptyList(),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.FERDIGSTILT));

    fun displayName(): String {
        return this.name.replace('_', ' ').toLowerCase().capitalize()
    }

    fun kommerEtter(steg: StegType): Boolean {
        return this.rekkefølge > steg.rekkefølge
    }

    fun erGyldigIKombinasjonMedStatus(behandlingStatus: BehandlingStatus): Boolean {
        return this.gyldigIKombinasjonMedStatus.contains(behandlingStatus)
    }

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