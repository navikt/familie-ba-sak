package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType

interface BehandlingSteg<T> {

    fun utførStegOgAngiNeste(behandling: Behandling,
                             data: T): StegType

    fun stegType(): StegType

    fun hentNesteStegForNormalFlyt(behandling: Behandling): StegType {
        return behandling.steg.hentNesteSteg(utførendeStegType = this.stegType(),
                                             behandlingType = behandling.type,
                                             behandlingÅrsak = behandling.opprettetÅrsak)
    }

    fun preValiderSteg(behandling: Behandling, stegService: StegService? = null) {}

    fun postValiderSteg(behandling: Behandling) {}
}

fun initSteg(behandlingType: BehandlingType? = null, behandlingÅrsak: BehandlingÅrsak? = null): StegType {
    return if (behandlingÅrsak == BehandlingÅrsak.FØDSELSHENDELSE
               || behandlingType == BehandlingType.MIGRERING_FRA_INFOTRYGD) {
        StegType.REGISTRERE_PERSONGRUNNLAG
    } else {
        StegType.REGISTRERE_SØKNAD
    }
}

val sisteSteg = StegType.BEHANDLING_AVSLUTTET

enum class StegType(val rekkefølge: Int,
                    val tillattFor: List<BehandlerRolle>,
                    private val gyldigIKombinasjonMedStatus: List<BehandlingStatus>) {

    REGISTRERE_SØKNAD(
            rekkefølge = 1,
            tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES)),
    REGISTRERE_PERSONGRUNNLAG(
            rekkefølge = 1,
            tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES)),
    VILKÅRSVURDERING(
            rekkefølge = 2,
            tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES)),
    SEND_TIL_BESLUTTER(
            rekkefølge = 3,
            tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES)),
    BESLUTTE_VEDTAK(
            rekkefølge = 4,
            tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.BESLUTTER),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.FATTER_VEDTAK)),
    HENLEGG_SØKNAD(
            rekkefølge = 5,
            tillattFor = listOf(BehandlerRolle.SAKSBEHANDLER),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES, BehandlingStatus.FATTER_VEDTAK)),
    IVERKSETT_MOT_OPPDRAG(
            rekkefølge = 5,
            tillattFor = listOf(BehandlerRolle.SYSTEM),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.IVERKSETTER_VEDTAK)
    ),
    VENTE_PÅ_STATUS_FRA_ØKONOMI(
            rekkefølge = 6,
            tillattFor = listOf(BehandlerRolle.SYSTEM),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.IVERKSETTER_VEDTAK)
    ),
    JOURNALFØR_VEDTAKSBREV(
            rekkefølge = 7,
            tillattFor = listOf(BehandlerRolle.SYSTEM),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.IVERKSETTER_VEDTAK)
    ),
    DISTRIBUER_VEDTAKSBREV(
            rekkefølge = 8,
            tillattFor = listOf(BehandlerRolle.SYSTEM),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.IVERKSETTER_VEDTAK)
    ),
    FERDIGSTILLE_BEHANDLING(
            rekkefølge = 9,
            tillattFor = listOf(BehandlerRolle.SYSTEM),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.IVERKSETTER_VEDTAK)),
    BEHANDLING_AVSLUTTET(
            rekkefølge = 10,
            tillattFor = emptyList(),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.AVSLUTTET));

    fun displayName(): String {
        return this.name.replace('_', ' ').toLowerCase().capitalize()
    }

    fun kommerEtter(steg: StegType): Boolean {
        return this.rekkefølge > steg.rekkefølge
    }

    fun erGyldigIKombinasjonMedStatus(behandlingStatus: BehandlingStatus): Boolean {
        return this.gyldigIKombinasjonMedStatus.contains(behandlingStatus)
    }

    fun erSaksbehandlerSteg(): Boolean {
        return this.tillattFor.any { it == BehandlerRolle.SAKSBEHANDLER || it == BehandlerRolle.BESLUTTER }
    }

    fun hentNesteSteg(utførendeStegType: StegType,
                      behandlingType: BehandlingType? = null,
                      behandlingÅrsak: BehandlingÅrsak? = null): StegType {
        return if (behandlingÅrsak == BehandlingÅrsak.FØDSELSHENDELSE) {
            when (utførendeStegType) {
                REGISTRERE_PERSONGRUNNLAG -> VILKÅRSVURDERING
                VILKÅRSVURDERING -> IVERKSETT_MOT_OPPDRAG
                IVERKSETT_MOT_OPPDRAG -> VENTE_PÅ_STATUS_FRA_ØKONOMI
                VENTE_PÅ_STATUS_FRA_ØKONOMI -> JOURNALFØR_VEDTAKSBREV
                JOURNALFØR_VEDTAKSBREV -> DISTRIBUER_VEDTAKSBREV
                DISTRIBUER_VEDTAKSBREV -> FERDIGSTILLE_BEHANDLING
                FERDIGSTILLE_BEHANDLING -> BEHANDLING_AVSLUTTET
                BEHANDLING_AVSLUTTET -> BEHANDLING_AVSLUTTET
                else -> throw IllegalStateException("Stegtype ${utførendeStegType.displayName()} er ikke implementert for fødselshendelser")
            }
        } else {
            when (behandlingType) {
                BehandlingType.TEKNISK_OPPHØR, BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT, BehandlingType.MIGRERING_FRA_INFOTRYGD ->
                    when (utførendeStegType) {
                        REGISTRERE_SØKNAD -> REGISTRERE_PERSONGRUNNLAG
                        REGISTRERE_PERSONGRUNNLAG -> VILKÅRSVURDERING
                        VILKÅRSVURDERING -> SEND_TIL_BESLUTTER
                        SEND_TIL_BESLUTTER -> BESLUTTE_VEDTAK
                        BESLUTTE_VEDTAK -> IVERKSETT_MOT_OPPDRAG
                        IVERKSETT_MOT_OPPDRAG -> VENTE_PÅ_STATUS_FRA_ØKONOMI
                        VENTE_PÅ_STATUS_FRA_ØKONOMI -> FERDIGSTILLE_BEHANDLING
                        HENLEGG_SØKNAD -> FERDIGSTILLE_BEHANDLING
                        FERDIGSTILLE_BEHANDLING -> BEHANDLING_AVSLUTTET
                        BEHANDLING_AVSLUTTET -> BEHANDLING_AVSLUTTET
                        else -> throw IllegalStateException("StegType ${utførendeStegType.displayName()} ugyldig ved teknisk opphør")
                    }
                else ->
                    when (utførendeStegType) {
                        REGISTRERE_SØKNAD -> REGISTRERE_PERSONGRUNNLAG
                        REGISTRERE_PERSONGRUNNLAG -> VILKÅRSVURDERING
                        VILKÅRSVURDERING -> SEND_TIL_BESLUTTER
                        SEND_TIL_BESLUTTER -> BESLUTTE_VEDTAK
                        BESLUTTE_VEDTAK -> IVERKSETT_MOT_OPPDRAG
                        IVERKSETT_MOT_OPPDRAG -> VENTE_PÅ_STATUS_FRA_ØKONOMI
                        VENTE_PÅ_STATUS_FRA_ØKONOMI -> JOURNALFØR_VEDTAKSBREV
                        JOURNALFØR_VEDTAKSBREV -> DISTRIBUER_VEDTAKSBREV
                        DISTRIBUER_VEDTAKSBREV -> FERDIGSTILLE_BEHANDLING
                        HENLEGG_SØKNAD -> FERDIGSTILLE_BEHANDLING
                        FERDIGSTILLE_BEHANDLING -> BEHANDLING_AVSLUTTET
                        BEHANDLING_AVSLUTTET -> BEHANDLING_AVSLUTTET
                    }
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

enum class BehandlingStegStatus(val navn: String, val beskrivelse: String) {
    STARTET("STARTET", "Steget er startet"),
    INNGANG("INNGANG", "Inngangkriterier er ikke oppfylt"),
    UTGANG("UTGANG", "Utgangskriterier er ikke oppfylt"),
    VENTER("VENTER", "På vent"),
    AVBRUTT("AVBRUTT", "Avbrutt"),
    UTFØRT("UTFØRT", "Utført"),
    FREMOVERFØRT("FREMOVERFØRT", "Fremoverført"),
    TILBAKEFØRT("TILBAKEFØRT", "Tilbakeført"),
    UDEFINERT("-", "Ikke definert")
}