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
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.hjemlerTilhørendeFritekst
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext

fun hentBrevtype(behandling: Behandling): Brevmal =
        if (behandling.opprettetÅrsak == BehandlingÅrsak.DØDSFALL_BRUKER) Brevmal.DØDSFALL
        else hentVedtaksbrevmal(behandling)

fun hentVedtaksbrevmal(behandling: Behandling): Brevmal {
    if (behandling.resultat == IKKE_VURDERT) {
        throw Feil("Kan ikke opprette brev. Behandlingen er ikke vurdert.")
    }

    val brevmal = if (behandling.skalBehandlesAutomatisk)

        hentAutomatiskVedtaksbrevtype(behandling.opprettetÅrsak, behandling.fagsak.status)
    else {
        hentManuellVedtaksbrevtype(behandling.type, behandling.resultat)
    }

    return if (brevmal.erVedtaksbrev) brevmal else throw Feil("Brevmal ${brevmal.visningsTekst} er ikke vedtaksbrev")
}

private fun hentAutomatiskVedtaksbrevtype(behandlingÅrsak: BehandlingÅrsak, fagsakStatus: FagsakStatus): Brevmal =

        when (behandlingÅrsak) {
            BehandlingÅrsak.FØDSELSHENDELSE -> {
                if (fagsakStatus == FagsakStatus.LØPENDE) {
                    Brevmal.AUTOVEDTAK_NYFØDT_BARN_FRA_FØR
                } else Brevmal.AUTOVEDTAK_NYFØDT_FØRSTE_BARN
            }
            BehandlingÅrsak.OMREGNING_6ÅR -> Brevmal.AUTOVEDTAK_BARN6_ÅR
            BehandlingÅrsak.OMREGNING_18ÅR -> Brevmal.AUTOVEDTAK_BARN18_ÅR
            else -> throw Feil("Det er ikke laget funksjonalitet for automatisk behandling for ${behandlingÅrsak}")
        }

fun hentManuellVedtaksbrevtype(behandlingType: BehandlingType,
                               behandlingResultat: BehandlingResultat): Brevmal {
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
                DELVIS_INNVILGET_OG_OPPHØRT -> Brevmal.VEDTAK_FØRSTEGANGSVEDTAK

                AVSLÅTT -> Brevmal.VEDTAK_AVSLAG

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
                ENDRET -> Brevmal.VEDTAK_ENDRING

                OPPHØRT -> Brevmal.VEDTAK_OPPHØRT

                INNVILGET_OG_OPPHØRT,
                INNVILGET_ENDRET_OG_OPPHØRT,
                DELVIS_INNVILGET_OG_OPPHØRT,
                DELVIS_INNVILGET_ENDRET_OG_OPPHØRT,
                AVSLÅTT_OG_OPPHØRT,
                AVSLÅTT_ENDRET_OG_OPPHØRT,
                ENDRET_OG_OPPHØRT -> Brevmal.VEDTAK_OPPHØR_MED_ENDRING

                FORTSATT_INNVILGET -> Brevmal.VEDTAK_FORTSATT_INNVILGET

                AVSLÅTT -> Brevmal.VEDTAK_AVSLAG

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

fun List<VedtaksperiodeMedBegrunnelser>.sorter(): List<VedtaksperiodeMedBegrunnelser> {
    val (perioderMedFom, perioderUtenFom) = this.partition { it.fom != null }
    return perioderMedFom.sortedWith(compareBy { it.fom }) + perioderUtenFom
}