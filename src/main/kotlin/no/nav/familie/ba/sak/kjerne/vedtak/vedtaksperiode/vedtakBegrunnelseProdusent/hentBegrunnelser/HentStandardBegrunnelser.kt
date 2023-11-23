package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.hentBegrunnelser

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.brev.domene.EndretUtbetalingsperiodeDeltBostedTriggere
import no.nav.familie.ba.sak.kjerne.brev.domene.ISanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityPeriodeResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.Tema
import no.nav.familie.ba.sak.kjerne.brev.domene.UtvidetBarnetrygdTrigger
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.BegrunnelseGrunnlagForPeriodeMedOpphør
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.BegrunnelseGrunnlagForPeriodeMedReduksjonPåTversAvBehandlinger
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.IBegrunnelseGrunnlagForPeriode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.begrunnelseGjelderOpphørFraForrigeBehandling
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.begrunnelseGjelderReduksjonFraForrigeBehandling
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.erGjeldendeForBrevPeriodeType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.erGjeldendeForFagsakType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.erGjeldendeForRolle
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.erGjeldendeForUtgjørendeVilkår
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.erLikVilkårOgUtdypendeVilkårIPeriode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.filtrerPåHendelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.filtrerPåSkalVisesSelvOmIkkeEndring
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.hentEndretUtbetalingDennePerioden
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.hentResultaterForForrigePeriode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.matcherErAutomatisk
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.IEndretUtbetalingAndelForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.VilkårResultatForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import java.math.BigDecimal

internal fun hentStandardBegrunnelser(
    begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode,
    sanityBegrunnelser: Map<Standardbegrunnelse, SanityBegrunnelse>,
    person: Person,
    vedtaksperiode: VedtaksperiodeMedBegrunnelser,
    behandling: Behandling,
    relevantePeriodeResultater: List<SanityPeriodeResultat>,
    erUtbetalingEllerDeltBostedIPeriode: Boolean,
    utvidetVilkårPåSøkerIPeriode: VilkårResultatForVedtaksperiode?,
    utvidetVilkårPåSøkerIForrigePeriode: VilkårResultatForVedtaksperiode?,
): Map<IVedtakBegrunnelse, ISanityBegrunnelse> {
    val endretUtbetalingDennePerioden = hentEndretUtbetalingDennePerioden(begrunnelseGrunnlag)

    val filtrertPåRolleOgFagsaktype =
        sanityBegrunnelser
            .filterValues { begrunnelse -> begrunnelse.erGjeldendeForRolle(person, behandling.fagsak.type) }
            .filterValues { it.erGjeldendeForFagsakType(behandling.fagsak.type) }

    val relevanteBegrunnelser =
        filtrertPåRolleOgFagsaktype
            .filterValues { it.periodeResultat in relevantePeriodeResultater }
            .filterValues { it.matcherErAutomatisk(behandling.skalBehandlesAutomatisk) }
            .filterValues { it.erGjeldendeForBrevPeriodeType(vedtaksperiode, erUtbetalingEllerDeltBostedIPeriode) }
            .filterValues { !it.begrunnelseGjelderReduksjonFraForrigeBehandling() && !it.begrunnelseGjelderOpphørFraForrigeBehandling() }

    val filtrertPåVilkårOgEndretUtbetaling =
        relevanteBegrunnelser.filterValues {
            val begrunnelseErGjeldendeForUtgjørendeVilkår = it.vilkår.isNotEmpty()
            val begrunnelseErGjeldendeForEndretUtbetaling = it.endringsaarsaker.isNotEmpty()

            when {
                begrunnelseErGjeldendeForUtgjørendeVilkår && begrunnelseErGjeldendeForEndretUtbetaling ->
                    filtrerPåVilkår(
                        it,
                        begrunnelseGrunnlag,
                        utvidetVilkårPåSøkerIPeriode,
                        utvidetVilkårPåSøkerIForrigePeriode,
                    ) && filtrerPåEndretUtbetaling(it, endretUtbetalingDennePerioden)

                begrunnelseErGjeldendeForUtgjørendeVilkår ->
                    filtrerPåVilkår(
                        it,
                        begrunnelseGrunnlag,
                        utvidetVilkårPåSøkerIPeriode,
                        utvidetVilkårPåSøkerIForrigePeriode,
                    )

                else -> it.erEndretUtbetaling(endretUtbetalingDennePerioden)
            }
        }

    val filtrertPåReduksjonFraForrigeBehandling =
        filtrertPåRolleOgFagsaktype.filterValues {
            it.erGjeldendeForReduksjonFraForrigeBehandling(begrunnelseGrunnlag)
        }

    val filtrertPåOpphørFraForrigeBehandling =
        filtrertPåRolleOgFagsaktype.filterValues {
            it.erGjeldendeForOpphørFraForrigeBehandling(begrunnelseGrunnlag)
        }

    val filtrertPåSmåbarnstillegg =
        relevanteBegrunnelser.filterValues { begrunnelse ->
            begrunnelse.erGjeldendeForSmåbarnstillegg(begrunnelseGrunnlag)
        }

    val filtrertPåEtterEndretUtbetaling =
        sanityBegrunnelser
            .filterValues { it.periodeResultat in hentResultaterForForrigePeriode(begrunnelseGrunnlag.forrigePeriode) }
            .filterValues { begrunnelse -> begrunnelse.erGjeldendeForRolle(person, behandling.fagsak.type) }
            .filterValues {
                it.erEtterEndretUtbetaling(
                    endretUtbetalingDennePerioden = endretUtbetalingDennePerioden,
                    endretUtbetalingForrigePeriode = hentEndretUtbetalingForrigePeriode(begrunnelseGrunnlag),
                )
            }

    @Suppress("UNCHECKED_CAST")
    val filtrertPåHendelser =
        (relevanteBegrunnelser as Map<IVedtakBegrunnelse, ISanityBegrunnelse>).filtrerPåHendelser(
            begrunnelseGrunnlag,
            vedtaksperiode.fom,
        )

    @Suppress("UNCHECKED_CAST")
    val filtrertPåSkalVisesSelvOmIkkeEndring =
        (relevanteBegrunnelser as Map<IVedtakBegrunnelse, ISanityBegrunnelse>).filtrerPåSkalVisesSelvOmIkkeEndring(begrunnelseGrunnlag.dennePerioden)

    return filtrertPåVilkårOgEndretUtbetaling +
        filtrertPåReduksjonFraForrigeBehandling +
        filtrertPåOpphørFraForrigeBehandling +
        filtrertPåSmåbarnstillegg +
        filtrertPåEtterEndretUtbetaling +
        filtrertPåHendelser +
        filtrertPåSkalVisesSelvOmIkkeEndring
}

