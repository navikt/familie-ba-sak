package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.brevBegrunnelseProdusent

import filtrerPåVilkår
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
import no.nav.familie.ba.sak.kjerne.tidslinje.månedPeriodeAv
import no.nav.familie.ba.sak.kjerne.tidslinje.periodeAv
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tilTidslinje
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.AktørOgRolleBegrunnelseGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.AndelForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.EndretUtbetalingAndelForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.GrunnlagForPerson
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.GrunnlagForPersonTidslinjerSplittetPåOverlappendeGenerelleAvslag
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.GrunnlagForPersonVilkårInnvilget
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.GrunnlagForVedtaksperioder
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

fun UtvidetVedtaksperiodeMedBegrunnelser.hentGyldigeBegrunnelserForPeriode(
    grunnlagForVedtaksperioder: GrunnlagForVedtaksperioder,
    grunnlagForVedtaksperioderForrigeBehandling: GrunnlagForVedtaksperioder?,
    sanityBegrunnelser: Map<Standardbegrunnelse, SanityBegrunnelse>,
    sanityEØSBegrunnelser: Map<EØSStandardbegrunnelse, SanityEØSBegrunnelse>,
    behandlingUnderkategori: BehandlingUnderkategori,
): Set<Standardbegrunnelse> {
    val gyldigeBegrunnelserPerPerson = hentGyldigeBegrunnelserPerPerson(
        grunnlagForVedtaksperioder = grunnlagForVedtaksperioder,
        grunnlagForVedtaksperioderForrigeBehandling = grunnlagForVedtaksperioderForrigeBehandling,
        behandlingUnderkategori = behandlingUnderkategori,
        sanityBegrunnelser = sanityBegrunnelser,
        sanityEØSBegrunnelser = sanityEØSBegrunnelser,
    )

    return gyldigeBegrunnelserPerPerson.values.flatten().toSet()
}

