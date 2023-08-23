package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.brevBegrunnelseProdusent

import filtrerPåVilkår
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.brev.domene.EndretUtbetalingsperiodeDeltBostedTriggere
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityVedtakResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.ØvrigTrigger
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TomTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.mapInnhold
import no.nav.familie.ba.sak.kjerne.tidslinje.månedPeriodeAv
import no.nav.familie.ba.sak.kjerne.tidslinje.periodeAv
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tilTidslinje
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.AndelForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.BehandlingsGrunnlagForVedtaksperioder
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.EndretUtbetalingAndelForVedtaksperiode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

fun UtvidetVedtaksperiodeMedBegrunnelser.hentGyldigeBegrunnelserForPeriode(
    behandlingsGrunnlagForVedtaksperioder: BehandlingsGrunnlagForVedtaksperioder,
    behandlingsGrunnlagForVedtaksperioderForrigeBehandling: BehandlingsGrunnlagForVedtaksperioder?,
    sanityBegrunnelser: Map<Standardbegrunnelse, SanityBegrunnelse>,
    sanityEØSBegrunnelser: Map<EØSStandardbegrunnelse, SanityEØSBegrunnelse>,
    behandlingUnderkategori: BehandlingUnderkategori,
): Set<Standardbegrunnelse> {
    val gyldigeBegrunnelserPerPerson = hentGyldigeBegrunnelserPerPerson(
        behandlingsGrunnlagForVedtaksperioder = behandlingsGrunnlagForVedtaksperioder,
        behandlingsGrunnlagForVedtaksperioderForrigeBehandling = behandlingsGrunnlagForVedtaksperioderForrigeBehandling,
        behandlingUnderkategori = behandlingUnderkategori,
        sanityBegrunnelser = sanityBegrunnelser,
        sanityEØSBegrunnelser = sanityEØSBegrunnelser,
    )

    return gyldigeBegrunnelserPerPerson.values.flatten().toSet()
}

private fun UtvidetVedtaksperiodeMedBegrunnelser.hentGyldigeBegrunnelserPerPerson(
    behandlingsGrunnlagForVedtaksperioder: BehandlingsGrunnlagForVedtaksperioder,
    behandlingsGrunnlagForVedtaksperioderForrigeBehandling: BehandlingsGrunnlagForVedtaksperioder?,
    behandlingUnderkategori: BehandlingUnderkategori,
    sanityBegrunnelser: Map<Standardbegrunnelse, SanityBegrunnelse>,
    sanityEØSBegrunnelser: Map<EØSStandardbegrunnelse, SanityEØSBegrunnelse>,
): Map<Person, Set<Standardbegrunnelse>> {
    val begrunnelseGrunnlagPerPerson =
        this.finnBegrunnelseGrunnlagPerPerson(
            behandlingsGrunnlagForVedtaksperioder,
            behandlingsGrunnlagForVedtaksperioderForrigeBehandling,
        )

    return begrunnelseGrunnlagPerPerson.mapValues { (person, begrunnelseGrunnlag) ->
        val endretUtbetalingDennePerioden = hentEndretUtbetalingDennePerioden(begrunnelseGrunnlag)

        val begrunnelserFiltrertPåPeriodetype = sanityBegrunnelser.filtrerPåPeriodetype(begrunnelseGrunnlag)
        val begrunnelserFiltrertPåPeriodetypeForrigePeriode =
            sanityBegrunnelser.filtrerPåPeriodetypeForrigePeriode(begrunnelseGrunnlag)

        val filtrertPåVilkår = begrunnelserFiltrertPåPeriodetype.filtrerPåVilkår(
            begrunnelseGrunnlag = begrunnelseGrunnlag,
            person = person,
            behandlingUnderkategori = behandlingUnderkategori,
        )

        val filtrertPåEndretUtbetaling = begrunnelserFiltrertPåPeriodetype.filtrerPåEndretUtbetaling(
            endretUtbetaling = endretUtbetalingDennePerioden,
        )

        val filtrertPåEtterEndretUtbetaling =
            begrunnelserFiltrertPåPeriodetypeForrigePeriode.filtrerPåEtterEndretUtbetaling(
                endretUtbetalingDennePerioden = endretUtbetalingDennePerioden,
                endretUtbetalingForrigePeriode = hentEndretUtbetalingForrigePeriode(begrunnelseGrunnlag),
            )

        val filtrertPåHendelser = begrunnelserFiltrertPåPeriodetype.filtrerPåHendelser(
            begrunnelseGrunnlag,
            this.fom,
        )

        filtrertPåVilkår.keys.toSet() +
            filtrertPåEndretUtbetaling.keys.toSet() +
            filtrertPåEtterEndretUtbetaling.keys.toSet() +
            filtrertPåHendelser.keys.toSet()
    }
}