private fun filtrerPåVilkår(
    it: SanityBegrunnelse,
    begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode,
    utvidetVilkårPåSøkerIPeriode: VilkårResultatForVedtaksperiode?,
    utvidetVilkårPåSøkerIForrigePeriode: VilkårResultatForVedtaksperiode?,
) =
    !it.begrunnelseGjelderReduksjonFraForrigeBehandling() &&
        it.erGjeldendeForUtgjørendeVilkår(
            begrunnelseGrunnlag,
            utvidetVilkårPåSøkerIPeriode,
            utvidetVilkårPåSøkerIForrigePeriode,
        ) &&
        it.erGjeldendeForRegelverk(
            begrunnelseGrunnlag,
        )

private fun filtrerPåEndretUtbetaling(
    it: SanityBegrunnelse,
    endretUtbetalingDennePerioden: IEndretUtbetalingAndelForVedtaksperiode?,
) = it.erEndretUtbetaling(endretUtbetalingDennePerioden)

private fun SanityBegrunnelse.erEndretUtbetaling(
    endretUtbetaling: IEndretUtbetalingAndelForVedtaksperiode?,
): Boolean {
    return this.gjelderEndretUtbetaling() && this.erLikEndretUtbetalingIPeriode(endretUtbetaling)
}

private fun SanityBegrunnelse.erGjeldendeForRegelverk(begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode): Boolean =
    begrunnelseGrunnlag.dennePerioden.vilkårResultater.none { it.vurderesEtter == Regelverk.EØS_FORORDNINGEN } || this.tema == Tema.FELLES

fun ISanityBegrunnelse.erGjeldendeForReduksjonFraForrigeBehandling(begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode): Boolean {
    if (begrunnelseGrunnlag !is BegrunnelseGrunnlagForPeriodeMedReduksjonPåTversAvBehandlinger) {
        return false
    }

    val oppfylteVilkårDenneBehandlingen =
        begrunnelseGrunnlag.dennePerioden.vilkårResultater.filter { it.resultat == Resultat.OPPFYLT }
            .map { it.vilkårType }.toSet()
    val oppfylteVilkårForrigeBehandling =
        begrunnelseGrunnlag.sammePeriodeForrigeBehandling?.vilkårResultater?.filter { it.resultat == Resultat.OPPFYLT }
            ?.map { it.vilkårType }?.toSet() ?: emptySet()

    val vilkårMistetSidenForrigeBehandling = oppfylteVilkårForrigeBehandling - oppfylteVilkårDenneBehandlingen

    val begrunnelseGjelderMistedeVilkår = this.vilkår.all { it in vilkårMistetSidenForrigeBehandling }

    val haddeSmåbarnstilleggForrigeBehandling = begrunnelseGrunnlag.erSmåbarnstilleggIForrigeBehandlingPeriode()
    val harSmåbarnstilleggDennePerioden =
        begrunnelseGrunnlag.dennePerioden.andeler.any { it.type == YtelseType.SMÅBARNSTILLEGG }

    val begrunnelseGjelderTaptSmåbarnstillegg =
        UtvidetBarnetrygdTrigger.SMÅBARNSTILLEGG in utvidetBarnetrygdTriggere && haddeSmåbarnstilleggForrigeBehandling && !harSmåbarnstilleggDennePerioden

    return begrunnelseGjelderReduksjonFraForrigeBehandling() && (begrunnelseGjelderMistedeVilkår || begrunnelseGjelderTaptSmåbarnstillegg)
}

