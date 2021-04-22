package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.behandling.steg.StegType.*

interface BehandlingSteg<T> {

    fun utførStegOgAngiNeste(behandling: Behandling,
                             data: T): StegType

    fun stegType(): StegType

    fun hentNesteStegForNormalFlyt(behandling: Behandling): StegType {
        return hentNesteSteg(
                utførendeStegType = this.stegType(),
                behandling = behandling
        )
    }

    fun preValiderSteg(behandling: Behandling, stegService: StegService? = null) {}

    fun postValiderSteg(behandling: Behandling) {}
}

val FØRSTE_STEG = REGISTRERE_PERSONGRUNNLAG
val SISTE_STEG = BEHANDLING_AVSLUTTET

enum class StegType(val rekkefølge: Int,
                    val tillattFor: List<BehandlerRolle>,
                    private val gyldigIKombinasjonMedStatus: List<BehandlingStatus>) {

    // Henlegg søknad går utenfor den normale stegflyten og går direkte til ferdigstilt.
    // Denne typen av steg skal bli endret til å bli av type aksjonspunkt isteden for steg.
    HENLEGG_SØKNAD(
            rekkefølge = 0,
            tillattFor = listOf(BehandlerRolle.SAKSBEHANDLER),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES)),
    REGISTRERE_PERSONGRUNNLAG(
            rekkefølge = 1,
            tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES)),
    REGISTRERE_SØKNAD(
            rekkefølge = 1,
            tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES)),
    VILKÅRSVURDERING(
            rekkefølge = 2,
            tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES)),
    SIMULERING(
            rekkefølge = 3,
            tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES)),
    SEND_TIL_BESLUTTER(
            rekkefølge = 4,
            tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES)),
    BESLUTTE_VEDTAK(
            rekkefølge = 5,
            tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.BESLUTTER),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.FATTER_VEDTAK)),
    IVERKSETT_MOT_OPPDRAG(
            rekkefølge = 6,
            tillattFor = listOf(BehandlerRolle.SYSTEM),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.IVERKSETTER_VEDTAK)
    ),
    VENTE_PÅ_STATUS_FRA_ØKONOMI(
            rekkefølge = 7,
            tillattFor = listOf(BehandlerRolle.SYSTEM),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.IVERKSETTER_VEDTAK)
    ),
    JOURNALFØR_VEDTAKSBREV(
            rekkefølge = 8,
            tillattFor = listOf(BehandlerRolle.SYSTEM),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.IVERKSETTER_VEDTAK)
    ),
    DISTRIBUER_VEDTAKSBREV(
            rekkefølge = 9,
            tillattFor = listOf(BehandlerRolle.SYSTEM),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.IVERKSETTER_VEDTAK)
    ),
    FERDIGSTILLE_BEHANDLING(
            rekkefølge = 10,
            tillattFor = listOf(BehandlerRolle.SYSTEM),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.IVERKSETTER_VEDTAK, BehandlingStatus.UTREDES)),
    BEHANDLING_AVSLUTTET(
            rekkefølge = 11,
            tillattFor = emptyList(),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.AVSLUTTET, BehandlingStatus.UTREDES));

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
}

