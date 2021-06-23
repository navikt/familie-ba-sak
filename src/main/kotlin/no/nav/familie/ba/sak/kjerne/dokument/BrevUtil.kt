package no.nav.familie.ba.sak.kjerne.dokument

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat.AVSLÅTT
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat.AVSLÅTT_ENDRET_OG_OPPHØRT
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat.AVSLÅTT_OG_ENDRET
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat.AVSLÅTT_OG_OPPHØRT
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat.DELVIS_INNVILGET
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat.DELVIS_INNVILGET_ENDRET_OG_OPPHØRT
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat.DELVIS_INNVILGET_OG_ENDRET
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat.DELVIS_INNVILGET_OG_OPPHØRT
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat.ENDRET
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat.ENDRET_OG_OPPHØRT
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat.FORTSATT_INNVILGET
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat.IKKE_VURDERT
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat.INNVILGET
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat.INNVILGET_ENDRET_OG_OPPHØRT
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat.INNVILGET_OG_ENDRET
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat.INNVILGET_OG_OPPHØRT
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat.OPPHØRT
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.BrevType
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.EnkelBrevtype
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.Vedtaksbrevtype
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.hjemlerTilhørendeFritekst
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext

fun hentBrevtype(behandling: Behandling): BrevType =
        if (behandling.opprettetÅrsak == BehandlingÅrsak.DØDSFALL_BRUKER) EnkelBrevtype.DØDSFALL
        else hentVedtaksbrevtype(behandling)

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
            else -> throw Feil("Det er ikke laget funksjonalitet for automatisk behandling for $behandlingÅrsak")
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
                INNVILGET,
                INNVILGET_OG_OPPHØRT,
                DELVIS_INNVILGET,
                DELVIS_INNVILGET_OG_OPPHØRT -> Vedtaksbrevtype.FØRSTEGANGSVEDTAK

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

                FORTSATT_INNVILGET -> Vedtaksbrevtype.FORTSATT_INNVILGET

                AVSLÅTT -> Vedtaksbrevtype.AVSLAG

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
                 if (totrinnskontroll.saksbehandler == SikkerhetContext.hentSaksbehandlerNavn()) "Beslutter"
                 else SikkerhetContext.hentSaksbehandlerNavn())
        }
        else -> {
            throw Feil("Prøver å hente saksbehandler og beslutters navn for generering av brev i en ukjent tilstand.")
        }
    }
}

fun hentOverstyrtDokumenttittel(behandling: Behandling): String? {
    return if (behandling.type == BehandlingType.REVURDERING) {
        when {
            behandling.opprettetÅrsak == BehandlingÅrsak.OMREGNING_6ÅR -> "Vedtak om endret barnetrygd - barn 6 år"
            behandling.opprettetÅrsak == BehandlingÅrsak.OMREGNING_18ÅR -> "Vedtak om endret barnetrygd - barn 18 år"
            listOf(INNVILGET,
                   DELVIS_INNVILGET,
                   INNVILGET_OG_ENDRET,
                   INNVILGET_OG_OPPHØRT,
                   DELVIS_INNVILGET_OG_OPPHØRT,
                   ENDRET_OG_OPPHØRT).contains(behandling.resultat) -> "Vedtak om endret barnetrygd"
            behandling.resultat == FORTSATT_INNVILGET -> "Vedtak om fortsatt barnetrygd"
            else -> null
        }
    } else null
}

fun hentHjemlerIVedtaksperioder(vedtaksperioderMedBegrunnelser: List<VedtaksperiodeMedBegrunnelser>): Set<Int> =
        vedtaksperioderMedBegrunnelser.flatMap { periode ->
            periode.begrunnelser.flatMap {
                it.vedtakBegrunnelseSpesifikasjon.hentHjemler()
            }
        }.toSortedSet()

fun hjemlerTilHjemmeltekst(hjemler: List<String>): String {
    return when (hjemler.size) {
        0 -> throw Feil("Ingen hjemler sendt med")
        1 -> "§ ${hjemler[0]}"
        else -> "§§ ${Utils.slåSammen(hjemler)}"
    }
}

fun hentHjemmeltekst(vedtak: Vedtak, vedtaksperioderMedBegrunnelser: List<VedtaksperiodeMedBegrunnelser>): String {
    val hjemler = (vedtak.hentHjemler() + hentHjemlerIVedtaksperioder(vedtaksperioderMedBegrunnelser))
            .toMutableSet()
    if (vedtaksperioderMedBegrunnelser.flatMap { it.fritekster }.isNotEmpty()) {
        hjemler.addAll(hjemlerTilhørendeFritekst)
    }
    return hjemlerTilHjemmeltekst(hjemler.sorted().map { it.toString() })
}

fun verifiserVedtakHarBegrunnelseEllerFritekst(vedtaksperioderMedBegrunnelser: List<VedtaksperiodeMedBegrunnelser>) {
    val antallBegrunnelser = vedtaksperioderMedBegrunnelser.flatMap { it.begrunnelser }.size
    val antallFritekster = vedtaksperioderMedBegrunnelser.flatMap { it.fritekster }.size
    if (antallBegrunnelser == 0 && antallFritekster == 0) {
        throw FunksjonellFeil("Vedtaket mangler begrunnelser. Du må legge til begrunnelser for å generere vedtaksbrevet.")
    }
}