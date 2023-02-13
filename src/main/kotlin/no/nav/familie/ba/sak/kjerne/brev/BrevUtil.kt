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
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.hjemlerTilhørendeFritekst
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.tilISanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Opphørsperiode

fun hentBrevmal(behandling: Behandling): Brevmal =
    when (behandling.opprettetÅrsak) {
        BehandlingÅrsak.DØDSFALL_BRUKER -> Brevmal.VEDTAK_OPPHØR_DØDSFALL
        BehandlingÅrsak.KORREKSJON_VEDTAKSBREV -> Brevmal.VEDTAK_KORREKSJON_VEDTAKSBREV
        else -> hentVedtaksbrevmal(behandling)
    }

fun hentVedtaksbrevmal(behandling: Behandling): Brevmal {
    if (behandling.resultat == IKKE_VURDERT) {
        throw Feil("Kan ikke opprette brev. Behandlingen er ikke vurdert.")
    }

    val brevmal = if (behandling.skalBehandlesAutomatisk) {
        hentAutomatiskVedtaksbrevtype(behandling.opprettetÅrsak, behandling.fagsak.status)
    } else {
        hentManuellVedtaksbrevtype(behandling.type, behandling.resultat, behandling.fagsak.institusjon != null)
    }

    return if (brevmal.erVedtaksbrev) brevmal else throw Feil("Brevmal ${brevmal.visningsTekst} er ikke vedtaksbrev")
}

private fun hentAutomatiskVedtaksbrevtype(behandlingÅrsak: BehandlingÅrsak, fagsakStatus: FagsakStatus): Brevmal =

    when (behandlingÅrsak) {
        BehandlingÅrsak.FØDSELSHENDELSE -> {
            if (fagsakStatus == FagsakStatus.LØPENDE) {
                Brevmal.AUTOVEDTAK_NYFØDT_BARN_FRA_FØR
            } else {
                Brevmal.AUTOVEDTAK_NYFØDT_FØRSTE_BARN
            }
        }
        BehandlingÅrsak.OMREGNING_6ÅR,
        BehandlingÅrsak.OMREGNING_18ÅR,
        BehandlingÅrsak.SMÅBARNSTILLEGG,
        BehandlingÅrsak.OMREGNING_SMÅBARNSTILLEGG -> Brevmal.AUTOVEDTAK_BARN_6_OG_18_ÅR_OG_SMÅBARNSTILLEGG
        else -> throw Feil("Det er ikke laget funksjonalitet for automatisk behandling for $behandlingÅrsak")
    }

fun hentManuellVedtaksbrevtype(
    behandlingType: BehandlingType,
    behandlingsresultat: Behandlingsresultat,
    erInstitusjon: Boolean = false
): Brevmal {
    val feilmeldingBehandlingTypeOgResultat =
        "Brev ikke støttet for behandlingstype=$behandlingType og behandlingsresultat=$behandlingsresultat"
    val feilmelidingBehandlingType =
        "Brev ikke støttet for behandlingstype=$behandlingType"
    val frontendFeilmelding = "Vi finner ikke vedtaksbrev som matcher med behandlingen og resultatet du har fått. " +
        "Ta kontakt med Team familie slik at vi kan se nærmere på saken."

    return when (behandlingType) {
        BehandlingType.FØRSTEGANGSBEHANDLING ->
            if (erInstitusjon) {
                when (behandlingsresultat) {
                    INNVILGET,
                    INNVILGET_OG_OPPHØRT,
                    DELVIS_INNVILGET,
                    DELVIS_INNVILGET_OG_OPPHØRT -> Brevmal.VEDTAK_FØRSTEGANGSVEDTAK_INSTITUSJON

                    AVSLÅTT -> Brevmal.VEDTAK_AVSLAG_INSTITUSJON

                    else -> throw FunksjonellFeil(
                        melding = feilmeldingBehandlingTypeOgResultat,
                        frontendFeilmelding = frontendFeilmelding
                    )
                }
            } else {
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
            }

        BehandlingType.REVURDERING ->
            if (erInstitusjon) {
                when (behandlingsresultat) {
                    INNVILGET,
                    INNVILGET_OG_ENDRET,
                    DELVIS_INNVILGET,
                    DELVIS_INNVILGET_OG_ENDRET,
                    AVSLÅTT_OG_ENDRET,
                    ENDRET_UTBETALING, ENDRET_UTEN_UTBETALING -> Brevmal.VEDTAK_ENDRING_INSTITUSJON

                    OPPHØRT,
                    FORTSATT_OPPHØRT -> Brevmal.VEDTAK_OPPHØRT_INSTITUSJON

                    INNVILGET_OG_OPPHØRT,
                    INNVILGET_ENDRET_OG_OPPHØRT,
                    DELVIS_INNVILGET_OG_OPPHØRT,
                    DELVIS_INNVILGET_ENDRET_OG_OPPHØRT,
                    AVSLÅTT_OG_OPPHØRT,
                    AVSLÅTT_ENDRET_OG_OPPHØRT,
                    ENDRET_OG_OPPHØRT -> Brevmal.VEDTAK_OPPHØR_MED_ENDRING_INSTITUSJON

                    FORTSATT_INNVILGET -> Brevmal.VEDTAK_FORTSATT_INNVILGET_INSTITUSJON

                    AVSLÅTT -> Brevmal.VEDTAK_AVSLAG_INSTITUSJON

                    else -> throw FunksjonellFeil(
                        melding = feilmeldingBehandlingTypeOgResultat,
                        frontendFeilmelding = frontendFeilmelding
                    )
                }
            } else {
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
            }

        else -> throw FunksjonellFeil(
            melding = feilmelidingBehandlingType,
            frontendFeilmelding = frontendFeilmelding
        )
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
    } else {
        null
    }
}