fun hentNesteSteg(behandling: Behandling, utførendeStegType: StegType): StegType {
    if (utførendeStegType == HENLEGG_SØKNAD) {
        return FERDIGSTILLE_BEHANDLING
    }

    val behandlingType = behandling.type
    val behandlingÅrsak = behandling.opprettetÅrsak

    if (behandlingType == BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT
        || behandlingType == BehandlingType.MIGRERING_FRA_INFOTRYGD) {
        return when (utførendeStegType) {
            REGISTRERE_PERSONGRUNNLAG -> VILKÅRSVURDERING
            VILKÅRSVURDERING -> IVERKSETT_MOT_OPPDRAG
            IVERKSETT_MOT_OPPDRAG -> VENTE_PÅ_STATUS_FRA_ØKONOMI
            VENTE_PÅ_STATUS_FRA_ØKONOMI -> FERDIGSTILLE_BEHANDLING
            FERDIGSTILLE_BEHANDLING -> BEHANDLING_AVSLUTTET
            BEHANDLING_AVSLUTTET -> BEHANDLING_AVSLUTTET
            else -> throw IllegalStateException("StegType ${utførendeStegType.displayName()} ugyldig ved migrering")
        }
    }

    return when (behandlingÅrsak) {
        BehandlingÅrsak.TEKNISK_OPPHØR -> {
            when (utførendeStegType) {
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
        }
        BehandlingÅrsak.FØDSELSHENDELSE -> {
            when (utførendeStegType) {
                REGISTRERE_PERSONGRUNNLAG -> VILKÅRSVURDERING
                VILKÅRSVURDERING -> hentNesteStegTypeBasertPåBehandlingsresultat(behandling.resultat)
                IVERKSETT_MOT_OPPDRAG -> VENTE_PÅ_STATUS_FRA_ØKONOMI
                VENTE_PÅ_STATUS_FRA_ØKONOMI -> JOURNALFØR_VEDTAKSBREV
                JOURNALFØR_VEDTAKSBREV -> DISTRIBUER_VEDTAKSBREV
                DISTRIBUER_VEDTAKSBREV -> FERDIGSTILLE_BEHANDLING
                FERDIGSTILLE_BEHANDLING -> BEHANDLING_AVSLUTTET
                BEHANDLING_AVSLUTTET -> BEHANDLING_AVSLUTTET
                else -> throw IllegalStateException("Stegtype ${utførendeStegType.displayName()} er ikke implementert for fødselshendelser")
            }
        }
        BehandlingÅrsak.SØKNAD -> {
            when (utførendeStegType) {
                REGISTRERE_PERSONGRUNNLAG -> REGISTRERE_SØKNAD
                REGISTRERE_SØKNAD -> VILKÅRSVURDERING
                VILKÅRSVURDERING -> SIMULERING
                SIMULERING -> SEND_TIL_BESLUTTER
                SEND_TIL_BESLUTTER -> BESLUTTE_VEDTAK
                BESLUTTE_VEDTAK -> hentNesteStegTypeBasertPåBehandlingsresultat(behandling.resultat)
                IVERKSETT_MOT_OPPDRAG -> VENTE_PÅ_STATUS_FRA_ØKONOMI
                VENTE_PÅ_STATUS_FRA_ØKONOMI -> JOURNALFØR_VEDTAKSBREV
                JOURNALFØR_VEDTAKSBREV -> DISTRIBUER_VEDTAKSBREV
                DISTRIBUER_VEDTAKSBREV -> FERDIGSTILLE_BEHANDLING
                FERDIGSTILLE_BEHANDLING -> BEHANDLING_AVSLUTTET
                BEHANDLING_AVSLUTTET -> BEHANDLING_AVSLUTTET
                else -> throw IllegalStateException("Stegtype ${utførendeStegType.displayName()} er ikke implementert for behandling med årsak $behandlingÅrsak og type $behandlingType.")
            }
        }
        BehandlingÅrsak.OMREGNING_18ÅR, BehandlingÅrsak.OMREGNING_6ÅR -> {
            when (utførendeStegType) {
                REGISTRERE_PERSONGRUNNLAG -> VILKÅRSVURDERING
                VILKÅRSVURDERING -> JOURNALFØR_VEDTAKSBREV
                JOURNALFØR_VEDTAKSBREV -> DISTRIBUER_VEDTAKSBREV
                DISTRIBUER_VEDTAKSBREV -> FERDIGSTILLE_BEHANDLING
                FERDIGSTILLE_BEHANDLING -> BEHANDLING_AVSLUTTET
                BEHANDLING_AVSLUTTET -> BEHANDLING_AVSLUTTET
                else -> throw IllegalStateException("Stegtype ${utførendeStegType.displayName()} er ikke implementert for behandling med årsak $behandlingÅrsak og type $behandlingType.")
            }
        }
        else -> {
            when (utførendeStegType) {
                REGISTRERE_PERSONGRUNNLAG -> VILKÅRSVURDERING
                VILKÅRSVURDERING -> SIMULERING
                SIMULERING -> SEND_TIL_BESLUTTER
                SEND_TIL_BESLUTTER -> BESLUTTE_VEDTAK
                BESLUTTE_VEDTAK -> hentNesteStegTypeBasertPåBehandlingsresultat(behandling.resultat)
                IVERKSETT_MOT_OPPDRAG -> VENTE_PÅ_STATUS_FRA_ØKONOMI
                VENTE_PÅ_STATUS_FRA_ØKONOMI -> JOURNALFØR_VEDTAKSBREV
                JOURNALFØR_VEDTAKSBREV -> DISTRIBUER_VEDTAKSBREV
                DISTRIBUER_VEDTAKSBREV -> FERDIGSTILLE_BEHANDLING
                FERDIGSTILLE_BEHANDLING -> BEHANDLING_AVSLUTTET
                BEHANDLING_AVSLUTTET -> BEHANDLING_AVSLUTTET
                else -> throw IllegalStateException("Stegtype ${utførendeStegType.displayName()} er ikke implementert for behandling med årsak $behandlingÅrsak og type $behandlingType.")
            }
        }
    }
}

fun hentNesteStegTypeBasertPåBehandlingsresultat(resultat: BehandlingResultat): StegType {
    return when (resultat) {
        BehandlingResultat.FORTSATT_INNVILGET, BehandlingResultat.AVSLÅTT -> JOURNALFØR_VEDTAKSBREV
        else -> IVERKSETT_MOT_OPPDRAG
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
    IKKE_UTFØRT("IKKE_UTFØRT", "Steget er ikke utført"),
    UTFØRT("UTFØRT", "Utført")
}