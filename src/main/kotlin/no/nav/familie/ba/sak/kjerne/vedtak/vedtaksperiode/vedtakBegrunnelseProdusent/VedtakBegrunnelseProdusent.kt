package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.brev.brevBegrunnelseProdusent.GrunnlagForBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.ISanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityPeriodeResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.Tema
import no.nav.familie.ba.sak.kjerne.brev.domene.Valgbarhet
import no.nav.familie.ba.sak.kjerne.brev.domene.eøs.landkodeTilBarnetsBostedsland
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.BrevPeriodeType
import no.nav.familie.ba.sak.kjerne.brev.domene.tilPersonType
import no.nav.familie.ba.sak.kjerne.brev.domene.ØvrigTrigger
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.domene.hentBrevPeriodeType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.hentBegrunnelser.hentAvslagsbegrunnelserPerPerson
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.hentBegrunnelser.hentEØSStandardBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.hentBegrunnelser.hentStandardBegrunnelser
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import java.math.BigDecimal
import java.time.LocalDate

fun VedtaksperiodeMedBegrunnelser.hentGyldigeBegrunnelserForPeriode(
    grunnlagForBegrunnelser: GrunnlagForBegrunnelse,
): Set<IVedtakBegrunnelse> {
    val gyldigeBegrunnelserPerPerson =
        hentGyldigeBegrunnelserPerPerson(
            grunnlagForBegrunnelser,
        )

    return gyldigeBegrunnelserPerPerson.values.flatten().toSet()
}

