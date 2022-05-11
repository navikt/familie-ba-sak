package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.AVSLÅTT
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.AVSLÅTT_ENDRET_OG_OPPHØRT
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.AVSLÅTT_OG_ENDRET
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.AVSLÅTT_OG_OPPHØRT
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.DELVIS_INNVILGET
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.DELVIS_INNVILGET_ENDRET_OG_OPPHØRT
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.DELVIS_INNVILGET_OG_ENDRET
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.DELVIS_INNVILGET_OG_OPPHØRT
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.ENDRET_OG_OPPHØRT
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.ENDRET_UTBETALING
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.ENDRET_UTEN_UTBETALING
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.FORTSATT_INNVILGET
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.FORTSATT_OPPHØRT
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.IKKE_VURDERT
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.INNVILGET
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.INNVILGET_ENDRET_OG_OPPHØRT
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.INNVILGET_OG_ENDRET
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.INNVILGET_OG_OPPHØRT
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.OPPHØRT
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertVedtaksperiode
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.hjemlerTilhørendeFritekst
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.tilSanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Opphørsperiode
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext

fun hentBrevtype(behandling: Behandling): Brevmal =
    when (behandling.opprettetÅrsak) {
        BehandlingÅrsak.DØDSFALL_BRUKER -> Brevmal.VEDTAK_OPPHØR_DØDSFALL
        BehandlingÅrsak.KORREKSJON_VEDTAKSBREV -> Brevmal.VEDTAK_KORREKSJON_VEDTAKSBREV
        else -> hentVedtaksbrevmal(behandling)
    }

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
        BehandlingÅrsak.OMREGNING_6ÅR,
        BehandlingÅrsak.OMREGNING_18ÅR,
        BehandlingÅrsak.SMÅBARNSTILLEGG,
        BehandlingÅrsak.OMREGNING_SMÅBARNSTILLEGG -> Brevmal.AUTOVEDTAK_BARN_6_OG_18_ÅR_OG_SMÅBARNSTILLEGG
        else -> throw Feil("Det er ikke laget funksjonalitet for automatisk behandling for $behandlingÅrsak")
    }

fun hentManuellVedtaksbrevtype(
    behandlingType: BehandlingType,
    behandlingsresultat: Behandlingsresultat
): Brevmal {
    val feilmeldingBehandlingTypeOgResultat =
        "Brev ikke støttet for behandlingstype=$behandlingType og behandlingsresultat=$behandlingsresultat"
    val feilmelidingBehandlingType =
        "Brev ikke støttet for behandlingstype=$behandlingType"
    val frontendFeilmelding = "Vi finner ikke vedtaksbrev som matcher med behandlingen og resultatet du har fått. " +
        "Ta kontakt med Team familie slik at vi kan se nærmere på saken."

    return when (behandlingType) {
        BehandlingType.FØRSTEGANGSBEHANDLING ->
            when (behandlingsresultat) {
                INNVILGET,
                INNVILGET_OG_OPPHØRT,
                DELVIS_INNVILGET,
                DELVIS_INNVILGET_OG_OPPHØRT -> Brevmal.VEDTAK_FØRSTEGANGSVEDTAK

                AVSLÅTT -> Brevmal.VEDTAK_AVSLAG

                else -> throw FunksjonellFeil(
                    melding = feilmeldingBehandlingTypeOgResultat,
                    frontendFeilmelding = frontendFeilmelding
                )
            }

        BehandlingType.REVURDERING ->
            when (behandlingsresultat) {
                INNVILGET,
                INNVILGET_OG_ENDRET,
                DELVIS_INNVILGET,
                DELVIS_INNVILGET_OG_ENDRET,
                AVSLÅTT_OG_ENDRET,
                ENDRET_UTBETALING, ENDRET_UTEN_UTBETALING -> Brevmal.VEDTAK_ENDRING

                OPPHØRT,
                FORTSATT_OPPHØRT -> Brevmal.VEDTAK_OPPHØRT

                INNVILGET_OG_OPPHØRT,
                INNVILGET_ENDRET_OG_OPPHØRT,
                DELVIS_INNVILGET_OG_OPPHØRT,
                DELVIS_INNVILGET_ENDRET_OG_OPPHØRT,
                AVSLÅTT_OG_OPPHØRT,
                AVSLÅTT_ENDRET_OG_OPPHØRT,
                ENDRET_OG_OPPHØRT -> Brevmal.VEDTAK_OPPHØR_MED_ENDRING

                FORTSATT_INNVILGET -> Brevmal.VEDTAK_FORTSATT_INNVILGET

                AVSLÅTT -> Brevmal.VEDTAK_AVSLAG

                else -> throw FunksjonellFeil(
                    melding = feilmeldingBehandlingTypeOgResultat,
                    frontendFeilmelding = frontendFeilmelding
                )
            }

        else -> throw FunksjonellFeil(
            melding = feilmelidingBehandlingType,
            frontendFeilmelding = frontendFeilmelding
        )
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
            Pair(
                totrinnskontroll.saksbehandler,
                if (totrinnskontroll.saksbehandler == SikkerhetContext.hentSaksbehandlerNavn()) "Beslutter"
                else SikkerhetContext.hentSaksbehandlerNavn()
            )
        }
        else -> {
            throw Feil("Prøver å hente saksbehandler og beslutters navn for generering av brev i en ukjent tilstand.")
        }
    }
}

