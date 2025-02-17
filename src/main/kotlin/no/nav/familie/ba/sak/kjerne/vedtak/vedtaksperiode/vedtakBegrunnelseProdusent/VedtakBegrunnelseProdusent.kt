package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent

import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
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
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TomTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.mapInnhold
import no.nav.familie.ba.sak.kjerne.tidslinje.månedPeriodeAv
import no.nav.familie.ba.sak.kjerne.tidslinje.periodeAv
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilNesteMåned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonthEllerNull
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonthEllerUendeligFortid
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonthEllerUendeligFramtid
import no.nav.familie.ba.sak.kjerne.tidslinje.tilTidslinje
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.domene.hentBrevPeriodeType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.hentBegrunnelser.hentEØSStandardBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.hentBegrunnelser.hentStandardBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.AndelForVedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.BehandlingsGrunnlagForVedtaksperioder
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilForskjøvedeVilkårTidslinjer
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

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

    val utbetalingPåSøkerIPeriode =
        begrunnelseGrunnlagForSøkerIPeriode
            ?.dennePerioden
            ?.andeler
            ?.sumOf { it.kalkulertUtbetalingsbeløp }
            ?.let { it > 0 } ?: false

    return begrunnelseGrunnlagPerPerson.mapValues { (person, begrunnelseGrunnlag) ->
        val relevantePeriodeResultater =
            hentResultaterForPeriode(begrunnelseGrunnlag.dennePerioden, begrunnelseGrunnlag.forrigePeriode, utbetalingPåSøkerIPeriode)

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

fun erUtbetalingEllerDeltBostedIPeriode(begrunnelseGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>) =
    begrunnelseGrunnlagPerPerson.values.any { grunnlagForPeriode ->
        val dennePerioden = grunnlagForPeriode.dennePerioden
        dennePerioden.endretUtbetalingAndel?.årsak == Årsak.DELT_BOSTED ||
            dennePerioden.andeler.any { it.prosent != BigDecimal.ZERO }
    }

fun ISanityBegrunnelse.erSammeTemaSomPeriode(
    temaerForBegrunnelser: TemaerForBegrunnelser,
): Boolean =
    if (this.periodeResultat == SanityPeriodeResultat.IKKE_INNVILGET) {
        temaerForBegrunnelser.temaerForOpphør.contains(tema) || tema == Tema.FELLES
    } else {
        temaerForBegrunnelser.temaForUtbetaling == tema || tema == Tema.FELLES
    }

data class TemaerForBegrunnelser(
    val temaerForOpphør: Set<Tema>,
    val temaForUtbetaling: Tema,
)

fun hentTemaSomPeriodeErVurdertEtter(
    begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode,
): TemaerForBegrunnelser {
    val regelverkSomBlirBorteFraForrigePeriode =
        finnRegelverkSomBlirBorte(
            dennePerioden = begrunnelseGrunnlag.dennePerioden,
            forrigePeriode = begrunnelseGrunnlag.forrigePeriode,
        )
    val regelverkSomBlirBorteFraForrigeBehandling =
        finnRegelverkSomBlirBorte(
            dennePerioden = begrunnelseGrunnlag.dennePerioden,
            forrigePeriode = begrunnelseGrunnlag.sammePeriodeForrigeBehandling,
        )
    val vurdertEtterEøsDennePerioden =
        begrunnelseGrunnlag.dennePerioden.vilkårResultater.any { it.vurderesEtter == Regelverk.EØS_FORORDNINGEN }

    val regelverkSomBlirBorte =
        listOfNotNull(regelverkSomBlirBorteFraForrigePeriode, regelverkSomBlirBorteFraForrigeBehandling).toSet()

    return TemaerForBegrunnelser(
        temaerForOpphør = regelverkSomBlirBorte.ifEmpty { setOf(Tema.NASJONAL) },
        temaForUtbetaling = if (vurdertEtterEøsDennePerioden) Tema.EØS else Tema.NASJONAL,
    )
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

private fun VedtaksperiodeMedBegrunnelser.hentAvslagsbegrunnelserPerPerson(
    behandlingsGrunnlagForVedtaksperioder: BehandlingsGrunnlagForVedtaksperioder,
): Map<Person, Set<IVedtakBegrunnelse>> =
    behandlingsGrunnlagForVedtaksperioder.persongrunnlag.personer.associateWith { person ->
        val vilkårResultaterForPerson =
            behandlingsGrunnlagForVedtaksperioder
                .personResultater
                .firstOrNull { it.aktør == person.aktør }
                ?.vilkårResultater ?: emptyList()

        val (generelleAvslag, vilkårResultaterUtenGenerelleAvslag) = vilkårResultaterForPerson.partition { it.erEksplisittAvslagUtenPeriode() }

        val generelleAvslagsbegrunnelser = generelleAvslag.flatMap { it.standardbegrunnelser }

        val avslagsbegrunnelserMedPeriodeTidslinjer =
            vilkårResultaterUtenGenerelleAvslag
                .tilForskjøvedeVilkårTidslinjer(person.fødselsdato)
                .filtrerKunEksplisittAvslagsPerioder()

        val avslagsbegrunnelserMedPeriode = avslagsbegrunnelserMedPeriodeTidslinjer.flatMap { it.perioder() }.filter { it.fraOgMed.tilYearMonthEllerNull() == this.fom?.toYearMonth() }.flatMap { it.innhold?.standardbegrunnelser ?: emptyList() }

        (generelleAvslagsbegrunnelser + avslagsbegrunnelserMedPeriode).toSet()
    }

private fun List<Tidslinje<VilkårResultat, Måned>>.filtrerKunEksplisittAvslagsPerioder(): List<Tidslinje<VilkårResultat, Måned>> =
    this.map { tidslinjeForVilkår ->
        val eksplisittAvslagsPerioder =
            tidslinjeForVilkår
                .perioder()
                .filter { it.innhold?.erEksplisittAvslagPåSøknad == true }

        tidslinje { eksplisittAvslagsPerioder }
    }

internal fun ISanityBegrunnelse.skalVisesSelvOmIkkeEndring(
    begrunnelseGrunnlagForPersonIPeriode: BegrunnelseGrunnlagForPersonIPeriode,
): Boolean {
    val begrunnelseMatcherVilkår =
        when (this.periodeResultat) {
            SanityPeriodeResultat.INNVILGET_ELLER_ØKNING, SanityPeriodeResultat.INGEN_ENDRING, SanityPeriodeResultat.REDUKSJON ->
                this.vilkår.isNotEmpty() &&
                    this.vilkår.any { vilkår ->
                        begrunnelseGrunnlagForPersonIPeriode.vilkårResultater.any { it.vilkårType == vilkår && it.resultat == Resultat.OPPFYLT }
                    }

            SanityPeriodeResultat.IKKE_INNVILGET, SanityPeriodeResultat.IKKE_RELEVANT -> false
            null -> false
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

internal fun ISanityBegrunnelse.begrunnelseGjelderReduksjonFraForrigeBehandling() = ØvrigTrigger.REDUKSJON_FRA_FORRIGE_BEHANDLING in this.øvrigeTriggere

internal fun ISanityBegrunnelse.begrunnelseGjelderOpphørFraForrigeBehandling() = ØvrigTrigger.OPPHØR_FRA_FORRIGE_BEHANDLING in this.øvrigeTriggere

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
            SanityPeriodeResultat.INNVILGET_ELLER_ØKNING, SanityPeriodeResultat.INGEN_ENDRING ->
                begrunnelseGrunnlag.dennePerioden.kompetanse
                    ?: return false

            SanityPeriodeResultat.IKKE_INNVILGET,
            SanityPeriodeResultat.REDUKSJON,
            -> begrunnelseGrunnlag.forrigePeriode?.kompetanse ?: return false

            SanityPeriodeResultat.IKKE_RELEVANT,
            null,
            -> return false
        }

    return this.annenForeldersAktivitet.contains(kompetanse.annenForeldersAktivitet) &&
        this.barnetsBostedsland.contains(
            landkodeTilBarnetsBostedsland(kompetanse.barnetsBostedsland),
        ) &&
        this.kompetanseResultat.contains(kompetanse.resultat)
}

fun ISanityBegrunnelse.skalFiltreresPåHendelser(
    begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode,
    fomVedtaksperiode: LocalDate?,
): Boolean =
    if (!begrunnelseGrunnlag.dennePerioden.erOrdinæreVilkårInnvilget()) {
        val person = begrunnelseGrunnlag.dennePerioden.person

        this.erBarnDød(person, fomVedtaksperiode)
    } else {
        val person = begrunnelseGrunnlag.dennePerioden.person

        this.erBarn6År(person, fomVedtaksperiode) ||
            this.erSatsendring(
                person,
                begrunnelseGrunnlag.dennePerioden.andeler,
                fomVedtaksperiode,
            )
    }

fun ISanityBegrunnelse.erBarn6År(
    person: Person,
    fomVedtaksperiode: LocalDate?,
): Boolean {
    val blirPerson6DennePerioden = person.hentSeksårsdag().toYearMonth() == fomVedtaksperiode?.toYearMonth()

    return blirPerson6DennePerioden && this.øvrigeTriggere.contains(ØvrigTrigger.BARN_MED_6_ÅRS_DAG)
}

fun ISanityBegrunnelse.erBarnDød(
    person: Person,
    fomVedtaksperiode: LocalDate?,
): Boolean {
    val dødsfall = person.dødsfall
    val personDødeForrigeMåned =
        dødsfall != null && dødsfall.dødsfallDato.toYearMonth().plusMonths(1) == fomVedtaksperiode?.toYearMonth()

    return personDødeForrigeMåned &&
        person.type == PersonType.BARN &&
        this.øvrigeTriggere.contains(ØvrigTrigger.BARN_DØD)
}

fun ISanityBegrunnelse.erSatsendring(
    person: Person,
    andeler: Iterable<AndelForVedtaksbegrunnelse>,
    fomVedtaksperiode: LocalDate?,
): Boolean {
    val satstyperPåAndelene = andeler.map { it.type.tilSatsType(person, fomVedtaksperiode ?: TIDENES_MORGEN) }.toSet()

    val erSatsendringIPeriodenForPerson =
        satstyperPåAndelene.any { satstype ->
            SatsService.finnAlleSatserFor(satstype).any { it.gyldigFom == fomVedtaksperiode }
        }

    return erSatsendringIPeriodenForPerson && this.øvrigeTriggere.contains(ØvrigTrigger.SATSENDRING)
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

private fun hentResultaterForPeriode(
    begrunnelseGrunnlagForPeriode: BegrunnelseGrunnlagForPersonIPeriode,
    begrunnelseGrunnlagForrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?,
    utbetalingPåSøkerIPeriode: Boolean,
): List<SanityPeriodeResultat> {
    val erAndelerPåPersonHvisBarn =
        begrunnelseGrunnlagForPeriode.person.type != PersonType.BARN ||
            begrunnelseGrunnlagForPeriode.andeler
                .toList()
                .isNotEmpty()

    val erInnvilgetEtterVilkårOgEndretUtbetaling =
        begrunnelseGrunnlagForPeriode.erOrdinæreVilkårInnvilget() && begrunnelseGrunnlagForPeriode.erInnvilgetEtterEndretUtbetaling()

    val erReduksjonIAndel =
        erReduksjonIAndelMellomPerioder(
            begrunnelseGrunnlagForPeriode,
            begrunnelseGrunnlagForrigePeriode,
        )

    return if (erInnvilgetEtterVilkårOgEndretUtbetaling && erAndelerPåPersonHvisBarn || utbetalingPåSøkerIPeriode) {
        val erEøs = begrunnelseGrunnlagForPeriode.kompetanse != null
        val erØkingIAndel =
            erØkningIAndelMellomPerioder(
                begrunnelseGrunnlagForPeriode,
                begrunnelseGrunnlagForrigePeriode,
            )
        val erSatsøkning =
            erSatsøkningMellomPerioder(
                begrunnelseGrunnlagForPeriode,
                begrunnelseGrunnlagForrigePeriode,
            )

        val erSøker = begrunnelseGrunnlagForPeriode.person.type == PersonType.SØKER
        val erOrdinæreVilkårOppfyltIForrigePeriode =
            begrunnelseGrunnlagForrigePeriode?.erOrdinæreVilkårInnvilget() == true

        val erIngenEndring = !erØkingIAndel && !erReduksjonIAndel && erOrdinæreVilkårOppfyltIForrigePeriode
        val erKunReduksjonAvSats =
            erKunReduksjonAvSats(begrunnelseGrunnlagForPeriode, begrunnelseGrunnlagForrigePeriode)

        listOfNotNull(
            if (erØkingIAndel || erSatsøkning || erSøker || erIngenEndring || erEøs || erKunReduksjonAvSats) SanityPeriodeResultat.INNVILGET_ELLER_ØKNING else null,
            if (erReduksjonIAndel) SanityPeriodeResultat.REDUKSJON else null,
            if (erIngenEndring || erKunReduksjonAvSats) SanityPeriodeResultat.INGEN_ENDRING else null,
        )
    } else {
        listOfNotNull(
            if (erReduksjonIAndel) SanityPeriodeResultat.REDUKSJON else null,
            SanityPeriodeResultat.IKKE_INNVILGET,
        )
    }
}

private fun erKunReduksjonAvSats(
    begrunnelseGrunnlagForPeriode: BegrunnelseGrunnlagForPersonIPeriode,
    begrunnelseGrunnlagForrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?,
): Boolean {
    val andelerForrigePeriode = begrunnelseGrunnlagForrigePeriode?.andeler ?: emptyList()
    val andelerDennePerioden = begrunnelseGrunnlagForPeriode.andeler

    return andelerForrigePeriode.any { andelIForrigePeriode ->
        val sammeAndelDennePerioden = andelerDennePerioden.singleOrNull { andelIForrigePeriode.type == it.type }

        val harAndelSammeProsent =
            sammeAndelDennePerioden != null && andelIForrigePeriode.prosent == sammeAndelDennePerioden.prosent
        val satsErRedusert = andelIForrigePeriode.sats > (sammeAndelDennePerioden?.sats ?: 0)

        harAndelSammeProsent && satsErRedusert
    }
}

private fun erReduksjonIAndelMellomPerioder(
    begrunnelseGrunnlagForPeriode: BegrunnelseGrunnlagForPersonIPeriode?,
    begrunnelseGrunnlagForrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?,
): Boolean {
    val andelerForrigePeriode = begrunnelseGrunnlagForrigePeriode?.andeler ?: emptyList()
    val andelerDennePerioden = begrunnelseGrunnlagForPeriode?.andeler ?: emptyList()

    return andelerForrigePeriode.any { andelIForrigePeriode ->
        val sammeAndelDennePerioden = andelerDennePerioden.singleOrNull { andelIForrigePeriode.type == it.type }

        val erAndelenMistet =
            sammeAndelDennePerioden == null && begrunnelseGrunnlagForrigePeriode?.erInnvilgetEtterEndretUtbetaling() == true
        val harAndelenGåttNedIProsent =
            sammeAndelDennePerioden != null && andelIForrigePeriode.prosent > sammeAndelDennePerioden.prosent
        val erSatsenRedusert = andelIForrigePeriode.sats > (sammeAndelDennePerioden?.sats ?: 0)

        erAndelenMistet || harAndelenGåttNedIProsent || erSatsenRedusert
    }
}

private fun erØkningIAndelMellomPerioder(
    begrunnelseGrunnlagForPeriode: BegrunnelseGrunnlagForPersonIPeriode,
    begrunnelseGrunnlagForrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?,
): Boolean {
    val andelerForrigePeriode = begrunnelseGrunnlagForrigePeriode?.andeler ?: emptyList()
    val andelerDennePerioden = begrunnelseGrunnlagForPeriode.andeler

    return andelerDennePerioden.any { andelIPeriode ->
        val sammeAndelForrigePeriode = andelerForrigePeriode.singleOrNull { andelIPeriode.type == it.type }

        val erAndelenTjent =
            sammeAndelForrigePeriode == null && begrunnelseGrunnlagForPeriode.erInnvilgetEtterEndretUtbetaling()
        val harAndelenGåttOppIProsent =
            sammeAndelForrigePeriode != null && andelIPeriode.prosent > sammeAndelForrigePeriode.prosent

        erAndelenTjent || harAndelenGåttOppIProsent
    }
}

private fun erSatsøkningMellomPerioder(
    begrunnelseGrunnlagForPeriode: BegrunnelseGrunnlagForPersonIPeriode,
    begrunnelseGrunnlagForrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?,
): Boolean {
    val andelerForrigePeriode = begrunnelseGrunnlagForrigePeriode?.andeler ?: emptyList()
    val andelerDennePerioden = begrunnelseGrunnlagForPeriode.andeler
    return andelerDennePerioden.any { andelIPeriode ->
        val sammeAndelForrigePeriode = andelerForrigePeriode.singleOrNull { andelIPeriode.type == it.type }
        sammeAndelForrigePeriode != null && andelIPeriode.sats > sammeAndelForrigePeriode.sats
    }
}

internal fun hentEndretUtbetalingDennePerioden(begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode) = begrunnelseGrunnlag.dennePerioden.endretUtbetalingAndel.takeIf { begrunnelseGrunnlag.dennePerioden.erOrdinæreVilkårInnvilget() }

fun VedtaksperiodeMedBegrunnelser.finnBegrunnelseGrunnlagPerPerson(
    grunnlag: GrunnlagForBegrunnelse,
): Map<Person, IBegrunnelseGrunnlagForPeriode> {
    val tidslinjeMedVedtaksperioden = this.tilTidslinjeForAktuellPeriode()

    val begrunnelsegrunnlagTidslinjerPerPerson =
        grunnlag.behandlingsGrunnlagForVedtaksperioder.lagBegrunnelseGrunnlagTidslinjer()

    val grunnlagTidslinjePerPersonForrigeBehandling =
        grunnlag.behandlingsGrunnlagForVedtaksperioderForrigeBehandling?.lagBegrunnelseGrunnlagTidslinjer()

    return begrunnelsegrunnlagTidslinjerPerPerson.mapValues { (person, grunnlagTidslinje) ->
        val grunnlagMedForrigePeriodeOgBehandlingTidslinje =
            tidslinjeMedVedtaksperioden.lagTidslinjeGrunnlagDennePeriodenForrigePeriodeOgPeriodeForrigeBehandling(
                grunnlagTidslinje,
                grunnlagTidslinjePerPersonForrigeBehandling,
                person,
            )

        val begrunnelseperioderIVedtaksperiode =
            grunnlagMedForrigePeriodeOgBehandlingTidslinje.perioder().mapNotNull { it.innhold }

        when (this.type) {
            Vedtaksperiodetype.OPPHØR -> begrunnelseperioderIVedtaksperiode.first()
            Vedtaksperiodetype.FORTSATT_INNVILGET ->
                if (this.fom == null && this.tom == null) {
                    val perioder = grunnlagMedForrigePeriodeOgBehandlingTidslinje.perioder()
                    perioder.single { grunnlag.nåDato.toYearMonth() in it.fraOgMed.tilYearMonthEllerUendeligFortid()..it.tilOgMed.tilYearMonthEllerUendeligFramtid() }.innhold!!
                } else {
                    begrunnelseperioderIVedtaksperiode.first()
                }

            else -> begrunnelseperioderIVedtaksperiode.first()
        }
    }
}

private fun Tidslinje<VedtaksperiodeMedBegrunnelser, Måned>.lagTidslinjeGrunnlagDennePeriodenForrigePeriodeOgPeriodeForrigeBehandling(
    grunnlagTidslinje: Tidslinje<BegrunnelseGrunnlagForPersonIPeriode, Måned>,
    grunnlagTidslinjePerPersonForrigeBehandling: Map<Person, Tidslinje<BegrunnelseGrunnlagForPersonIPeriode, Måned>>?,
    person: Person,
): Tidslinje<IBegrunnelseGrunnlagForPeriode, Måned> {
    val grunnlagMedForrigePeriodeTidslinje = grunnlagTidslinje.tilForrigeOgNåværendePeriodeTidslinje(this)

    val grunnlagForrigeBehandlingTidslinje = grunnlagTidslinjePerPersonForrigeBehandling?.get(person) ?: TomTidslinje()

    return this.kombinerMed(
        grunnlagMedForrigePeriodeTidslinje,
        grunnlagForrigeBehandlingTidslinje,
    ) { vedtaksPerioden, forrigeOgDennePerioden, forrigeBehandling ->
        val dennePerioden = forrigeOgDennePerioden?.denne

        if (vedtaksPerioden == null) {
            null
        } else {
            IBegrunnelseGrunnlagForPeriode.opprett(
                dennePerioden = dennePerioden ?: BegrunnelseGrunnlagForPersonIPeriode.tomPeriode(person),
                forrigePeriode = forrigeOgDennePerioden?.forrige,
                sammePeriodeForrigeBehandling = forrigeBehandling,
                periodetype = vedtaksPerioden.type,
            )
        }
    }
}

private fun VedtaksperiodeMedBegrunnelser.tilTidslinjeForAktuellPeriode(): Tidslinje<VedtaksperiodeMedBegrunnelser, Måned> =
    listOf(
        månedPeriodeAv(
            fraOgMed = this.fom?.toYearMonth(),
            tilOgMed = this.tom?.toYearMonth(),
            innhold = this,
        ),
    ).tilTidslinje()

data class ForrigeOgDennePerioden(
    val forrige: BegrunnelseGrunnlagForPersonIPeriode?,
    val denne: BegrunnelseGrunnlagForPersonIPeriode?,
)

private fun Tidslinje<BegrunnelseGrunnlagForPersonIPeriode, Måned>.tilForrigeOgNåværendePeriodeTidslinje(
    vedtaksperiodeTidslinje: Tidslinje<VedtaksperiodeMedBegrunnelser, Måned>,
): Tidslinje<ForrigeOgDennePerioden, Måned> {
    val grunnlagPerioderSplittetPåVedtaksperiode =
        kombinerMed(vedtaksperiodeTidslinje) { grunnlag, periode ->
            Pair(grunnlag, periode)
        }.perioder().mapInnhold { it?.first }

    return (
        listOf(
            månedPeriodeAv(YearMonth.now(), YearMonth.now(), null),
        ) + grunnlagPerioderSplittetPåVedtaksperiode
    ).zipWithNext { forrige, denne ->
        val innholdForrigePeriode = if (forrige.tilOgMed.tilNesteMåned() == denne.fraOgMed) forrige.innhold else null
        periodeAv(denne.fraOgMed, denne.tilOgMed, ForrigeOgDennePerioden(innholdForrigePeriode, denne.innhold))
    }.tilTidslinje()
}

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
