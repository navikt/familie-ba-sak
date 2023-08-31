package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.brevBegrunnelseProdusent

import erGjeldendeForUtgjørendeVilkår
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.brev.domene.EndretUtbetalingsperiodeDeltBostedTriggere
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityPeriodeResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.UtvidetBarnetrygdTrigger
import no.nav.familie.ba.sak.kjerne.brev.domene.tilPersonType
import no.nav.familie.ba.sak.kjerne.brev.domene.ØvrigTrigger
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TomTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.mapInnhold
import no.nav.familie.ba.sak.kjerne.tidslinje.månedPeriodeAv
import no.nav.familie.ba.sak.kjerne.tidslinje.periodeAv
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tilTidslinje
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.landkodeTilBarnetsBostedsland
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.AndelForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.BehandlingsGrunnlagForVedtaksperioder
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.EndretUtbetalingAndelForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilForskjøvedeVilkårTidslinjer
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

fun UtvidetVedtaksperiodeMedBegrunnelser.hentGyldigeBegrunnelserForPeriode(
    behandlingsGrunnlagForVedtaksperioder: BehandlingsGrunnlagForVedtaksperioder,
    behandlingsGrunnlagForVedtaksperioderForrigeBehandling: BehandlingsGrunnlagForVedtaksperioder?,
    sanityBegrunnelser: Map<Standardbegrunnelse, SanityBegrunnelse>,
    sanityEØSBegrunnelser: Map<EØSStandardbegrunnelse, SanityEØSBegrunnelse>,
): Set<IVedtakBegrunnelse> {
    val gyldigeBegrunnelserPerPerson = hentGyldigeBegrunnelserPerPerson(
        behandlingsGrunnlagForVedtaksperioder = behandlingsGrunnlagForVedtaksperioder,
        behandlingsGrunnlagForVedtaksperioderForrigeBehandling = behandlingsGrunnlagForVedtaksperioderForrigeBehandling,
        sanityBegrunnelser = sanityBegrunnelser,
        sanityEØSBegrunnelser = sanityEØSBegrunnelser,
    )

    return gyldigeBegrunnelserPerPerson.values.flatten().toSet()
}

private fun UtvidetVedtaksperiodeMedBegrunnelser.hentGyldigeBegrunnelserPerPerson(
    behandlingsGrunnlagForVedtaksperioder: BehandlingsGrunnlagForVedtaksperioder,
    behandlingsGrunnlagForVedtaksperioderForrigeBehandling: BehandlingsGrunnlagForVedtaksperioder?,
    sanityBegrunnelser: Map<Standardbegrunnelse, SanityBegrunnelse>,
    sanityEØSBegrunnelser: Map<EØSStandardbegrunnelse, SanityEØSBegrunnelse>,
): Map<Person, Set<IVedtakBegrunnelse>> {
    val avslagsbegrunnelserPerPerson = hentAvslagsbegrunnelserPerPerson(behandlingsGrunnlagForVedtaksperioder)

    if (this.type == Vedtaksperiodetype.AVSLAG) {
        return avslagsbegrunnelserPerPerson
    }

    val begrunnelseGrunnlagPerPerson =
        this.finnBegrunnelseGrunnlagPerPerson(
            behandlingsGrunnlagForVedtaksperioder,
            behandlingsGrunnlagForVedtaksperioderForrigeBehandling,
        )

    return begrunnelseGrunnlagPerPerson.mapValues { (person, begrunnelseGrunnlag) ->
        val standardBegrunnelser = hentStandardBegrunnelser(
            begrunnelseGrunnlag = begrunnelseGrunnlag,
            sanityBegrunnelser = sanityBegrunnelser,
            person = person,
            vedtaksperiode = this,
            fagsakType = behandlingsGrunnlagForVedtaksperioder.fagsakType,
        )

        val eøsBegrunnelser = hentEØSStandardBegrunnelser(
            sanityEØSBegrunnelser = sanityEØSBegrunnelser,
            begrunnelseGrunnlag = begrunnelseGrunnlag,
        )

        val avslagsbegrunnelser = avslagsbegrunnelserPerPerson[person] ?: emptySet()

        standardBegrunnelser + eøsBegrunnelser + avslagsbegrunnelser
    }
}