fun Map<Standardbegrunnelse, SanityBegrunnelse>.filtrerPåHendelser(
    begrunnelseGrunnlag: BegrunnelseGrunnlagForPeriode,
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
        this.filterValues { it.ovrigeTriggere?.contains(ØvrigTrigger.BARN_MED_6_ÅRS_DAG) == true }
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
        this.filterValues { it.ovrigeTriggere?.contains(ØvrigTrigger.BARN_DØD) == true }
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
        this.filterValues { it.ovrigeTriggere?.contains(ØvrigTrigger.SATSENDRING) == true }
    } else {
        emptyMap()
    }
}

private fun Map<Standardbegrunnelse, SanityBegrunnelse>.filtrerPåPeriodetype(
    begrunnelseGrunnlag: BegrunnelseGrunnlagForPeriode,
) = this.filterValues {
    val dennePerioden = begrunnelseGrunnlag.dennePerioden

    if (dennePerioden.erOrdinæreVilkårInnvilget() && dennePerioden.erInnvilgetEtterEndretUtbetaling()) {
        it.resultat in listOf(
            SanityVedtakResultat.INNVILGET_ELLER_ØKNING,
            SanityVedtakResultat.REDUKSJON,
        )
    } else {
        it.resultat in listOf(
            SanityVedtakResultat.REDUKSJON,
            SanityVedtakResultat.IKKE_INNVILGET,
        )
    }
}

private fun Map<Standardbegrunnelse, SanityBegrunnelse>.filtrerPåPeriodetypeForrigePeriode(
    begrunnelseGrunnlag: BegrunnelseGrunnlagForPeriode,
) = this.filterValues {
    val forrigePeriode = begrunnelseGrunnlag.forrigePeriode
    if (forrigePeriode?.erOrdinæreVilkårInnvilget() == true && forrigePeriode.erInnvilgetEtterEndretUtbetaling()) {
        it.resultat in listOf(
            SanityVedtakResultat.INNVILGET_ELLER_ØKNING,
            SanityVedtakResultat.REDUKSJON,
        )
    } else {
        it.resultat in listOf(
            SanityVedtakResultat.REDUKSJON,
            SanityVedtakResultat.IKKE_INNVILGET,
        )
    }
}

private fun Map<Standardbegrunnelse, SanityBegrunnelse>.filtrerPåEtterEndretUtbetaling(
    endretUtbetalingDennePerioden: EndretUtbetalingAndelForVedtaksperiode?,
    endretUtbetalingForrigePeriode: EndretUtbetalingAndelForVedtaksperiode?,
): Map<Standardbegrunnelse, SanityBegrunnelse> {
    val begrunnelserRelevanteForFilter = this.filterValues { it.erEndringsårsakOgGjelderEtterEndretUtbetaling() }

    return begrunnelserRelevanteForFilter.filtrerPåOmMatcherEtterEndretUtbetaling(
        endretUtbetalingDennePerioden = endretUtbetalingDennePerioden,
        endretUtbetalingForrigePeriode = endretUtbetalingForrigePeriode,
    )
}

private fun Map<Standardbegrunnelse, SanityBegrunnelse>.filtrerPåOmMatcherEtterEndretUtbetaling(
    endretUtbetalingDennePerioden: EndretUtbetalingAndelForVedtaksperiode?,
    endretUtbetalingForrigePeriode: EndretUtbetalingAndelForVedtaksperiode?,
): Map<Standardbegrunnelse, SanityBegrunnelse> {
    val filtrerteBegrunnelser = this
        .filterValues { sanityBegrunnelse ->
            val begrunnelseMatcherEndretUtbetalingIForrigePeriode =
                sanityBegrunnelse.endringsaarsaker?.all { it == endretUtbetalingForrigePeriode?.årsak } == true

            val begrunnelseMatcherEndretUtbetalingIDennePerioden =
                sanityBegrunnelse.endringsaarsaker?.all { it == endretUtbetalingDennePerioden?.årsak } == true

            begrunnelseMatcherEndretUtbetalingIForrigePeriode && !begrunnelseMatcherEndretUtbetalingIDennePerioden
        }

    return if (endretUtbetalingForrigePeriode?.årsak == Årsak.DELT_BOSTED) {
        filtrerteBegrunnelser.filtrerPåDeltBostedUtbetalingstype(endretUtbetalingForrigePeriode)
    } else {
        filtrerteBegrunnelser
    }
}

