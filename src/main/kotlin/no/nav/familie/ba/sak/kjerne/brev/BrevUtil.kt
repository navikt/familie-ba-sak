package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.erSenereEnnInneværendeMåned
import no.nav.familie.ba.sak.common.tilDagMånedÅr
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
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.MinimertUregistrertBarn
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevGrunnlag
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevPeriodeGrunnlag
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevPeriodeGrunnlagMedPersoner
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.EndretUtbetalingBrevPeriodeType
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.AvslagBrevPeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.AvslagUtenPeriodeBrevPeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.EndretUtbetalingBarnetrygdType
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.EndretUtbetalingBrevPeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.FortsattInnvilgetBrevPeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.InnvilgelseBrevPeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.OpphørBrevPeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertPersonResultat
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.hjemlerTilhørendeFritekst
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.tilSanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Begrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.MinimertPerson
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilMinimertPerson
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import java.math.BigDecimal

fun hentBrevtype(behandling: Behandling): Brevmal =
    when (behandling.opprettetÅrsak) {
        BehandlingÅrsak.DØDSFALL_BRUKER -> Brevmal.DØDSFALL
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
        BehandlingÅrsak.SMÅBARNSTILLEGG -> Brevmal.AUTOVEDTAK_BARN_6_OG_18_ÅR_OG_SMÅBARNSTILLEGG
        else -> throw Feil("Det er ikke laget funksjonalitet for automatisk behandling for $behandlingÅrsak")
    }