fun VedtaksperiodeMedBegrunnelser.hentGyldigeBegrunnelserPerPerson(
    grunnlag: GrunnlagForBegrunnelse,
): Map<Person, Set<IVedtakBegrunnelse>> {
    val avslagsbegrunnelserPerPerson = hentAvslagsbegrunnelserPerPerson(grunnlag.behandlingsGrunnlagForVedtaksperioder)

    if (this.type == Vedtaksperiodetype.AVSLAG) {
        return avslagsbegrunnelserPerPerson
    }

    val begrunnelseGrunnlagPerPerson =
        this.finnBegrunnelseGrunnlagPerPerson(
            grunnlag,
        )

    if (this.type == Vedtaksperiodetype.FORTSATT_INNVILGET) {
        return hentFortsattInnvilgetBegrunnelserPerPerson(
            begrunnelseGrunnlagPerPerson = begrunnelseGrunnlagPerPerson,
            grunnlag = grunnlag,
            vedtaksperiode = this,
        )
    }

    val erUtbetalingEllerDeltBostedIPeriode = erUtbetalingEllerDeltBostedIPeriode(begrunnelseGrunnlagPerPerson)

    val begrunnelseGrunnlagForSøkerIPeriode =
        begrunnelseGrunnlagPerPerson
            .filterKeys { it.type == PersonType.SØKER }
            .values
            .firstOrNull()

    val utvidetVilkårPåSøkerIPeriode =
        begrunnelseGrunnlagForSøkerIPeriode
            ?.dennePerioden
            ?.vilkårResultater
            ?.singleOrNull { it.vilkårType == Vilkår.UTVIDET_BARNETRYGD }

    val utvidetVilkårPåSøkerIForrigePeriode =
        begrunnelseGrunnlagForSøkerIPeriode
            ?.forrigePeriode
            ?.vilkårResultater
            ?.singleOrNull { it.vilkårType == Vilkår.UTVIDET_BARNETRYGD }

    val erUtbetalingPåSøkerIPeriode =
        begrunnelseGrunnlagForSøkerIPeriode
            ?.dennePerioden
            ?.andeler
            ?.sumOf { it.kalkulertUtbetalingsbeløp }
            ?.let { it > 0 } ?: false

    val erReduksjonIFinnmarkstilleggIPeriode = hentErReduksjonIFinnmarkstilleggIPeriode(grunnlag.behandlingsGrunnlagForVedtaksperioder.andelerTilkjentYtelse, this.fom)
    val erReduksjonISvalbardtilleggIPeriode = hentErReduksjonISvalbardtilleggIPeriode(grunnlag.behandlingsGrunnlagForVedtaksperioder.andelerTilkjentYtelse, this.fom)

    return begrunnelseGrunnlagPerPerson.mapValues { (person, begrunnelseGrunnlag) ->
        val relevantePeriodeResultater =
            hentResultaterForPeriode(
                begrunnelseGrunnlagForPeriode = begrunnelseGrunnlag.dennePerioden,
                begrunnelseGrunnlagForrigePeriode = begrunnelseGrunnlag.forrigePeriode,
                erUtbetalingPåSøkerIPeriode = erUtbetalingPåSøkerIPeriode,
                erReduksjonIFinnmarkstillegg = erReduksjonIFinnmarkstilleggIPeriode,
                erReduksjonISvalbardtilleggIPeriode = erReduksjonISvalbardtilleggIPeriode,
            )

        val temaSomPeriodeErVurdertEtter = hentTemaSomPeriodeErVurdertEtter(begrunnelseGrunnlag)

        val standardBegrunnelser =
            hentStandardBegrunnelser(
                begrunnelseGrunnlag = begrunnelseGrunnlag,
                sanityBegrunnelser = grunnlag.sanityBegrunnelser,
                person = person,
                vedtaksperiode = this,
                behandling = grunnlag.behandlingsGrunnlagForVedtaksperioder.behandling,
                relevantePeriodeResultater = relevantePeriodeResultater,
                erUtbetalingEllerDeltBostedIPeriode = erUtbetalingEllerDeltBostedIPeriode,
                utvidetVilkårPåSøkerIPeriode = utvidetVilkårPåSøkerIPeriode,
                utvidetVilkårPåSøkerIForrigePeriode = utvidetVilkårPåSøkerIForrigePeriode,
                temaSomPeriodeErVurdertEtter = temaSomPeriodeErVurdertEtter,
            )

        val eøsBegrunnelser =
            hentEØSStandardBegrunnelser(
                sanityEØSBegrunnelser = grunnlag.sanityEØSBegrunnelser,
                begrunnelseGrunnlag = begrunnelseGrunnlag,
                relevantePeriodeResultater = relevantePeriodeResultater,
                behandling = grunnlag.behandlingsGrunnlagForVedtaksperioder.behandling,
                erUtbetalingEllerDeltBostedIPeriode = erUtbetalingEllerDeltBostedIPeriode,
                vedtaksperiode = this,
                utvidetVilkårPåSøkerIPeriode = utvidetVilkårPåSøkerIPeriode,
                utvidetVilkårPåSøkerIForrigePeriode = utvidetVilkårPåSøkerIForrigePeriode,
                temaSomPeriodeErVurdertEtter = temaSomPeriodeErVurdertEtter,
            )

        val avslagsbegrunnelser = avslagsbegrunnelserPerPerson[person] ?: emptySet()

        val standardOgEøsBegrunnelser =
            (standardBegrunnelser + eøsBegrunnelser)
                .filter { !it.vedtakBegrunnelseType.erAvslag() }

        (standardOgEøsBegrunnelser + avslagsbegrunnelser).toSet()
    }
}

private fun hentErReduksjonIFinnmarkstilleggIPeriode(
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    periodeFom: LocalDate?,
): Boolean {
    if (periodeFom == null) return false

    val erFinnmarksAndelDennePerioden =
        andelerTilkjentYtelse
            .filter { it.stønadFom == periodeFom.toYearMonth() }
            .any { it.type == YtelseType.FINNMARKSTILLEGG }

    val finnmarksAndelForrigePeriode =
        andelerTilkjentYtelse
            .filter { it.stønadTom == periodeFom.toYearMonth().minusMonths(1) }
            .any { it.type == YtelseType.FINNMARKSTILLEGG }

    return !erFinnmarksAndelDennePerioden && finnmarksAndelForrigePeriode
}