private fun SanityBegrunnelse.erGjeldendeForSmåbarnstillegg(
    begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode,
): Boolean {
    val erSmåbarnstilleggForrigePeriode =
        begrunnelseGrunnlag.forrigePeriode?.andeler?.any { it.type == YtelseType.SMÅBARNSTILLEGG } == true
    val erSmåbarnstilleggDennePerioden =
        begrunnelseGrunnlag.dennePerioden.andeler.any { it.type == YtelseType.SMÅBARNSTILLEGG }

    val erSmåbarnstilleggIForrigeBehandlingPeriode = begrunnelseGrunnlag.erSmåbarnstilleggIForrigeBehandlingPeriode()

    val begrunnelseGjelderSmåbarnstillegg = UtvidetBarnetrygdTrigger.SMÅBARNSTILLEGG in utvidetBarnetrygdTriggere

    val erEndringISmåbarnstilleggFraForrigeBehandling =
        erSmåbarnstilleggIForrigeBehandlingPeriode != erSmåbarnstilleggDennePerioden

    val begrunnelseMatcherPeriodeResultat =
        this.matcherPerioderesultat(
            erSmåbarnstilleggForrigePeriode,
            erSmåbarnstilleggDennePerioden,
            erSmåbarnstilleggIForrigeBehandlingPeriode,
        )

    val erEndringISmåbarnstillegg = erSmåbarnstilleggForrigePeriode != erSmåbarnstilleggDennePerioden

    return begrunnelseGjelderSmåbarnstillegg && begrunnelseMatcherPeriodeResultat && (erEndringISmåbarnstillegg || erEndringISmåbarnstilleggFraForrigeBehandling)
}

private fun SanityBegrunnelse.erEtterEndretUtbetaling(
    endretUtbetalingDennePerioden: IEndretUtbetalingAndelForVedtaksperiode?,
    endretUtbetalingForrigePeriode: IEndretUtbetalingAndelForVedtaksperiode?,
): Boolean {
    if (!this.erEndringsårsakOgGjelderEtterEndretUtbetaling()) return false

    return this.matcherEtterEndretUtbetaling(
        endretUtbetalingDennePerioden = endretUtbetalingDennePerioden,
        endretUtbetalingForrigePeriode = endretUtbetalingForrigePeriode,
    )
}

private fun SanityBegrunnelse.matcherEtterEndretUtbetaling(
    endretUtbetalingDennePerioden: IEndretUtbetalingAndelForVedtaksperiode?,
    endretUtbetalingForrigePeriode: IEndretUtbetalingAndelForVedtaksperiode?,
): Boolean {
    val begrunnelseMatcherEndretUtbetalingIForrigePeriode =
        this.endringsaarsaker.all { it == endretUtbetalingForrigePeriode?.årsak }

    val begrunnelseMatcherEndretUtbetalingIDennePerioden =
        this.endringsaarsaker.all { it == endretUtbetalingDennePerioden?.årsak }

    if (!begrunnelseMatcherEndretUtbetalingIForrigePeriode || begrunnelseMatcherEndretUtbetalingIDennePerioden) return false

    return endretUtbetalingForrigePeriode?.årsak != Årsak.DELT_BOSTED ||
        this.erDeltBostedUtbetalingstype(
            endretUtbetalingForrigePeriode,
        )
}

private fun SanityBegrunnelse.erEndringsårsakOgGjelderEtterEndretUtbetaling() =
    this.endringsaarsaker.isNotEmpty() && this.gjelderEtterEndretUtbetaling()

private fun SanityBegrunnelse.gjelderEndretUtbetaling() =
    this.endringsaarsaker.isNotEmpty() && !this.gjelderEtterEndretUtbetaling()

private fun SanityBegrunnelse.erLikEndretUtbetalingIPeriode(
    endretUtbetaling: IEndretUtbetalingAndelForVedtaksperiode?,
): Boolean {
    if (endretUtbetaling == null) return false

    val erEndringsårsakerIBegrunnelseOgPeriodeLike = this.endringsaarsaker.all { it == endretUtbetaling.årsak }
    if (!erEndringsårsakerIBegrunnelseOgPeriodeLike) return false

    return if (endretUtbetaling.årsak == Årsak.DELT_BOSTED) {
        this.erDeltBostedUtbetalingstype(endretUtbetaling)
    } else {
        true
    }
}