private fun UtvidetVedtaksperiodeMedBegrunnelser.hentGyldigeBegrunnelserPerPerson(
    grunnlagForVedtaksperioder: GrunnlagForVedtaksperioder,
    grunnlagForVedtaksperioderForrigeBehandling: GrunnlagForVedtaksperioder?,
    behandlingUnderkategori: BehandlingUnderkategori,
    sanityBegrunnelser: Map<Standardbegrunnelse, SanityBegrunnelse>,
    sanityEØSBegrunnelser: Map<EØSStandardbegrunnelse, SanityEØSBegrunnelse>,
): Map<AktørOgRolleBegrunnelseGrunnlag, Set<Standardbegrunnelse>> {
    val begrunnelseGrunnlagPerPerson =
        this.finnBegrunnelseGrunnlagPerPerson(
            grunnlagForVedtaksperioder,
            grunnlagForVedtaksperioderForrigeBehandling,
        )

    return begrunnelseGrunnlagPerPerson.mapValues { (aktørOgRolleForVedtaksgrunnlag, begrunnelseGrunnlag) ->
        val endretUtbetalingDennePerioden = hentEndretUtbetalingDennePerioden(begrunnelseGrunnlag)

        val begrunnelserFiltrertPåPeriodetype = sanityBegrunnelser.filtrerPåPeriodetype(begrunnelseGrunnlag)
        val begrunnelserFiltrertPåPeriodetypeForrigePeriode =
            sanityBegrunnelser.filtrerPåPeriodetypeForrigePeriode(begrunnelseGrunnlag)

        val filtrertPåVilkår = begrunnelserFiltrertPåPeriodetype.filtrerPåVilkår(
            begrunnelseGrunnlag = begrunnelseGrunnlag,
            aktørOgRolle = aktørOgRolleForVedtaksgrunnlag,
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

        val filtrertPåHendelse = begrunnelserFiltrertPåPeriodetype.filtrerPåHendelser(
            begrunnelseGrunnlag,
            this.fom,
        )

        filtrertPåVilkår.keys.toSet() +
            filtrertPåEndretUtbetaling.keys.toSet() +
            filtrertPåEtterEndretUtbetaling.keys.toSet() +
            filtrertPåHendelse.keys.toSet()
    }
}

fun Map<Standardbegrunnelse, SanityBegrunnelse>.filtrerPåHendelser(
    begrunnelseGrunnlag: BegrunnelseGrunnlag,
    fomVedtaksperiode: LocalDate?,
): Map<Standardbegrunnelse, SanityBegrunnelse> {
    return if (begrunnelseGrunnlag !is BegrunnelseGrunnlagMedVerdiIDennePerioden) {
        emptyMap()
    } else if (begrunnelseGrunnlag.grunnlagForVedtaksperiode !is GrunnlagForPersonVilkårInnvilget) {
        val person = begrunnelseGrunnlag.grunnlagForVedtaksperiode.person

        this.filtrerPåBarnDød(person, fomVedtaksperiode)
    } else {
        val person = begrunnelseGrunnlag.grunnlagForVedtaksperiode.person

        this.filtrerPåBarn6år(person, fomVedtaksperiode) +
            this.filtrerPåSatsendring(person, begrunnelseGrunnlag.grunnlagForVedtaksperiode.andeler, fomVedtaksperiode)
    }
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
    begrunnelseGrunnlag: BegrunnelseGrunnlag,
) = this.filterValues {
    val begrunnelseGjelderFraInnvilgelsestidspunkt =
        it.ovrigeTriggere?.contains(ØvrigTrigger.GJELDER_FRA_INNVILGELSESTIDSPUNKT) == true

    when (begrunnelseGrunnlag) {
        is BegrunnelseGrunnlagMedVerdiIDennePerioden -> {
            val resultater = if (begrunnelseGrunnlag.grunnlagForVedtaksperiode.erInnvilget()) {
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

            !begrunnelseGjelderFraInnvilgelsestidspunkt && resultater
        }

        is BegrunnelseGrunnlagIngenVerdiIDennePerioden -> {
            begrunnelseGrunnlag.erInnvilgetForrigeBehandling &&
                it.resultat in listOf(SanityVedtakResultat.REDUKSJON) &&
                begrunnelseGjelderFraInnvilgelsestidspunkt
        }
    }
}

private fun Map<Standardbegrunnelse, SanityBegrunnelse>.filtrerPåPeriodetypeForrigePeriode(
    begrunnelseGrunnlag: BegrunnelseGrunnlag,
) = this.filterValues {
    when (begrunnelseGrunnlag) {
        is BegrunnelseGrunnlagMedVerdiIDennePerioden -> {
            when (begrunnelseGrunnlag.grunnlagForForrigeVedtaksperiode?.erInnvilget()) {
                true -> {
                    it.resultat in listOf(
                        SanityVedtakResultat.INNVILGET_ELLER_ØKNING,
                        SanityVedtakResultat.REDUKSJON,
                    )
                }

                false -> {
                    it.resultat in listOf(
                        SanityVedtakResultat.REDUKSJON,
                        SanityVedtakResultat.IKKE_INNVILGET,
                    )
                }

                null -> it.resultat in listOf(SanityVedtakResultat.REDUKSJON)
            } && it.ovrigeTriggere?.contains(ØvrigTrigger.GJELDER_FRA_INNVILGELSESTIDSPUNKT) != true
        }

        is BegrunnelseGrunnlagIngenVerdiIDennePerioden -> {
            it.resultat in listOf(SanityVedtakResultat.REDUKSJON) &&
                it.ovrigeTriggere?.contains(ØvrigTrigger.GJELDER_FRA_INNVILGELSESTIDSPUNKT) == true
        }
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

private fun hentEndretUtbetalingDennePerioden(begrunnelseGrunnlag: BegrunnelseGrunnlag) =
    if (
        begrunnelseGrunnlag is BegrunnelseGrunnlagMedVerdiIDennePerioden &&
        begrunnelseGrunnlag.grunnlagForVedtaksperiode is GrunnlagForPersonVilkårInnvilget
    ) {
        begrunnelseGrunnlag.grunnlagForVedtaksperiode.endretUtbetalingAndel
    } else {
        null
    }

private fun hentEndretUtbetalingForrigePeriode(begrunnelseGrunnlag: BegrunnelseGrunnlag) =
    if (
        begrunnelseGrunnlag is BegrunnelseGrunnlagMedVerdiIDennePerioden &&
        begrunnelseGrunnlag.grunnlagForForrigeVedtaksperiode is GrunnlagForPersonVilkårInnvilget
    ) {
        begrunnelseGrunnlag.grunnlagForForrigeVedtaksperiode.endretUtbetalingAndel
    } else {
        null
    }

private fun UtvidetVedtaksperiodeMedBegrunnelser.finnBegrunnelseGrunnlagPerPerson(
    grunnlagForVedtaksperioder: GrunnlagForVedtaksperioder,
    grunnlagForVedtaksperioderForrigeBehandling: GrunnlagForVedtaksperioder?,
): Map<AktørOgRolleBegrunnelseGrunnlag, BegrunnelseGrunnlag> {
    val tidslinjeMedVedtaksperioden = this.tilTidslinjeForAktuellPeriode()

    val grunnlagTidslinjePerPerson = grunnlagForVedtaksperioder.utledGrunnlagTidslinjePerPerson()
        .mapValues { it.value.copy(grunnlagForPerson = it.value.grunnlagForPerson.fjernOverflødigePerioderPåSlutten()) }

    val grunnlagTidslinjePerPersonForrigeBehandling =
        grunnlagForVedtaksperioderForrigeBehandling?.utledGrunnlagTidslinjePerPerson()

    val grunnlagPerPerson =
        grunnlagTidslinjePerPerson.mapValues { (aktørOgRolleForVedtaksgrunnlag, grunnlagTidslinje) ->
            val grunnlagMedForrigePeriodeOgBehandlingTidslinje =
                tidslinjeMedVedtaksperioden.lagTidslinjeGrunnlagDennePeriodenForrigePeriodeOgPeriodeForrigeBehandling(
                    grunnlagTidslinje,
                    grunnlagTidslinjePerPersonForrigeBehandling,
                    aktørOgRolleForVedtaksgrunnlag,
                )

            grunnlagMedForrigePeriodeOgBehandlingTidslinje.perioder().mapNotNull { it.innhold }.single()
        }

    return grunnlagPerPerson.flyttSøkersOrdinæreVilkårFraBarnaTilSøker()
}

/**
 * Søker sine ordinære vilkår er knyttet til barnet når vi lager vedtaksperiodene fordi de er med på å bestemme om
 * barnet får andeler.
 * Siden vi gjenbruker dataklassene fra generering av vedtaksperiodene må vi flytte søker sine ordinære vilkår tilbake
 * til søker fra barna.
 */
private fun Map<AktørOgRolleBegrunnelseGrunnlag, BegrunnelseGrunnlag>.flyttSøkersOrdinæreVilkårFraBarnaTilSøker(): Map<AktørOgRolleBegrunnelseGrunnlag, BegrunnelseGrunnlag> {
    val førsteBarnMedVilkårIPerioden = toList()
        .filter { it.first.rolleBegrunnelseGrunnlag == PersonType.BARN }
        .map { it.second }
        .filterIsInstance<BegrunnelseGrunnlagMedVerdiIDennePerioden>()
        .firstOrNull()

    val søkerGrunnlag = toList()
        .single { it.first.rolleBegrunnelseGrunnlag == PersonType.SØKER }

    val søkersOrdinæreVilkårDennePerioden =
        førsteBarnMedVilkårIPerioden?.grunnlagForVedtaksperiode?.vilkårResultaterForVedtaksperiode?.filter {
            it.aktørId == søkerGrunnlag.first.aktør.aktørId
        } ?: emptyList()

    val søkersOrdinæreVilkårForrigePeriode =
        førsteBarnMedVilkårIPerioden?.grunnlagForForrigeVedtaksperiode?.vilkårResultaterForVedtaksperiode?.filter {
            it.aktørId == søkerGrunnlag.first.aktør.aktørId
        } ?: emptyList()

    return this.mapValues { (aktørOgRolle, begrunnelseGrunnlag) ->
        if (aktørOgRolle == søkerGrunnlag.first && begrunnelseGrunnlag is BegrunnelseGrunnlagMedVerdiIDennePerioden) {
            begrunnelseGrunnlag.copy(
                grunnlagForVedtaksperiode = begrunnelseGrunnlag.grunnlagForVedtaksperiode.kopier(
                    vilkårResultaterForVedtaksperiode = begrunnelseGrunnlag.grunnlagForVedtaksperiode.vilkårResultaterForVedtaksperiode + søkersOrdinæreVilkårDennePerioden,
                ),
                grunnlagForForrigeVedtaksperiode = begrunnelseGrunnlag.grunnlagForForrigeVedtaksperiode?.kopier(
                    vilkårResultaterForVedtaksperiode = begrunnelseGrunnlag.grunnlagForForrigeVedtaksperiode.vilkårResultaterForVedtaksperiode + søkersOrdinæreVilkårForrigePeriode,
                ),
            )
        } else if (begrunnelseGrunnlag is BegrunnelseGrunnlagMedVerdiIDennePerioden) {
            begrunnelseGrunnlag.copy(
                grunnlagForVedtaksperiode = begrunnelseGrunnlag.grunnlagForVedtaksperiode.kopier(
                    vilkårResultaterForVedtaksperiode = begrunnelseGrunnlag.grunnlagForVedtaksperiode.vilkårResultaterForVedtaksperiode.filter { it.aktørId == aktørOgRolle.aktør.aktørId },
                ),
                grunnlagForForrigeVedtaksperiode = begrunnelseGrunnlag.grunnlagForForrigeVedtaksperiode?.kopier(
                    vilkårResultaterForVedtaksperiode = begrunnelseGrunnlag.grunnlagForForrigeVedtaksperiode.vilkårResultaterForVedtaksperiode.filter { it.aktørId == aktørOgRolle.aktør.aktørId },
                ),
            )
        } else {
            begrunnelseGrunnlag
        }
    }
}

private fun Tidslinje<UtvidetVedtaksperiodeMedBegrunnelser, Måned>.lagTidslinjeGrunnlagDennePeriodenForrigePeriodeOgPeriodeForrigeBehandling(
    grunnlagTidslinje: GrunnlagForPersonTidslinjerSplittetPåOverlappendeGenerelleAvslag,
    grunnlagTidslinjePerPersonForrigeBehandling: Map<AktørOgRolleBegrunnelseGrunnlag, GrunnlagForPersonTidslinjerSplittetPåOverlappendeGenerelleAvslag>?,
    aktørOgRolleForVedtaksgrunnlag: AktørOgRolleBegrunnelseGrunnlag,
): Tidslinje<BegrunnelseGrunnlag, Måned> {
    val grunnlagMedForrigePeriodeTidslinje =
        grunnlagTidslinje.grunnlagForPerson.tilForrigeOgNåværendePeriodeTidslinje()

    val grunnlagForrigeBehandlingTidslinje =
        grunnlagTidslinjePerPersonForrigeBehandling?.get(aktørOgRolleForVedtaksgrunnlag)?.grunnlagForPerson
            ?: TomTidslinje()

    return this.kombinerMed(
        grunnlagMedForrigePeriodeTidslinje,
        grunnlagForrigeBehandlingTidslinje,
    ) { vedtaksPerioden, forrigeOgDennePerioden, forrigeBehandling ->
        if (vedtaksPerioden == null) {
            null
        } else {
            lagBegrunnelseGrunnlag(
                dennePerioden = forrigeOgDennePerioden?.denne,
                forrigePeriode = forrigeOgDennePerioden?.forrige,
                sammePeriodeForrigeBehandling = forrigeBehandling,
            )
        }
    }
}

private fun Tidslinje<GrunnlagForPerson, Måned>.fjernOverflødigePerioderPåSlutten(): Tidslinje<GrunnlagForPerson, Måned> {
    val sortertePerioder = this.perioder()
        .sortedWith(compareBy({ it.fraOgMed }, { it.tilOgMed }))

    val perioderTilOgMedSisteInnvilgede = sortertePerioder
        .dropLastWhile { it.innhold !is GrunnlagForPersonVilkårInnvilget }

    val perioderEtterSisteInnvilgedePeriode =
        sortertePerioder.subList(perioderTilOgMedSisteInnvilgede.size, sortertePerioder.size)

    val (eksplisitteAvslagEtterSisteInnvilgedePeriode, opphørEtterSisteInnvilgedePeriode) =
        perioderEtterSisteInnvilgedePeriode
            .filter { it.innhold != null }
            .partition { it.innhold!!.erEksplisittAvslag() }

    val førsteOpphørEtterSisteInnvilgedePeriode =
        opphørEtterSisteInnvilgedePeriode.firstOrNull()?.copy(tilOgMed = MånedTidspunkt.uendeligLengeTil())

    return (perioderTilOgMedSisteInnvilgede + førsteOpphørEtterSisteInnvilgedePeriode + eksplisitteAvslagEtterSisteInnvilgedePeriode).filterNotNull()
        .tilTidslinje()
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

data class ForrigeOgDennePerioden(val forrige: GrunnlagForPerson?, val denne: GrunnlagForPerson?)

private fun Tidslinje<GrunnlagForPerson, Måned>.tilForrigeOgNåværendePeriodeTidslinje(): Tidslinje<ForrigeOgDennePerioden, Måned> {
    return (
        listOf(
            månedPeriodeAv(YearMonth.now(), YearMonth.now(), null),
        ) + this.perioder()
        ).zipWithNext { forrige, denne ->
        periodeAv(denne.fraOgMed, denne.tilOgMed, ForrigeOgDennePerioden(forrige.innhold, denne.innhold))
    }.tilTidslinje()
}