private fun UtvidetVedtaksperiodeMedBegrunnelser.hentAvslagsbegrunnelserPerPerson(
    behandlingsGrunnlagForVedtaksperioder: BehandlingsGrunnlagForVedtaksperioder,
): Map<Person, Set<IVedtakBegrunnelse>> {
    val tidslinjeMedVedtaksperioden = this.tilTidslinjeForAktuellPeriode()

    return behandlingsGrunnlagForVedtaksperioder.persongrunnlag.personer.associateWith { person ->
        val avslagsbegrunnelserTisdlinje =
            behandlingsGrunnlagForVedtaksperioder.personResultater.single { it.aktør == person.aktør }
                .vilkårResultater
                .filter { it.erEksplisittAvslagPåSøknad == true }
                .tilForskjøvedeVilkårTidslinjer(person.fødselsdato)
                .kombiner { vilkårResultaterIPeriode -> vilkårResultaterIPeriode.flatMap { it.standardbegrunnelser } }

        tidslinjeMedVedtaksperioden.kombinerMed(avslagsbegrunnelserTisdlinje) { h, v ->
            v.takeIf { h != null }
        }.perioder().mapNotNull { it.innhold }.flatten().toSet()
    }
}

private fun hentStandardBegrunnelser(
    begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode,
    sanityBegrunnelser: Map<Standardbegrunnelse, SanityBegrunnelse>,
    person: Person,
    vedtaksperiode: UtvidetVedtaksperiodeMedBegrunnelser,
    fagsakType: FagsakType,
): Set<Standardbegrunnelse> {
    val endretUtbetalingDennePerioden = hentEndretUtbetalingDennePerioden(begrunnelseGrunnlag)

    val relevantePeriodeResultater =
        hentResultaterForPeriode(begrunnelseGrunnlag.dennePerioden, begrunnelseGrunnlag.forrigePeriode)

    val relevantePeriodeResultaterForrigePeriode =
        hentResultaterForForrigePeriode(begrunnelseGrunnlag.forrigePeriode)

    val begrunnelserFiltrertPåPeriodetype = sanityBegrunnelser.filterValues {
        it.periodeResultat in relevantePeriodeResultater
    }

    val filtrertPåRolleOgPeriodetype = begrunnelserFiltrertPåPeriodetype.filterValues { begrunnelse ->
        begrunnelse.erGjeldendeForRolle(person, fagsakType)
    }

    val filtrertPåVilkår = filtrertPåRolleOgPeriodetype.filterValues {
        ØvrigTrigger.GJELDER_FRA_INNVILGELSESTIDSPUNKT !in it.ovrigeTriggere &&
            it.erGjeldendeForUtgjørendeVilkår(begrunnelseGrunnlag)
    }

    val filtrertPåReduksjonFraForrigeBehandling = filtrertPåRolleOgPeriodetype.filterValues {
        it.erGjeldendeForReduksjonFraForrigeBehandling(begrunnelseGrunnlag)
    }

    val filtrertPåSmåbarnstillegg = filtrertPåRolleOgPeriodetype.filterValues { begrunnelse ->
        begrunnelse.erGjeldendeForSmåbarnstillegg(begrunnelseGrunnlag)
    }

    val begrunnelserFiltrertPåPeriodetypeForrigePeriode = sanityBegrunnelser.filterValues {
        it.periodeResultat in relevantePeriodeResultaterForrigePeriode
    }

    val filtrertPåRolleOgPeriodetypeForrigePeriode =
        begrunnelserFiltrertPåPeriodetypeForrigePeriode.filterValues { begrunnelse ->
            begrunnelse.erGjeldendeForRolle(person, fagsakType)
        }

    val filtrertPåEndretUtbetaling = filtrertPåRolleOgPeriodetype.filterValues {
        it.erEndretUtbetaling(endretUtbetaling = endretUtbetalingDennePerioden)
    }

    val filtrertPåEtterEndretUtbetaling =
        filtrertPåRolleOgPeriodetypeForrigePeriode.filterValues {
            it.erEtterEndretUtbetaling(
                endretUtbetalingDennePerioden = endretUtbetalingDennePerioden,
                endretUtbetalingForrigePeriode = hentEndretUtbetalingForrigePeriode(begrunnelseGrunnlag),
            )
        }

    val filtrertPåHendelser = filtrertPåRolleOgPeriodetype.filtrerPåHendelser(
        begrunnelseGrunnlag,
        vedtaksperiode.fom,
    )

    return filtrertPåVilkår.keys.toSet() +
        filtrertPåReduksjonFraForrigeBehandling.keys.toSet() +
        filtrertPåSmåbarnstillegg.keys.toSet() +
        filtrertPåEndretUtbetaling.keys.toSet() +
        filtrertPåEtterEndretUtbetaling.keys.toSet() +
        filtrertPåHendelser.keys.toSet()
}