fun hentManuellVedtaksbrevtype(
    behandlingType: BehandlingType,
    behandlingResultat: BehandlingResultat
): Brevmal {
    val feilmeldingBehandlingTypeOgResultat =
        "Brev ikke støttet for behandlingstype=$behandlingType og behandlingsresultat=$behandlingResultat"
    val feilmelidingBehandlingType =
        "Brev ikke støttet for behandlingstype=$behandlingType"
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

                else -> throw FunksjonellFeil(
                    melding = feilmeldingBehandlingTypeOgResultat,
                    frontendFeilmelding = frontendFeilmelding
                )
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
        when {
            behandling.opprettetÅrsak == BehandlingÅrsak.OMREGNING_6ÅR -> "Vedtak om endret barnetrygd - barn 6 år"
            behandling.opprettetÅrsak == BehandlingÅrsak.OMREGNING_18ÅR -> "Vedtak om endret barnetrygd - barn 18 år"
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
    brevPeriodeGrunnlag: List<BrevPeriodeGrunnlag>,
    sanityBegrunnelser: List<SanityBegrunnelse>
): List<String> =
    brevPeriodeGrunnlag.flatMap { periode ->
        periode.begrunnelser.mapNotNull {
            it.vedtakBegrunnelseSpesifikasjon.tilSanityBegrunnelse(sanityBegrunnelser)?.hjemler
        }.flatten()
    }

fun hjemlerTilHjemmeltekst(hjemler: List<String>): String {
    return when (hjemler.size) {
        0 -> throw Feil("Ingen hjemler sendt med")
        1 -> "§ ${hjemler[0]}"
        else -> "§§ ${Utils.slåSammen(hjemler)}"
    }
}

fun hentHjemmeltekst(
    brevPeriodeGrunnlag: List<BrevPeriodeGrunnlag>,
    sanityBegrunnelser: List<SanityBegrunnelse>
): String {
    val hjemler =
        hentHjemlerIVedtaksperioderFraSanity(brevPeriodeGrunnlag, sanityBegrunnelser).toMutableSet()

    if (brevPeriodeGrunnlag.flatMap { it.fritekster }.isNotEmpty()) {
        hjemler.addAll(hjemlerTilhørendeFritekst.map { it.toString() }.toSet())
    }

    val sorterteHjemler = hjemler.map { it.toInt() }.sorted().map { it.toString() }

    return hjemlerTilHjemmeltekst(sorterteHjemler)
}

enum class UtvidetScenarioForEndringsperiode {
    IKKE_UTVIDET_YTELSE,
    UTVIDET_YTELSE_ENDRET,
    UTVIDET_YTELSE_IKKE_ENDRET
}

fun BrevPeriodeGrunnlag.tilBrevPeriode(
    brevGrunnlag: BrevGrunnlag,
    utvidetScenarioForEndringsperiode: UtvidetScenarioForEndringsperiode = UtvidetScenarioForEndringsperiode.IKKE_UTVIDET_YTELSE,
    uregistrerteBarn: List<MinimertUregistrertBarn> = emptyList(),
    erFørsteVedtaksperiodePåFagsak: Boolean,
    målformSøker: Målform
): BrevPeriode? {
    val brevPeriodeGrunnlagMedPersoner =
        this.tilBrevPeriodeGrunnlagMedPersoner(brevGrunnlag, erFørsteVedtaksperiodePåFagsak)

    val personerIPersongrunnlag = brevGrunnlag.personerPåBehandling

    val begrunnelserOgFritekster = brevPeriodeGrunnlagMedPersoner.byggBegrunnelserOgFritekster(
        uregistrerteBarn = uregistrerteBarn,
        brevGrunnlag = brevGrunnlag,
        målformSøker = målformSøker
    )

    if (begrunnelserOgFritekster.isEmpty()) return null

    val tomDato =
        if (brevPeriodeGrunnlagMedPersoner.tom?.erSenereEnnInneværendeMåned() == false)
            brevPeriodeGrunnlagMedPersoner.tom.tilDagMånedÅr()
        else null

    return when (brevPeriodeGrunnlagMedPersoner.type) {
        Vedtaksperiodetype.FORTSATT_INNVILGET -> brevPeriodeGrunnlagMedPersoner.hentFortsattInnvilgetBrevPeriode(
            målformSøker,
            begrunnelserOgFritekster
        )

        Vedtaksperiodetype.UTBETALING -> brevPeriodeGrunnlagMedPersoner.hentInnvilgelseBrevPeriode(
            tomDato,
            begrunnelserOgFritekster,
            personerIPersongrunnlag
        )

        Vedtaksperiodetype.ENDRET_UTBETALING -> brevPeriodeGrunnlagMedPersoner.hentEndretUtbetalingBrevPeriode(
            tomDato,
            begrunnelserOgFritekster,
            utvidetScenarioForEndringsperiode,
            målformSøker
        )

        Vedtaksperiodetype.AVSLAG -> brevPeriodeGrunnlagMedPersoner.hentAvslagBrevPeriode(
            tomDato,
            begrunnelserOgFritekster
        )

        Vedtaksperiodetype.OPPHØR -> OpphørBrevPeriode(
            fom = brevPeriodeGrunnlagMedPersoner.fom!!.tilDagMånedÅr(),
            tom = tomDato,
            begrunnelser = begrunnelserOgFritekster
        )
    }
}

private fun BrevPeriodeGrunnlagMedPersoner.hentAvslagBrevPeriode(
    tomDato: String?,
    begrunnelserOgFritekster: List<Begrunnelse>,
) =
    if (this.fom != null)
        AvslagBrevPeriode(
            fom = fom.tilDagMånedÅr(),
            tom = tomDato,
            begrunnelser = begrunnelserOgFritekster
        )
    else AvslagUtenPeriodeBrevPeriode(begrunnelser = begrunnelserOgFritekster)

fun BrevPeriodeGrunnlagMedPersoner.hentEndretUtbetalingBrevPeriode(
    tomDato: String?,
    begrunnelserOgFritekster: List<Begrunnelse>,
    utvidetScenario: UtvidetScenarioForEndringsperiode = UtvidetScenarioForEndringsperiode.IKKE_UTVIDET_YTELSE,
    målform: Målform = Målform.NB,
): EndretUtbetalingBrevPeriode {
    val ingenUtbetalingForEndringsperioden =
        minimerteUtbetalingsperiodeDetaljer.all { it.prosent == BigDecimal.ZERO }

    return EndretUtbetalingBrevPeriode(
        fom = this.fom!!.tilDagMånedÅr(),
        tom = tomDato,
        barnasFodselsdager = this.minimerteUtbetalingsperiodeDetaljer.map { it.person }.tilBarnasFødselsdatoer(),
        begrunnelser = begrunnelserOgFritekster,
        belop = Utils.formaterBeløp(this.minimerteUtbetalingsperiodeDetaljer.totaltUtbetalt()),
        type = when {
            ingenUtbetalingForEndringsperioden && utvidetScenario == UtvidetScenarioForEndringsperiode.UTVIDET_YTELSE_IKKE_ENDRET ->
                EndretUtbetalingBrevPeriodeType.ENDRET_UTBETALINGSPERIODE_DELVIS_UTBETALING
            ingenUtbetalingForEndringsperioden ->
                EndretUtbetalingBrevPeriodeType.ENDRET_UTBETALINGSPERIODE_INGEN_UTBETALING
            else ->
                EndretUtbetalingBrevPeriodeType.ENDRET_UTBETALINGSPERIODE
        },
        typeBarnetrygd = if (utvidetScenario == UtvidetScenarioForEndringsperiode.IKKE_UTVIDET_YTELSE)
            EndretUtbetalingBarnetrygdType.DELT
        else when (målform) {
            Målform.NB -> EndretUtbetalingBarnetrygdType.DELT_UTVIDET_NB
            Målform.NN -> EndretUtbetalingBarnetrygdType.DELT_UTVIDET_NN
        }
    )
}

private fun BrevPeriodeGrunnlagMedPersoner.hentInnvilgelseBrevPeriode(
    tomDato: String?,
    begrunnelserOgFritekster: List<Begrunnelse>,
    personerIPersongrunnlag: List<MinimertPerson>,
): InnvilgelseBrevPeriode {
    val barnIPeriode = this.finnBarnIInnvilgelsePeriode(personerIPersongrunnlag)

    return InnvilgelseBrevPeriode(
        fom = this.fom!!.tilDagMånedÅr(),
        tom = tomDato,
        belop = Utils.formaterBeløp(this.minimerteUtbetalingsperiodeDetaljer.totaltUtbetalt()),
        antallBarn = barnIPeriode.size.toString(),
        barnasFodselsdager = barnIPeriode.tilBarnasFødselsdatoer(),
        begrunnelser = begrunnelserOgFritekster
    )
}

fun BrevPeriodeGrunnlagMedPersoner.finnBarnIInnvilgelsePeriode(
    personerIPersongrunnlag: List<MinimertPerson>,
): List<MinimertPerson> {
    val identerIBegrunnelsene = this.begrunnelser
        .filter { it.vedtakBegrunnelseType == VedtakBegrunnelseType.INNVILGET }
        .flatMap { it.personIdenter }
    val identerMedUtbetaling = this.minimerteUtbetalingsperiodeDetaljer.map { it.person.personIdent }

    val barnIPeriode = (identerIBegrunnelsene + identerMedUtbetaling)
        .toSet()
        .mapNotNull { personIdent ->
            personerIPersongrunnlag.find { it.personIdent == personIdent }
        }
        .filter { it.type == PersonType.BARN }

    return barnIPeriode
}

private fun BrevPeriodeGrunnlagMedPersoner.hentFortsattInnvilgetBrevPeriode(
    målform: Målform,
    begrunnelserOgFritekster: List<Begrunnelse>
): FortsattInnvilgetBrevPeriode {
    val erAutobrev = this.begrunnelser.any { vedtaksbegrunnelse ->
        vedtaksbegrunnelse.vedtakBegrunnelseSpesifikasjon == VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR_AUTOVEDTAK ||
            vedtaksbegrunnelse.vedtakBegrunnelseSpesifikasjon == VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_18_ÅR_AUTOVEDTAK
    }
    val fom = if (erAutobrev && this.fom != null) {
        val fra = if (målform == Målform.NB) "Fra" else "Frå"
        "$fra ${this.fom.tilDagMånedÅr()} får du:"
    } else null
    return FortsattInnvilgetBrevPeriode(
        fom = fom ?: "Du får:",
        belop = Utils.formaterBeløp(this.minimerteUtbetalingsperiodeDetaljer.totaltUtbetalt()),
        antallBarn = this.minimerteUtbetalingsperiodeDetaljer.antallBarn().toString(),
        barnasFodselsdager = this.minimerteUtbetalingsperiodeDetaljer.map { it.person }.tilBarnasFødselsdatoer(),
        begrunnelser = begrunnelserOgFritekster
    )
}

fun hentBrevGrunnlag(
    persongrunnlag: PersonopplysningGrunnlag,
    vilkårsvurdering: Vilkårsvurdering,
    endredeUtbetalingAndeler: List<EndretUtbetalingAndel>
): BrevGrunnlag {

    return BrevGrunnlag(
        personerPåBehandling = persongrunnlag.personer.map { it.tilMinimertPerson() },
        minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
        minimerteEndredeUtbetalingAndeler = endredeUtbetalingAndeler.map { it.tilMinimertEndretUtbetalingAndel() },
    )
}