fun hjemlerTilHjemmeltekst(hjemler: List<String>, lovForHjemmel: String): String {
    return when (hjemler.size) {
        0 -> throw Feil("Kan ikke lage hjemmeltekst for $lovForHjemmel når ingen begrunnelser har hjemler fra $lovForHjemmel knyttet til seg.")
        1 -> "§ ${hjemler[0]}"
        else -> "§§ ${Utils.slåSammen(hjemler)}"
    }
}

fun hentHjemmeltekst(
    minimerteVedtaksperioder: List<MinimertVedtaksperiode>,
    sanityBegrunnelser: List<SanityBegrunnelse>,
    opplysningspliktHjemlerSkalMedIBrev: Boolean = false,
    målform: Målform,
    vedtakKorrigertHjemmelSkalMedIBrev: Boolean = false
): String {
    val sanityStandardbegrunnelser = minimerteVedtaksperioder.flatMap { vedtaksperiode ->
        vedtaksperiode.begrunnelser.mapNotNull { begrunnelse ->
            begrunnelse.standardbegrunnelse.tilISanityBegrunnelse(sanityBegrunnelser)
        }
    }

    val sanityEøsBegrunnelser = minimerteVedtaksperioder.flatMap { vedtaksperiode ->
        vedtaksperiode.eøsBegrunnelser.map { begrunnelse ->
            begrunnelse.sanityEØSBegrunnelse
        }
    }

    val ordinæreHjemler =
        hentOrdinæreHjemler(
            hjemler = (sanityStandardbegrunnelser.flatMap { it.hjemler } + sanityEøsBegrunnelser.flatMap { it.hjemler })
                .toMutableSet(),
            opplysningspliktHjemlerSkalMedIBrev = opplysningspliktHjemlerSkalMedIBrev,
            finnesVedtaksperiodeMedFritekst = minimerteVedtaksperioder.flatMap { it.fritekster }.isNotEmpty()
        )

    val forvaltningsloverHjemler = hentForvaltningsloverHjemler(vedtakKorrigertHjemmelSkalMedIBrev)

    val alleHjemlerForBegrunnelser = hentAlleTyperHjemler(
        hjemlerSeparasjonsavtaleStorbritannia = sanityEøsBegrunnelser.flatMap { it.hjemlerSeperasjonsavtalenStorbritannina }
            .distinct(),
        ordinæreHjemler = ordinæreHjemler.distinct(),
        hjemlerFraFolketrygdloven = (sanityStandardbegrunnelser.flatMap { it.hjemlerFolketrygdloven } + sanityEøsBegrunnelser.flatMap { it.hjemlerFolketrygdloven })
            .distinct(),
        hjemlerEØSForordningen883 = sanityEøsBegrunnelser.flatMap { it.hjemlerEØSForordningen883 }.distinct(),
        hjemlerEØSForordningen987 = sanityEøsBegrunnelser.flatMap { it.hjemlerEØSForordningen987 }.distinct(),
        målform = målform,
        hjemlerFraForvaltningsloven = forvaltningsloverHjemler
    )

    return slåSammenHjemlerAvUlikeTyper(alleHjemlerForBegrunnelser)
}

private fun slåSammenHjemlerAvUlikeTyper(hjemler: List<String>) = when (hjemler.size) {
    0 -> throw FunksjonellFeil("Ingen hjemler var knyttet til begrunnelsen(e) som er valgt. Du må velge minst én begrunnelse som er knyttet til en hjemmel.")
    1 -> hjemler.single()
    else -> slåSammenListeMedHjemler(hjemler)
}