private fun SanityBegrunnelse.erGjeldendeForReduksjonFraForrigeBehandling(begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode): Boolean {
    if (begrunnelseGrunnlag !is BegrunnelseGrunnlagForPeriodeMedReduksjonPåTversAvBehandlinger) {
        return false
    }

    val vilkårDenneBehandlingen = begrunnelseGrunnlag.dennePerioden.vilkårResultater.map { it.vilkårType }.toSet()
    val vilkårForrigeBehandling =
        begrunnelseGrunnlag.sammePeriodeForrigeBehandling?.vilkårResultater?.map { it.vilkårType }?.toSet()
            ?: emptySet()

    val vilkårMistetSidenForrigeBehandling = vilkårForrigeBehandling - vilkårDenneBehandlingen
    val begrunnelseGjelderReduksjonPåTversAvBehandlinger =
        ØvrigTrigger.GJELDER_FRA_INNVILGELSESTIDSPUNKT in this.ovrigeTriggere
    val begrunnelseGjelderMistedeVilkår = this.vilkår.all { it in vilkårMistetSidenForrigeBehandling }

    return begrunnelseGjelderReduksjonPåTversAvBehandlinger && begrunnelseGjelderMistedeVilkår
}

private fun hentEØSStandardBegrunnelser(
    sanityEØSBegrunnelser: Map<EØSStandardbegrunnelse, SanityEØSBegrunnelse>,
    begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode,
): Set<EØSStandardbegrunnelse> {
    val relevantePeriodeResultater =
        hentResultaterForPeriode(begrunnelseGrunnlag.dennePerioden, begrunnelseGrunnlag.forrigePeriode)

    val begrunnelserFiltrertPåPeriodetype = sanityEØSBegrunnelser.filterValues {
        it.periodeResultat in relevantePeriodeResultater
    }

    val filtrertPåVilkår = begrunnelserFiltrertPåPeriodetype.filterValues {
        it.erGjeldendeForUtgjørendeVilkår(begrunnelseGrunnlag)
    }

    val filtrertPåKompetanse = begrunnelserFiltrertPåPeriodetype.filterValues { begrunnelse ->
        erEndringIKompetanse(begrunnelseGrunnlag) && begrunnelse.erLikKompetanseIPeriode(begrunnelseGrunnlag)
    }

    return filtrertPåVilkår.keys.toSet() +
        filtrertPåKompetanse.keys.toSet()
}

private fun SanityBegrunnelse.erGjeldendeForRolle(
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
    val kompetanseIPeriode = begrunnelseGrunnlag.dennePerioden.kompetanse ?: return false

    return this.annenForeldersAktivitet.contains(kompetanseIPeriode.annenForeldersAktivitet) &&
        this.barnetsBostedsland.contains(landkodeTilBarnetsBostedsland(kompetanseIPeriode.barnetsBostedsland)) &&
        this.kompetanseResultat.contains(kompetanseIPeriode.resultat)
}