private fun hentErReduksjonISvalbardtilleggIPeriode(
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    periodeFom: LocalDate?,
): Boolean {
    if (periodeFom == null) return false

    val erSvalbardAndelDennePerioden =
        andelerTilkjentYtelse
            .filter { it.stønadFom == periodeFom.toYearMonth() }
            .any { it.type == YtelseType.SVALBARDTILLEGG }

    val svalbardAndelForrigePeriode =
        andelerTilkjentYtelse
            .filter { it.stønadTom == periodeFom.toYearMonth().minusMonths(1) }
            .any { it.type == YtelseType.SVALBARDTILLEGG }

    return !erSvalbardAndelDennePerioden && svalbardAndelForrigePeriode
}

fun erUtbetalingEllerDeltBostedIPeriode(begrunnelseGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>) =
    begrunnelseGrunnlagPerPerson.values.any { grunnlagForPeriode ->
        val dennePerioden = grunnlagForPeriode.dennePerioden
        dennePerioden.endretUtbetalingAndel?.årsak == Årsak.DELT_BOSTED ||
            dennePerioden.andeler.any { it.prosent != BigDecimal.ZERO }
    }

fun finnRegelverkSomBlirBorte(
    dennePerioden: BegrunnelseGrunnlagForPersonIPeriode,
    forrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?,
): Tema? {
    if (forrigePeriode == null) return null
    val vilkårRelevantForRegelverk = listOf(Vilkår.BOR_MED_SØKER, Vilkår.BOSATT_I_RIKET, Vilkår.LOVLIG_OPPHOLD)
    val finnesVilkårSomStopperOpp =
        forrigePeriode.vilkårResultater.any { vilkårForrigePeriode -> vilkårForrigePeriode.resultat == Resultat.OPPFYLT && (dennePerioden.vilkårResultater.none { it.vilkårType == vilkårForrigePeriode.vilkårType } || dennePerioden.vilkårResultater.any { it.vilkårType == vilkårForrigePeriode.vilkårType && it.resultat == Resultat.IKKE_OPPFYLT }) }

    val kompetanseStopperOpp = forrigePeriode.kompetanse != null && dennePerioden.kompetanse == null

    return if (kompetanseStopperOpp) {
        Tema.EØS
    } else if (finnesVilkårSomStopperOpp) {
        if (forrigePeriode.vilkårResultater
                .filter { it.vilkårType in vilkårRelevantForRegelverk }
                .any { it.vurderesEtter == Regelverk.EØS_FORORDNINGEN }
        ) {
            Tema.EØS
        } else {
            Tema.NASJONAL
        }
    } else {
        null
    }
}

internal fun ISanityBegrunnelse.skalVisesSelvOmIkkeEndring(
    begrunnelseGrunnlagForPersonIPeriode: BegrunnelseGrunnlagForPersonIPeriode,
): Boolean {
    val begrunnelseMatcherVilkår =
        when (this.periodeResultat) {
            SanityPeriodeResultat.INNVILGET_ELLER_ØKNING, SanityPeriodeResultat.INGEN_ENDRING, SanityPeriodeResultat.REDUKSJON -> {
                this.vilkår.isNotEmpty() &&
                    this.vilkår.any { vilkår ->
                        begrunnelseGrunnlagForPersonIPeriode.vilkårResultater.any { it.vilkårType == vilkår && it.resultat == Resultat.OPPFYLT }
                    }
            }

            SanityPeriodeResultat.IKKE_INNVILGET, SanityPeriodeResultat.IKKE_RELEVANT -> {
                false
            }

            null -> {
                false
            }
        }
    val begrunnelseSkalVisesSelvOmIkkeEndring =
        ØvrigTrigger.SKAL_VISES_SELV_OM_IKKE_ENDRING in this.øvrigeTriggere
    return begrunnelseSkalVisesSelvOmIkkeEndring && begrunnelseMatcherVilkår
}