private fun slåSammenListeMedHjemler(hjemler: List<String>): String {
    return hjemler.reduceIndexed { index, acc, s ->
        when (index) {
            0 -> acc + s
            hjemler.size - 1 -> "$acc og $s"
            else -> "$acc, $s"
        }
    }
}

private fun hentAlleTyperHjemler(
    hjemlerSeparasjonsavtaleStorbritannia: List<String>,
    ordinæreHjemler: List<String>,
    hjemlerFraFolketrygdloven: List<String>,
    hjemlerEØSForordningen883: List<String>,
    hjemlerEØSForordningen987: List<String>,
    målform: Målform,
    hjemlerFraForvaltningsloven: List<String>
): List<String> {
    val alleHjemlerForBegrunnelser = mutableListOf<String>()

    // Rekkefølgen her er viktig
    if (hjemlerSeparasjonsavtaleStorbritannia.isNotEmpty()) {
        alleHjemlerForBegrunnelser.add(
            "${
            when (målform) {
                Målform.NB -> "Separasjonsavtalen mellom Storbritannia og Norge artikkel"
                Målform.NN -> "Separasjonsavtalen mellom Storbritannia og Noreg artikkel"
            }
            } ${
            Utils.slåSammen(
                hjemlerSeparasjonsavtaleStorbritannia
            )
            }"
        )
    }
    if (ordinæreHjemler.isNotEmpty()) {
        alleHjemlerForBegrunnelser.add(
            "${
            when (målform) {
                Målform.NB -> "barnetrygdloven"
                Målform.NN -> "barnetrygdlova"
            }
            } ${
            hjemlerTilHjemmeltekst(
                hjemler = ordinæreHjemler,
                lovForHjemmel = "barnetrygdloven"
            )
            }"
        )
    }
    if (hjemlerFraFolketrygdloven.isNotEmpty()) {
        alleHjemlerForBegrunnelser.add(
            "${
            when (målform) {
                Målform.NB -> "folketrygdloven"
                Målform.NN -> "folketrygdlova"
            }
            } ${
            hjemlerTilHjemmeltekst(
                hjemler = hjemlerFraFolketrygdloven,
                lovForHjemmel = "folketrygdloven"
            )
            }"
        )
    }
    if (hjemlerEØSForordningen883.isNotEmpty()) {
        alleHjemlerForBegrunnelser.add("EØS-forordning 883/2004 artikkel ${Utils.slåSammen(hjemlerEØSForordningen883)}")
    }
    if (hjemlerEØSForordningen987.isNotEmpty()) {
        alleHjemlerForBegrunnelser.add("EØS-forordning 987/2009 artikkel ${Utils.slåSammen(hjemlerEØSForordningen987)}")
    }
    if (hjemlerFraForvaltningsloven.isNotEmpty()) {
        alleHjemlerForBegrunnelser.add(
            "${
            when (målform) {
                Målform.NB -> "forvaltningsloven"
                Målform.NN -> "forvaltningslova"
            }
            } ${
            hjemlerTilHjemmeltekst(hjemler = hjemlerFraForvaltningsloven, lovForHjemmel = "forvaltningsloven")
            }"
        )
    }
    return alleHjemlerForBegrunnelser
}

private fun hentOrdinæreHjemler(
    hjemler: MutableSet<String>,
    opplysningspliktHjemlerSkalMedIBrev: Boolean,
    finnesVedtaksperiodeMedFritekst: Boolean
): List<String> {
    if (opplysningspliktHjemlerSkalMedIBrev) {
        val hjemlerNårOpplysningspliktIkkeOppfylt = listOf("17", "18")
        hjemler.addAll(hjemlerNårOpplysningspliktIkkeOppfylt)
    }

    if (finnesVedtaksperiodeMedFritekst) {
        hjemler.addAll(hjemlerTilhørendeFritekst.map { it.toString() }.toSet())
    }

    val sorterteHjemler = hjemler.map { it.toInt() }.sorted().map { it.toString() }
    return sorterteHjemler
}

fun hentVirkningstidspunkt(opphørsperioder: List<Opphørsperiode>, behandlingId: Long) = (
    opphørsperioder
        .maxOfOrNull { it.periodeFom }
        ?.tilMånedÅr()
        ?: throw Feil("Fant ikke opphørdato ved generering av dødsfallbrev på behandling $behandlingId")
    )

fun hentForvaltningsloverHjemler(vedtakKorrigertHjemmelSkalMedIBrev: Boolean): List<String> {
    return if (vedtakKorrigertHjemmelSkalMedIBrev) listOf("35") else emptyList()
}
