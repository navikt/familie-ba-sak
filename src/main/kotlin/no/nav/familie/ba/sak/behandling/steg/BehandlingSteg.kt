package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingOpprinnelse
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType

interface BehandlingSteg<T> {
    fun utførStegOgAngiNeste(behandling: Behandling,
                             data: T): StegType

    fun stegType(): StegType

    fun hentNesteStegForNormalFlyt(behandling: Behandling): StegType {
        return behandling.steg.hentNesteSteg(utførendeStegType = this.stegType(),
                                             behandlingType = behandling.type,
                                             behandlingOpprinnelse = behandling.opprinnelse)
    }

    fun preValiderSteg(behandling: Behandling, stegService: StegService? = null) {}

    fun postValiderSteg(behandling: Behandling) {}
}

fun initSteg(behandlingType: BehandlingType? = null, behandlingOpprinnelse: BehandlingOpprinnelse? = null): StegType {
    return if (behandlingOpprinnelse == BehandlingOpprinnelse.AUTOMATISK_VED_FØDSELSHENDELSE
               || behandlingType == BehandlingType.MIGRERING_FRA_INFOTRYGD) { StegType.REGISTRERE_PERSONGRUNNLAG }
    else { StegType.REGISTRERE_SØKNAD }
}

val sisteSteg = StegType.BEHANDLING_AVSLUTTET

enum class StegType(val rekkefølge: Int,
                    val tillattFor: List<BehandlerRolle>,
                    private val gyldigIKombinasjonMedStatus: List<BehandlingStatus>) {

    REGISTRERE_SØKNAD(
            rekkefølge = 1,
            tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.OPPRETTET, BehandlingStatus.UNDERKJENT_AV_BESLUTTER)),
    REGISTRERE_PERSONGRUNNLAG(
            rekkefølge = 1,
            tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.OPPRETTET, BehandlingStatus.UNDERKJENT_AV_BESLUTTER)),
    VILKÅRSVURDERING(
            rekkefølge = 3,
            tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.OPPRETTET, BehandlingStatus.UNDERKJENT_AV_BESLUTTER)),
    SEND_TIL_BESLUTTER(
            rekkefølge = 4,
            tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.OPPRETTET, BehandlingStatus.UNDERKJENT_AV_BESLUTTER)),
    BESLUTTE_VEDTAK(
            rekkefølge = 5,
            tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.BESLUTTER),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.SENDT_TIL_BESLUTTER)),
    IVERKSETT_MOT_OPPDRAG(
            rekkefølge = 6,
            tillattFor = listOf(BehandlerRolle.SYSTEM),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.GODKJENT)
    ),
    VENTE_PÅ_STATUS_FRA_ØKONOMI(
            rekkefølge = 7,
            tillattFor = listOf(BehandlerRolle.SYSTEM),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.SENDT_TIL_IVERKSETTING)
    ),
    JOURNALFØR_VEDTAKSBREV(
            rekkefølge = 8,
            tillattFor = listOf(BehandlerRolle.SYSTEM),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.IVERKSATT)
    ),
    DISTRIBUER_VEDTAKSBREV(
            rekkefølge = 9,
            tillattFor = listOf(BehandlerRolle.SYSTEM),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.IVERKSATT)
    ),
    FERDIGSTILLE_BEHANDLING(
            rekkefølge = 10,
            tillattFor = listOf(BehandlerRolle.SYSTEM),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.IVERKSATT)),
    BEHANDLING_AVSLUTTET(
            rekkefølge = 11,
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

    fun hentNesteSteg(utførendeStegType: StegType,
                      behandlingType: BehandlingType? = null,
                      behandlingOpprinnelse: BehandlingOpprinnelse? = null): StegType {
        return if (behandlingOpprinnelse == BehandlingOpprinnelse.AUTOMATISK_VED_FØDSELSHENDELSE) {
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
                        FERDIGSTILLE_BEHANDLING -> BEHANDLING_AVSLUTTET
                        BEHANDLING_AVSLUTTET -> BEHANDLING_AVSLUTTET
                        else -> throw IllegalStateException("StegType ${utførendeStegType.displayName()} ugyldig ved behandlingstype $behandlingType")
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