fun Map<Standardbegrunnelse, SanityBegrunnelse>.filtrerPåHendelser(
    begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode,
    fomVedtaksperiode: LocalDate?,
): Map<Standardbegrunnelse, SanityBegrunnelse> = if (!begrunnelseGrunnlag.dennePerioden.erOrdinæreVilkårInnvilget()) {
    val person = begrunnelseGrunnlag.dennePerioden.person

    this.filtrerPåBarnDød(person, fomVedtaksperiode)
} else {
    val person = begrunnelseGrunnlag.dennePerioden.person

    this.filtrerPåBarn6år(person, fomVedtaksperiode) +
        this.filtrerPåSatsendring(person, begrunnelseGrunnlag.dennePerioden.andeler, fomVedtaksperiode)
}

fun Map<Standardbegrunnelse, SanityBegrunnelse>.filtrerPåBarn6år(
    person: Person,
    fomVedtaksperiode: LocalDate?,
): Map<Standardbegrunnelse, SanityBegrunnelse> {
    val blirPerson6DennePerioden = person.hentSeksårsdag().toYearMonth() == fomVedtaksperiode?.toYearMonth()

    return if (blirPerson6DennePerioden) {
        this.filterValues { it.ovrigeTriggere.contains(ØvrigTrigger.BARN_MED_6_ÅRS_DAG) }
    } else {
        emptyMap()
    }
}

fun Map<Standardbegrunnelse, SanityBegrunnelse>.filtrerPåBarnDød(
    person: Person,
    fomVedtaksperiode: LocalDate?,
): Map<Standardbegrunnelse, SanityBegrunnelse> {
    val dødsfall = person.dødsfall
    val personDødeForrigeMåned =
        dødsfall != null && dødsfall.dødsfallDato.toYearMonth().plusMonths(1) == fomVedtaksperiode?.toYearMonth()

    return if (personDødeForrigeMåned && person.type == PersonType.BARN) {
        this.filterValues { it.ovrigeTriggere.contains(ØvrigTrigger.BARN_DØD) }
    } else {
        emptyMap()
    }
}

fun Map<Standardbegrunnelse, SanityBegrunnelse>.filtrerPåSatsendring(
    person: Person,
    andeler: Iterable<AndelForVedtaksperiode>,
    fomVedtaksperiode: LocalDate?,
): Map<Standardbegrunnelse, SanityBegrunnelse> {
    val satstyperPåAndelene = andeler.map { it.type.tilSatsType(person, fomVedtaksperiode ?: TIDENES_MORGEN) }.toSet()

    val erSatsendringIPeriodenForPerson =
        satstyperPåAndelene.any { satstype ->
            SatsService.finnAlleSatserFor(satstype).any { it.gyldigFom == fomVedtaksperiode }
        }

    return if (erSatsendringIPeriodenForPerson) {
        this.filterValues { it.ovrigeTriggere.contains(ØvrigTrigger.SATSENDRING) }
    } else {
        emptyMap()
    }
}

private fun hentResultaterForForrigePeriode(
    begrunnelseGrunnlagForrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?,
) = if (begrunnelseGrunnlagForrigePeriode?.erOrdinæreVilkårInnvilget() == true &&
    begrunnelseGrunnlagForrigePeriode.erInnvilgetEtterEndretUtbetaling()
) {
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
    begrunnelseGrunnlagForPeriode: BegrunnelseGrunnlagForPersonIPeriode?,
    begrunnelseGrunnlagForrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?,
) = if (begrunnelseGrunnlagForPeriode?.erOrdinæreVilkårInnvilget() == true &&
    begrunnelseGrunnlagForPeriode.erInnvilgetEtterEndretUtbetaling()
) {
    val erReduksjonIAndel = erReduksjonIAndelMellomPerioder(
        begrunnelseGrunnlagForPeriode,
        begrunnelseGrunnlagForrigePeriode,
    )
    val erØkingIAndel = erØkningIAndelMellomPerioder(
        begrunnelseGrunnlagForPeriode,
        begrunnelseGrunnlagForrigePeriode,
    )

    val erSøker = begrunnelseGrunnlagForPeriode.person.type == PersonType.SØKER
    val erOrdinæreVilkårOppfyltIForrigePeriode =
        begrunnelseGrunnlagForrigePeriode?.erOrdinæreVilkårInnvilget() == true

    listOfNotNull(
        if (erØkingIAndel || erSøker) SanityPeriodeResultat.INNVILGET_ELLER_ØKNING else null,
        if (erReduksjonIAndel) SanityPeriodeResultat.REDUKSJON else null,
        if (!erØkingIAndel && !erReduksjonIAndel && erOrdinæreVilkårOppfyltIForrigePeriode) SanityPeriodeResultat.INGEN_ENDRING else null,
    )
} else {
    listOf(
        SanityPeriodeResultat.REDUKSJON,
        SanityPeriodeResultat.IKKE_INNVILGET,
    )
}