private fun SanityBegrunnelse.erDeltBostedUtbetalingstype(
    endretUtbetaling: IEndretUtbetalingAndelForVedtaksperiode,
): Boolean {
    val inneholderAndelSomSkalUtbetales = endretUtbetaling.prosent != BigDecimal.ZERO

    return when (this.endretUtbetalingsperiodeDeltBostedUtbetalingTrigger) {
        EndretUtbetalingsperiodeDeltBostedTriggere.UTBETALING_IKKE_RELEVANT -> true
        EndretUtbetalingsperiodeDeltBostedTriggere.SKAL_UTBETALES -> inneholderAndelSomSkalUtbetales
        EndretUtbetalingsperiodeDeltBostedTriggere.SKAL_IKKE_UTBETALES -> !inneholderAndelSomSkalUtbetales
        null -> true
    }
}

private fun hentEndretUtbetalingForrigePeriode(begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode) =
    begrunnelseGrunnlag.forrigePeriode?.endretUtbetalingAndel.takeIf { begrunnelseGrunnlag.forrigePeriode?.erOrdinæreVilkårInnvilget() == true }

private fun SanityBegrunnelse.matcherPerioderesultat(
    erSmåbarnstilleggForrigePeriode: Boolean,
    erSmåbarnstilleggDennePerioden: Boolean,
    erSmåbarnstilleggIForrigeBehandlingPeriode: Boolean,
): Boolean {
    val erReduksjon =
        !erSmåbarnstilleggDennePerioden && (erSmåbarnstilleggForrigePeriode || erSmåbarnstilleggIForrigeBehandlingPeriode)
    val erØkning =
        erSmåbarnstilleggDennePerioden && (!erSmåbarnstilleggForrigePeriode || !erSmåbarnstilleggIForrigeBehandlingPeriode)

    val erBegrunnelseReduksjon = periodeResultat == SanityPeriodeResultat.REDUKSJON
    val erBegrunnelseØkning = periodeResultat == SanityPeriodeResultat.INNVILGET_ELLER_ØKNING

    val reduksjonMatcher = erReduksjon == erBegrunnelseReduksjon
    val økningMatcher = erØkning == erBegrunnelseØkning
    return reduksjonMatcher && økningMatcher
}

fun ISanityBegrunnelse.erGjeldendeForOpphørFraForrigeBehandling(begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode): Boolean {
    if (begrunnelseGrunnlag !is BegrunnelseGrunnlagForPeriodeMedOpphør || !begrunnelseGjelderOpphørFraForrigeBehandling()) {
        return false
    }

    val oppfylteVilkårDenneBehandlingen =
        begrunnelseGrunnlag.dennePerioden.vilkårResultater.filter { it.resultat == Resultat.OPPFYLT }
            .map { it.vilkårType }.toSet()

    val oppfylteVilkårsresultaterForrigeBehandling =
        begrunnelseGrunnlag.sammePeriodeForrigeBehandling?.vilkårResultater?.filter { it.resultat == Resultat.OPPFYLT }
    val oppfylteVilkårForrigeBehandling =
        oppfylteVilkårsresultaterForrigeBehandling?.map { it.vilkårType }?.toSet() ?: emptySet()

    val vilkårMistetSidenForrigeBehandling = oppfylteVilkårForrigeBehandling - oppfylteVilkårDenneBehandlingen

    val begrunnelseGjelderMistedeVilkår =
        this.erLikVilkårOgUtdypendeVilkårIPeriode(
            oppfylteVilkårsresultaterForrigeBehandling?.filter { it.vilkårType in vilkårMistetSidenForrigeBehandling }
                ?: emptyList(),
        )

    val ikkeOppfylteVilkårDenneBehandlingen = begrunnelseGrunnlag.dennePerioden.vilkårResultater.filter { it.resultat == Resultat.IKKE_OPPFYLT }

    val begrunnelsenGjelderVilkårIkkeLengerOppfylt = this.erLikVilkårOgUtdypendeVilkårIPeriode(ikkeOppfylteVilkårDenneBehandlingen.filter { it.vilkårType in vilkårMistetSidenForrigeBehandling })

    val dennePeriodenErFørsteVedtaksperiodePåFagsak =
        begrunnelseGrunnlag.forrigePeriode == null || begrunnelseGrunnlag.forrigePeriode!!.andeler.firstOrNull() == null

    return (begrunnelseGjelderMistedeVilkår || begrunnelsenGjelderVilkårIkkeLengerOppfylt) && dennePeriodenErFørsteVedtaksperiodePåFagsak
}
