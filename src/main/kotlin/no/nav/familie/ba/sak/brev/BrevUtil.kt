package no.nav.familie.ba.sak.brev

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat.*
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.brev.domene.maler.Vedtaksbrevtype
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.totrinnskontroll.domene.Totrinnskontroll

fun hentVedtaksbrevtype(behandling: Behandling): Vedtaksbrevtype {
    if (behandling.resultat == IKKE_VURDERT) {
        throw Feil("Kan ikke opprette brev. Behandlingen er ikke vurdert.")
    }

    return if (behandling.skalBehandlesAutomatisk)
        hentAutomatiskVedtaksbrevtype(behandling.opprettetÅrsak)

    else {
        hentManuellVedtaksbrevtype(behandling.type, behandling.resultat)
    }
}

private fun hentAutomatiskVedtaksbrevtype(behandlingÅrsak: BehandlingÅrsak) =
        when (behandlingÅrsak) {
            BehandlingÅrsak.OMREGNING_6ÅR -> Vedtaksbrevtype.AUTOVEDTAK_BARN6_ÅR
            BehandlingÅrsak.OMREGNING_18ÅR -> Vedtaksbrevtype.AUTOVEDTAK_BARN18_ÅR
            else -> throw Feil("Det er ikke laget funksjonalitet for automatisk behandling for ${behandlingÅrsak}")
        }

fun hentManuellVedtaksbrevtype(behandlingType: BehandlingType,
                               behandlingResultat: BehandlingResultat): Vedtaksbrevtype {
    val feilmeldingBehandlingTypeOgResultat =
            "Brev ikke støttet for behandlingstype=${behandlingType} og behandlingsresultat=${behandlingResultat}"
    val feilmelidingBehandlingType =
            "Brev ikke støttet for behandlingstype=${behandlingType}"
    val frontendFeilmelding = "Vi finner ikke vedtaksbrev som matcher med behandlingen og resultatet du har fått. " +
                              "Ta kontakt med Team familie slik at vi kan se nærmere på saken."

    return when (behandlingType) {
        BehandlingType.FØRSTEGANGSBEHANDLING ->
            when (behandlingResultat) {
                INNVILGET, INNVILGET_OG_OPPHØRT, DELVIS_INNVILGET, DELVIS_INNVILGET_OG_OPPHØRT -> Vedtaksbrevtype.FØRSTEGANGSVEDTAK
                AVSLÅTT -> Vedtaksbrevtype.AVSLAG
                else -> throw FunksjonellFeil(melding = feilmeldingBehandlingTypeOgResultat,
                                              frontendFeilmelding = frontendFeilmelding)
            }

        BehandlingType.REVURDERING ->
            when (behandlingResultat) {
                INNVILGET,
                INNVILGET_OG_ENDRET,
                DELVIS_INNVILGET,
                DELVIS_INNVILGET_OG_ENDRET,
                AVSLÅTT_OG_ENDRET,
                ENDRET -> Vedtaksbrevtype.VEDTAK_ENDRING

                OPPHØRT -> Vedtaksbrevtype.OPPHØRT

                INNVILGET_OG_OPPHØRT,
                INNVILGET_ENDRET_OG_OPPHØRT,
                DELVIS_INNVILGET_OG_OPPHØRT,
                DELVIS_INNVILGET_ENDRET_OG_OPPHØRT,
                AVSLÅTT_OG_OPPHØRT,
                AVSLÅTT_ENDRET_OG_OPPHØRT,
                ENDRET_OG_OPPHØRT -> Vedtaksbrevtype.OPPHØR_MED_ENDRING

                else -> throw FunksjonellFeil(melding = feilmeldingBehandlingTypeOgResultat,
                                              frontendFeilmelding = frontendFeilmelding)
            }

        else -> throw FunksjonellFeil(
                melding = feilmelidingBehandlingType,
                frontendFeilmelding = frontendFeilmelding)
    }
}


fun hentSaksbehandlerOgBeslutter(behandling: Behandling, totrinnskontroll: Totrinnskontroll?): Pair<String, String> {
    return when {
        behandling.steg <= StegType.SEND_TIL_BESLUTTER || totrinnskontroll == null -> {
            Pair(SikkerhetContext.hentSaksbehandlerNavn(), "Beslutter")
        }
        totrinnskontroll.erBesluttet() -> {
            Pair(totrinnskontroll.saksbehandler, totrinnskontroll.beslutter!!)
        }
        behandling.steg == StegType.BESLUTTE_VEDTAK -> {
            Pair(totrinnskontroll.saksbehandler,
                 if (totrinnskontroll.saksbehandler == SikkerhetContext.hentSaksbehandlerNavn()) "Beslutter" else SikkerhetContext.hentSaksbehandlerNavn())
        }
        else -> {
            throw Feil("Prøver å hente saksbehandler og beslutters navn for generering av brev i en ukjent tilstand.")
        }
    }
}