private fun erReduksjonIAndelMellomPerioder(
    begrunnelseGrunnlagForPeriode: BegrunnelseGrunnlagForPersonIPeriode?,
    begrunnelseGrunnlagForrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?,
): Boolean {
    val andelerForrigePeriode = begrunnelseGrunnlagForrigePeriode?.andeler ?: emptyList()
    val andelerDennePerioden = begrunnelseGrunnlagForPeriode?.andeler ?: emptyList()

    return andelerForrigePeriode.any { andelIForrigePeriode ->
        val sammeAndelDennePerioden =
            andelerDennePerioden.singleOrNull { andelIForrigePeriode.type == it.type }

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
        val sammeAndelForrigePeriode =
            andelerForrigePeriode.singleOrNull { andelIPeriode.type == it.type }

        val erAndelenTjent =
            sammeAndelForrigePeriode == null && begrunnelseGrunnlagForPeriode.erInnvilgetEtterEndretUtbetaling()
        val harAndelenGåttOppIProsent =
            sammeAndelForrigePeriode != null && andelIPeriode.prosent > sammeAndelForrigePeriode.prosent
        val erSatsenØkt = andelIPeriode.sats > (sammeAndelForrigePeriode?.sats ?: 0)

        erAndelenTjent || harAndelenGåttOppIProsent || erSatsenØkt
    }
}

private fun SanityBegrunnelse.erEtterEndretUtbetaling(
    endretUtbetalingDennePerioden: EndretUtbetalingAndelForVedtaksperiode?,
    endretUtbetalingForrigePeriode: EndretUtbetalingAndelForVedtaksperiode?,
): Boolean {
    if (!this.erEndringsårsakOgGjelderEtterEndretUtbetaling()) return false

    return this.matcherEtterEndretUtbetaling(
        endretUtbetalingDennePerioden = endretUtbetalingDennePerioden,
        endretUtbetalingForrigePeriode = endretUtbetalingForrigePeriode,
    )
}

private fun SanityBegrunnelse.matcherEtterEndretUtbetaling(
    endretUtbetalingDennePerioden: EndretUtbetalingAndelForVedtaksperiode?,
    endretUtbetalingForrigePeriode: EndretUtbetalingAndelForVedtaksperiode?,
): Boolean {
    val begrunnelseMatcherEndretUtbetalingIForrigePeriode =
        this.endringsaarsaker.all { it == endretUtbetalingForrigePeriode?.årsak }

    val begrunnelseMatcherEndretUtbetalingIDennePerioden =
        this.endringsaarsaker.all { it == endretUtbetalingDennePerioden?.årsak }

    if (!begrunnelseMatcherEndretUtbetalingIForrigePeriode || begrunnelseMatcherEndretUtbetalingIDennePerioden) return false

    return endretUtbetalingForrigePeriode?.årsak != Årsak.DELT_BOSTED || this.erDeltBostedUtbetalingstype(
        endretUtbetalingForrigePeriode,
    )
}

private fun SanityBegrunnelse.erEndringsårsakOgGjelderEtterEndretUtbetaling() =
    this.endringsaarsaker.isNotEmpty() && this.gjelderEtterEndretUtbetaling()

private fun SanityBegrunnelse.erEndretUtbetaling(
    endretUtbetaling: EndretUtbetalingAndelForVedtaksperiode?,
): Boolean {
    return this.gjelderEndretUtbetaling() && this.erLikEndretUtbetalingIPeriode(endretUtbetaling)
}