fun hentOverstyrtDokumenttittel(behandling: Behandling): String? {
    return if (behandling.type == BehandlingType.REVURDERING) {
        behandling.opprettetÅrsak.hentOverstyrtDokumenttittelForOmregningsbehandling() ?: when {
            listOf(
                INNVILGET,
                DELVIS_INNVILGET,
                INNVILGET_OG_ENDRET,
                INNVILGET_OG_OPPHØRT,
                DELVIS_INNVILGET_OG_OPPHØRT,
                ENDRET_OG_OPPHØRT
            ).contains(behandling.resultat) -> "Vedtak om endret barnetrygd"
            behandling.resultat == FORTSATT_INNVILGET -> "Vedtak om fortsatt barnetrygd"
            else -> null
        }
    } else null
}

fun hentHjemlerIVedtaksperioderFraSanity(
    brevPeriodeGrunnlag: List<MinimertVedtaksperiode>,
    sanityBegrunnelser: List<SanityBegrunnelse>
): List<String> =
    brevPeriodeGrunnlag.flatMap { periode ->
        periode.begrunnelser.mapNotNull {
            it.standardbegrunnelse.tilSanityBegrunnelse(sanityBegrunnelser)?.hjemler
        }.flatten()
    }

fun hjemlerTilHjemmeltekst(hjemler: List<String>): String {
    return when (hjemler.size) {
        0 -> throw FunksjonellFeil("Ingen hjemler var knyttet til begrunnelsene som er valgt.")
        1 -> "§ ${hjemler[0]}"
        else -> "§§ ${Utils.slåSammen(hjemler)}"
    }
}

fun hentHjemmeltekst(
    minimerteVedtaksperioder: List<MinimertVedtaksperiode>,
    sanityBegrunnelser: List<SanityBegrunnelse>,
    opplysningspliktHjemlerSkalMedIBrev: Boolean = false
): String {
    val hjemler =
        hentHjemlerIVedtaksperioderFraSanity(minimerteVedtaksperioder, sanityBegrunnelser).toMutableSet()

    if (opplysningspliktHjemlerSkalMedIBrev) {
        val hjemlerNårOpplysningspliktIkkeOppfylt = listOf("17", "18")
        hjemler.addAll(hjemlerNårOpplysningspliktIkkeOppfylt)
    }

    if (minimerteVedtaksperioder.flatMap { it.fritekster }.isNotEmpty()) {
        hjemler.addAll(hjemlerTilhørendeFritekst.map { it.toString() }.toSet())
    }

    val sorterteHjemler = hjemler.map { it.toInt() }.sorted().map { it.toString() }

    validerBegrunnelserErKnyttetTilHjemler(sorterteHjemler, sanityBegrunnelser)

    return hjemlerTilHjemmeltekst(sorterteHjemler)
}

private fun validerBegrunnelserErKnyttetTilHjemler(
    sorterteHjemler: List<String>,
    sanityBegrunnelser: List<SanityBegrunnelse>
) {
    if (sorterteHjemler.isEmpty()) {
        throw FunksjonellFeil(
            "Ingen hjemler var knyttet til begrunnelsen(e) " +
                Utils.slåSammen(sanityBegrunnelser.map { it.navnISystem }) +
                ". Du må velge minst én begrunnelse som er knyttet til en hjemmel."
        )
    }
}

fun hentVirkningstidspunkt(opphørsperioder: List<Opphørsperiode>, behandlingId: Long) = (
    opphørsperioder
        .maxOfOrNull { it.periodeFom }
        ?.tilMånedÅr()
        ?: throw Feil("Fant ikke opphørdato ved generering av dødsfallbrev på behandling $behandlingId")
    )