internal fun ISanityBegrunnelse.matcherErAutomatisk(erAutomatiskBehandling: Boolean): Boolean =
    when {
        this.valgbarhet != Valgbarhet.AUTOMATISK -> !erAutomatiskBehandling
        ØvrigTrigger.ALLTID_AUTOMATISK in this.øvrigeTriggere -> erAutomatiskBehandling
        else -> true
    }

fun ISanityBegrunnelse.erGjeldendeForFagsakType(
    fagsakType: FagsakType,
) = if (valgbarhet == Valgbarhet.SAKSPESIFIKK) {
    fagsakType == this.fagsakType
} else {
    true
}

internal fun ISanityBegrunnelse.begrunnelseSkalTriggesForOpphørFraForrigeBehandling() = ØvrigTrigger.OPPHØR_FRA_FORRIGE_BEHANDLING in this.øvrigeTriggere

fun SanityBegrunnelse.erGjeldendeForRolle(
    person: Person,
    fagsakType: FagsakType,
): Boolean {
    val rolleErRelevantForBegrunnelse = this.rolle.isNotEmpty()

    val begrunnelseGjelderPersonSinRolle =
        person.type in this.rolle.map { it.tilPersonType() } || fagsakType.erBarnSøker()

    return !rolleErRelevantForBegrunnelse || begrunnelseGjelderPersonSinRolle
}

fun SanityEØSBegrunnelse.erLikKompetanseIPeriode(
    begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode,
): Boolean {
    val kompetanse =
        when (this.periodeResultat) {
            SanityPeriodeResultat.INNVILGET_ELLER_ØKNING, SanityPeriodeResultat.INGEN_ENDRING -> {
                begrunnelseGrunnlag.dennePerioden.kompetanse
                    ?: return false
            }

            SanityPeriodeResultat.IKKE_INNVILGET,
            SanityPeriodeResultat.REDUKSJON,
            -> {
                begrunnelseGrunnlag.forrigePeriode?.kompetanse ?: return false
            }

            SanityPeriodeResultat.IKKE_RELEVANT,
            null,
            -> {
                return false
            }
        }

    return this.annenForeldersAktivitet.contains(kompetanse.annenForeldersAktivitet) &&
        this.barnetsBostedsland.contains(
            landkodeTilBarnetsBostedsland(kompetanse.barnetsBostedsland),
        ) &&
        this.kompetanseResultat.contains(kompetanse.resultat)
}

internal fun hentResultaterForForrigePeriode(
    begrunnelseGrunnlagForrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?,
) = if (begrunnelseGrunnlagForrigePeriode?.erOrdinæreVilkårInnvilget() == true && begrunnelseGrunnlagForrigePeriode.erInnvilgetEtterEndretUtbetaling()) {
    listOf(
        SanityPeriodeResultat.REDUKSJON,
        SanityPeriodeResultat.INNVILGET_ELLER_ØKNING,
    )
} else {
    listOf(
        SanityPeriodeResultat.REDUKSJON,
        SanityPeriodeResultat.IKKE_INNVILGET,
    )
}

internal fun hentEndretUtbetalingDennePerioden(begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode) = begrunnelseGrunnlag.dennePerioden.endretUtbetalingAndel.takeIf { begrunnelseGrunnlag.dennePerioden.erOrdinæreVilkårInnvilget() }

fun ISanityBegrunnelse.erGjeldendeForBrevPeriodeType(
    vedtaksperiode: VedtaksperiodeMedBegrunnelser,
    erUtbetalingEllerDeltBostedIPeriode: Boolean,
): Boolean {
    val brevPeriodeType =
        hentBrevPeriodeType(
            vedtaksperiode.type,
            vedtaksperiode.fom,
            erUtbetalingEllerDeltBostedIPeriode,
        )
    return this.periodeType == brevPeriodeType || this.periodeType == BrevPeriodeType.IKKE_RELEVANT
}