private fun SanityBegrunnelse.gjelderEndretUtbetaling() =
    this.endringsaarsaker.isNotEmpty() && !this.gjelderEtterEndretUtbetaling()

private fun SanityBegrunnelse.erLikEndretUtbetalingIPeriode(
    endretUtbetaling: EndretUtbetalingAndelForVedtaksperiode?,
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
    endretUtbetaling: EndretUtbetalingAndelForVedtaksperiode,
): Boolean {
    val inneholderAndelSomSkalUtbetales = endretUtbetaling.prosent != BigDecimal.ZERO

    return when (this.endretUtbetalingsperiodeDeltBostedUtbetalingTrigger) {
        EndretUtbetalingsperiodeDeltBostedTriggere.UTBETALING_IKKE_RELEVANT -> true
        EndretUtbetalingsperiodeDeltBostedTriggere.SKAL_UTBETALES -> inneholderAndelSomSkalUtbetales
        EndretUtbetalingsperiodeDeltBostedTriggere.SKAL_IKKE_UTBETALES -> !inneholderAndelSomSkalUtbetales
        null -> true
    }
}

private fun hentEndretUtbetalingDennePerioden(begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode) =
    begrunnelseGrunnlag.dennePerioden.endretUtbetalingAndel
        .takeIf { begrunnelseGrunnlag.dennePerioden.erOrdinæreVilkårInnvilget() }

private fun hentEndretUtbetalingForrigePeriode(begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode) =
    begrunnelseGrunnlag.forrigePeriode?.endretUtbetalingAndel
        .takeIf { begrunnelseGrunnlag.forrigePeriode?.erOrdinæreVilkårInnvilget() == true }

private fun UtvidetVedtaksperiodeMedBegrunnelser.finnBegrunnelseGrunnlagPerPerson(
    behandlingsGrunnlagForVedtaksperioder: BehandlingsGrunnlagForVedtaksperioder,
    behandlingsGrunnlagForVedtaksperioderForrigeBehandling: BehandlingsGrunnlagForVedtaksperioder?,
): Map<Person, IBegrunnelseGrunnlagForPeriode> {
    val tidslinjeMedVedtaksperioden = this.tilTidslinjeForAktuellPeriode()

    val begrunnelsegrunnlagTidslinjerPerPerson =
        behandlingsGrunnlagForVedtaksperioder.lagBegrunnelseGrunnlagTidslinjer()

    val grunnlagTidslinjePerPersonForrigeBehandling =
        behandlingsGrunnlagForVedtaksperioderForrigeBehandling?.lagBegrunnelseGrunnlagTidslinjer()

    return begrunnelsegrunnlagTidslinjerPerPerson.mapValues { (person, grunnlagTidslinje) ->
        val grunnlagMedForrigePeriodeOgBehandlingTidslinje =
            tidslinjeMedVedtaksperioden.lagTidslinjeGrunnlagDennePeriodenForrigePeriodeOgPeriodeForrigeBehandling(
                grunnlagTidslinje,
                grunnlagTidslinjePerPersonForrigeBehandling,
                person,
            )

        grunnlagMedForrigePeriodeOgBehandlingTidslinje.perioder().mapNotNull { it.innhold }.single()
    }
}