private fun SanityBegrunnelse.erEndringsårsakOgGjelderEtterEndretUtbetaling() =
    !this.endringsaarsaker.isNullOrEmpty() && this.gjelderEtterEndretUtbetaling()

private fun Map<Standardbegrunnelse, SanityBegrunnelse>.filtrerPåEndretUtbetaling(
    endretUtbetaling: EndretUtbetalingAndelForVedtaksperiode?,
): Map<Standardbegrunnelse, SanityBegrunnelse> {
    val begrunnelserRelevanteForFilter = this.filterValues { it.gjelderEndretUtbetaling() }

    return begrunnelserRelevanteForFilter.filtrerPåOmMatcherEndretUtbetaling(endretUtbetaling)
}

private fun SanityBegrunnelse.gjelderEndretUtbetaling() =
    !this.endringsaarsaker.isNullOrEmpty() && !this.gjelderEtterEndretUtbetaling()

private fun Map<Standardbegrunnelse, SanityBegrunnelse>.filtrerPåOmMatcherEndretUtbetaling(
    endretUtbetaling: EndretUtbetalingAndelForVedtaksperiode?,
): Map<Standardbegrunnelse, SanityBegrunnelse> {
    val filtrerteBegrunnelser = this.filterValues { sanityBegrunnelse ->
        endretUtbetaling != null && sanityBegrunnelse.endringsaarsaker!!.all { it == endretUtbetaling.årsak }
    }

    return if (endretUtbetaling?.årsak == Årsak.DELT_BOSTED) {
        filtrerteBegrunnelser.filtrerPåDeltBostedUtbetalingstype(endretUtbetaling)
    } else {
        filtrerteBegrunnelser
    }
}

private fun Map<Standardbegrunnelse, SanityBegrunnelse>.filtrerPåDeltBostedUtbetalingstype(
    endretUtbetaling: EndretUtbetalingAndelForVedtaksperiode,
) = filter { (_, sanityBegrunnelse) ->
    val inneholderAndelSomSkalUtbetales = endretUtbetaling.prosent != BigDecimal.ZERO

    when (sanityBegrunnelse.endretUtbetalingsperiodeDeltBostedUtbetalingTrigger) {
        EndretUtbetalingsperiodeDeltBostedTriggere.UTBETALING_IKKE_RELEVANT -> true
        EndretUtbetalingsperiodeDeltBostedTriggere.SKAL_UTBETALES -> inneholderAndelSomSkalUtbetales
        EndretUtbetalingsperiodeDeltBostedTriggere.SKAL_IKKE_UTBETALES -> !inneholderAndelSomSkalUtbetales
        null -> true
    }
}

private fun hentEndretUtbetalingDennePerioden(begrunnelseGrunnlag: BegrunnelseGrunnlagForPeriode) =
    if (
        begrunnelseGrunnlag.dennePerioden.erOrdinæreVilkårInnvilget()
    ) {
        begrunnelseGrunnlag.dennePerioden.endretUtbetalingAndel
    } else {
        null
    }

private fun hentEndretUtbetalingForrigePeriode(begrunnelseGrunnlag: BegrunnelseGrunnlagForPeriode) =
    if (
        begrunnelseGrunnlag.forrigePeriode?.erOrdinæreVilkårInnvilget() == true
    ) {
        begrunnelseGrunnlag.forrigePeriode.endretUtbetalingAndel
    } else {
        null
    }

private fun UtvidetVedtaksperiodeMedBegrunnelser.finnBegrunnelseGrunnlagPerPerson(
    behandlingsGrunnlagForVedtaksperioder: BehandlingsGrunnlagForVedtaksperioder,
    behandlingsGrunnlagForVedtaksperioderForrigeBehandling: BehandlingsGrunnlagForVedtaksperioder?,
): Map<Person, BegrunnelseGrunnlagForPeriode> {
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
): Tidslinje<BegrunnelseGrunnlagForPeriode, Måned> {
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
        } else if (dennePerioden == null) {
            throw Feil("Ingen data på person i perioden ${vedtaksPerioden.fom} - ${vedtaksPerioden.tom}")
        } else {
            BegrunnelseGrunnlagForPeriode(
                dennePerioden = dennePerioden,
                forrigePeriode = forrigeOgDennePerioden?.forrige,
                sammePeriodeForrigeBehandling = forrigeBehandling,
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
