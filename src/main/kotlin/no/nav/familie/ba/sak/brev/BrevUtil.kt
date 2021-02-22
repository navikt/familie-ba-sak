package no.nav.familie.ba.sak.brev

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.brev.domene.maler.Vedtaksbrevtype
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.totrinnskontroll.domene.Totrinnskontroll

fun hentVedtaksbrevtype(behandling: Behandling) =
        if (behandling.skalBehandlesAutomatisk)
            throw Feil("Det er ikke laget funksjonalitet for automatisk behandling med ny brevløsning.")
        else {
            hentManuellVedtaksbrevtype(behandling.type, behandling.resultat)
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
                BehandlingResultat.INNVILGET, BehandlingResultat.INNVILGET_OG_OPPHØRT, BehandlingResultat.DELVIS_INNVILGET -> Vedtaksbrevtype.FØRSTEGANGSVEDTAK
                else -> throw FunksjonellFeil(melding = feilmeldingBehandlingTypeOgResultat,
                                              frontendFeilmelding = frontendFeilmelding)
            }

        BehandlingType.REVURDERING ->
            when (behandlingResultat) {
                BehandlingResultat.INNVILGET, BehandlingResultat.DELVIS_INNVILGET -> Vedtaksbrevtype.VEDTAK_ENDRING
                BehandlingResultat.OPPHØRT -> Vedtaksbrevtype.OPPHØRT
                BehandlingResultat.INNVILGET_OG_OPPHØRT, BehandlingResultat.ENDRET_OG_OPPHØRT -> Vedtaksbrevtype.OPPHØRT_ENDRING
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