private fun Tidslinje<UtvidetVedtaksperiodeMedBegrunnelser, Måned>.lagTidslinjeGrunnlagDennePeriodenForrigePeriodeOgPeriodeForrigeBehandling(
    grunnlagTidslinje: Tidslinje<BegrunnelseGrunnlagForPersonIPeriode, Måned>,
    grunnlagTidslinjePerPersonForrigeBehandling: Map<Person, Tidslinje<BegrunnelseGrunnlagForPersonIPeriode, Måned>>?,
    person: Person,
): Tidslinje<IBegrunnelseGrunnlagForPeriode, Måned> {
    val grunnlagMedForrigePeriodeTidslinje = grunnlagTidslinje.tilForrigeOgNåværendePeriodeTidslinje(this)

    val grunnlagForrigeBehandlingTidslinje =
        grunnlagTidslinjePerPersonForrigeBehandling?.get(person) ?: TomTidslinje()

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

private fun UtvidetVedtaksperiodeMedBegrunnelser.tilTidslinjeForAktuellPeriode(): Tidslinje<UtvidetVedtaksperiodeMedBegrunnelser, Måned> {
    return listOf(
        månedPeriodeAv(
            fraOgMed = this.fom?.toYearMonth(),
            tilOgMed = this.tom?.toYearMonth(),
            innhold = this,
        ),
    ).tilTidslinje()
}

data class ForrigeOgDennePerioden(
    val forrige: BegrunnelseGrunnlagForPersonIPeriode?,
    val denne: BegrunnelseGrunnlagForPersonIPeriode?,
)

private fun Tidslinje<BegrunnelseGrunnlagForPersonIPeriode, Måned>.tilForrigeOgNåværendePeriodeTidslinje(
    vedtaksperiodeTidslinje: Tidslinje<UtvidetVedtaksperiodeMedBegrunnelser, Måned>,
): Tidslinje<ForrigeOgDennePerioden, Måned> {
    val grunnlagPerioderSplittetPåVedtaksperiode = kombinerMed(vedtaksperiodeTidslinje) { grunnlag, periode ->
        Pair(grunnlag, periode)
    }.perioder().mapInnhold { it?.first }

    return (
        listOf(
            månedPeriodeAv(YearMonth.now(), YearMonth.now(), null),
        ) + grunnlagPerioderSplittetPåVedtaksperiode
        ).zipWithNext { forrige, denne ->
        periodeAv(denne.fraOgMed, denne.tilOgMed, ForrigeOgDennePerioden(forrige.innhold, denne.innhold))
    }.tilTidslinje()
}

private fun SanityBegrunnelse.erGjeldendeForSmåbarnstillegg(
    begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode,
): Boolean {
    val erSmåbarnstilleggForrigePeriode =
        begrunnelseGrunnlag.forrigePeriode?.andeler?.any { it.type == YtelseType.SMÅBARNSTILLEGG } == true
    val erSmåbarnstilleggDennePerioden =
        begrunnelseGrunnlag.dennePerioden.andeler.any { it.type == YtelseType.SMÅBARNSTILLEGG }

    val begrunnelseGjelderSmåbarnstillegg =
        UtvidetBarnetrygdTrigger.SMÅBARNSTILLEGG in utvidetBarnetrygdTriggere

    val begrunnelseMatcherPeriodeResultat =
        this.matcherPerioderesultat(erSmåbarnstilleggForrigePeriode, erSmåbarnstilleggDennePerioden)

    val erEndringISmåbarnstillegg = erSmåbarnstilleggForrigePeriode != erSmåbarnstilleggDennePerioden

    return begrunnelseGjelderSmåbarnstillegg && begrunnelseMatcherPeriodeResultat && erEndringISmåbarnstillegg
}

private fun SanityBegrunnelse.matcherPerioderesultat(
    erSmåbarnstilleggForrigePeriode: Boolean,
    erSmåbarnstilleggDennePerioden: Boolean,
): Boolean {
    val erReduksjon = erSmåbarnstilleggForrigePeriode && !erSmåbarnstilleggDennePerioden
    val erØkning = !erSmåbarnstilleggForrigePeriode && erSmåbarnstilleggDennePerioden

    val erBegrunnelseReduksjon = periodeResultat == SanityPeriodeResultat.REDUKSJON
    val erBegrunnelseØkning = periodeResultat == SanityPeriodeResultat.INNVILGET_ELLER_ØKNING

    val reduksjonMatcher = erReduksjon == erBegrunnelseReduksjon
    val økningMatcher = erØkning == erBegrunnelseØkning
    return reduksjonMatcher && økningMatcher
}

private fun erEndringIKompetanse(begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode) =
    begrunnelseGrunnlag.dennePerioden.kompetanse !=
        begrunnelseGrunnlag.forrigePeriode?.kompetanse
