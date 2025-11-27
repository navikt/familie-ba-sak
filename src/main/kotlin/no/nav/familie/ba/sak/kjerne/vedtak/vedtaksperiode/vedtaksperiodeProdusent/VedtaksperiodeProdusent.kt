package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilAndelForVedtaksperiodeTidslinjerPerAktørOgType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.ZipPadding
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.zipMedNeste
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.EØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.endringstidspunkt.utledEndringstidspunkt
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.mapVerdi
import no.nav.familie.tidslinje.omfatter
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.tomTidslinje
import no.nav.familie.tidslinje.utvidelser.filtrer
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.slåSammenLikePerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import java.time.LocalDate

fun genererVedtaksperioder(
    grunnlagForVedtaksperioder: BehandlingsGrunnlagForVedtaksperioder,
    grunnlagForVedtaksperioderForrigeBehandling: BehandlingsGrunnlagForVedtaksperioder?,
    vedtak: Vedtak,
    nåDato: LocalDate,
    featureToggleService: FeatureToggleService,
): List<VedtaksperiodeMedBegrunnelser> {
    if (vedtak.behandling.opprettetÅrsak.erOmregningsårsak()) {
        return lagPeriodeForOmregningsbehandling(
            vedtak = vedtak,
            nåDato = nåDato,
            andelTilkjentYtelser = grunnlagForVedtaksperioder.andelerTilkjentYtelse,
        )
    }

    if (vedtak.behandling.resultat == Behandlingsresultat.FORTSATT_INNVILGET) {
        return lagFortsattInnvilgetPeriode(vedtak = vedtak)
    }

    val grunnlagTidslinjePerPersonForrigeBehandling =
        grunnlagForVedtaksperioderForrigeBehandling
            ?.let { grunnlagForVedtaksperioderForrigeBehandling.utledGrunnlagTidslinjePerPerson(skalSplittePåValutakursendringer = false, featureToggleService = featureToggleService) }
            ?: emptyMap()

    val grunnlagTidslinjePerPerson = grunnlagForVedtaksperioder.utledGrunnlagTidslinjePerPerson(skalSplittePåValutakursendringer = false, featureToggleService = featureToggleService)

    val perioderSomSkalBegrunnesBasertPåDenneOgForrigeBehandling =
        finnPerioderSomSkalBegrunnes(
            grunnlagTidslinjePerPerson = grunnlagTidslinjePerPerson,
            grunnlagTidslinjePerPersonForrigeBehandling = grunnlagTidslinjePerPersonForrigeBehandling,
            endringstidspunkt =
                vedtak.behandling.overstyrtEndringstidspunkt ?: utledEndringstidspunkt(
                    behandlingsGrunnlagForVedtaksperioder = grunnlagForVedtaksperioder,
                    behandlingsGrunnlagForVedtaksperioderForrigeBehandling = grunnlagForVedtaksperioderForrigeBehandling,
                    featureToggleService = featureToggleService,
                ),
            personerFremstiltKravFor = grunnlagForVedtaksperioder.personerFremstiltKravFor,
        )

    val vedtaksperioder =
        perioderSomSkalBegrunnesBasertPåDenneOgForrigeBehandling.map { it.tilVedtaksperiodeMedBegrunnelser(vedtak, grunnlagForVedtaksperioder.personerFremstiltKravFor) }

    return if (grunnlagForVedtaksperioder.uregistrerteBarn.isNotEmpty()) {
        vedtaksperioder.leggTilPeriodeForUregistrerteBarn(vedtak)
    } else {
        vedtaksperioder
    }
}

private fun List<VedtaksperiodeMedBegrunnelser>.leggTilPeriodeForUregistrerteBarn(
    vedtak: Vedtak,
): List<VedtaksperiodeMedBegrunnelser> {
    fun VedtaksperiodeMedBegrunnelser.leggTilAvslagUregistrertBarnBegrunnelse() =
        when (vedtak.behandling.kategori) {
            BehandlingKategori.EØS -> {
                this.eøsBegrunnelser.add(
                    EØSBegrunnelse(
                        vedtaksperiodeMedBegrunnelser = this,
                        begrunnelse = EØSStandardbegrunnelse.AVSLAG_EØS_UREGISTRERT_BARN,
                    ),
                )
            }

            BehandlingKategori.NASJONAL -> {
                this.begrunnelser.add(
                    Vedtaksbegrunnelse(
                        vedtaksperiodeMedBegrunnelser = this,
                        standardbegrunnelse = Standardbegrunnelse.AVSLAG_UREGISTRERT_BARN,
                    ),
                )
            }
        }

    val avslagsperiodeUtenDatoer = this.find { it.fom == null && it.tom == null }

    return if (avslagsperiodeUtenDatoer != null) {
        avslagsperiodeUtenDatoer.leggTilAvslagUregistrertBarnBegrunnelse()
        this
    } else {
        val avslagsperiode: VedtaksperiodeMedBegrunnelser =
            VedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = null,
                tom = null,
                type = Vedtaksperiodetype.AVSLAG,
            ).also { it.leggTilAvslagUregistrertBarnBegrunnelse() }

        this + avslagsperiode
    }
}

fun finnPerioderSomSkalBegrunnes(
    grunnlagTidslinjePerPerson: Map<AktørOgRolleBegrunnelseGrunnlag, GrunnlagForPersonTidslinjerSplittetPåOverlappendeGenerelleAvslag>,
    grunnlagTidslinjePerPersonForrigeBehandling: Map<AktørOgRolleBegrunnelseGrunnlag, GrunnlagForPersonTidslinjerSplittetPåOverlappendeGenerelleAvslag>,
    endringstidspunkt: LocalDate,
    personerFremstiltKravFor: List<Aktør>,
): List<Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>>> {
    val gjeldendeOgForrigeGrunnlagKombinert =
        kombinerGjeldendeOgForrigeGrunnlag(
            grunnlagTidslinjePerPerson = grunnlagTidslinjePerPerson.mapValues { it.value.vedtaksperiodeGrunnlagForPerson },
            grunnlagTidslinjePerPersonForrigeBehandling = grunnlagTidslinjePerPersonForrigeBehandling.mapValues { it.value.vedtaksperiodeGrunnlagForPerson },
            personerFremstiltKravFor = personerFremstiltKravFor,
        )

    val sammenslåttePerioderUtenEksplisittAvslag =
        gjeldendeOgForrigeGrunnlagKombinert
            .slåSammenUtenEksplisitteAvslag(personerFremstiltKravFor)
            .filtrerPåEndringstidspunkt(endringstidspunkt)
            .slåSammenSammenhengendeOpphørsperioder()

    val eksplisitteAvslagsperioder = gjeldendeOgForrigeGrunnlagKombinert.utledEksplisitteAvslagsperioder(personerFremstiltKravFor = personerFremstiltKravFor)

    val overlappendeGenerelleAvslagPerioder = grunnlagTidslinjePerPerson.lagOverlappendeGenerelleAvslagsPerioder()

    return (overlappendeGenerelleAvslagPerioder + sammenslåttePerioderUtenEksplisittAvslag + eksplisitteAvslagsperioder)
        .slåSammenAvslagOgReduksjonsperioderMedSammeFomOgTom()
        .leggTilUendelighetPåSisteOpphørsPeriode(personerFremstiltKravFor)
}

fun List<Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>>>.slåSammenSammenhengendeOpphørsperioder(): List<Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>>> {
    val sortertePerioder = this.sortedWith(compareBy({ it.fom }, { it.tom }))

    return sortertePerioder.fold(emptyList()) { acc: List<Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>>>, dennePerioden ->
        val forrigePeriode = acc.lastOrNull()

        if (forrigePeriode != null &&
            !forrigePeriode.erPersonMedInnvilgedeVilkårIPeriode() &&
            !dennePerioden.erPersonMedInnvilgedeVilkårIPeriode()
        ) {
            acc.dropLast(1) + forrigePeriode.copy(tom = dennePerioden.tom)
        } else {
            acc + dennePerioden
        }
    }
}

fun List<Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>>>.leggTilUendelighetPåSisteOpphørsPeriode(personerFremstiltKravFor: List<Aktør>): List<Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>>> {
    val sortertePerioder = this.sortedWith(compareBy({ it.fom }, { it.tom }))

    val sistePeriode = sortertePerioder.lastOrNull()
    val sistePeriodeInneholderEksplisittAvslag = sistePeriode?.verdi?.any { it.gjeldende?.erEksplisittAvslag(personerFremstiltKravFor) == true } == true
    return if (sistePeriode != null &&
        !sistePeriode.erPersonMedInnvilgedeVilkårIPeriode() &&
        !sistePeriodeInneholderEksplisittAvslag
    ) {
        sortertePerioder.dropLast(1) + sistePeriode.copy(tom = null)
    } else {
        sortertePerioder
    }
}

private fun Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>>.erPersonMedInnvilgedeVilkårIPeriode() = verdi.any { it.gjeldende is VedtaksperiodeGrunnlagForPersonVilkårInnvilget }

private fun Map<AktørOgRolleBegrunnelseGrunnlag, GrunnlagForPersonTidslinjerSplittetPåOverlappendeGenerelleAvslag>.lagOverlappendeGenerelleAvslagsPerioder() =
    map {
        it.value.overlappendeGenerelleAvslagVedtaksperiodeGrunnlagForPerson
    }.kombiner {
        it
            .map { grunnlagForPerson ->
                GrunnlagForGjeldendeOgForrigeBehandling(
                    grunnlagForPerson,
                    false,
                )
            }.toList()
    }.tilPerioderIkkeNull()

private fun Collection<Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>>>.filtrerPåEndringstidspunkt(
    endringstidspunkt: LocalDate,
) = this.filter { (it.tom ?: TIDENES_ENDE).isSameOrAfter(endringstidspunkt) }

private fun List<Tidslinje<GrunnlagForGjeldendeOgForrigeBehandling>>.slåSammenUtenEksplisitteAvslag(personerFremstiltKravFor: List<Aktør>): Collection<Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>>> {
    val kombinerteAvslagOgReduksjonsperioder =
        this.map { grunnlagForDenneOgForrigeBehandlingTidslinje ->
            grunnlagForDenneOgForrigeBehandlingTidslinje.filtrerIkkeNull {
                val gjeldendeErIkkeInnvilgetIkkeAvslag =
                    it.gjeldende is VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget && !it.gjeldende.erEksplisittAvslag(personerFremstiltKravFor)
                val gjeldendeErInnvilget = it.gjeldende is VedtaksperiodeGrunnlagForPersonVilkårInnvilget
                val erReduksjonSidenForrigeBehandling = it.erReduksjonSidenForrigeBehandling

                gjeldendeErIkkeInnvilgetIkkeAvslag || gjeldendeErInnvilget || erReduksjonSidenForrigeBehandling
            }
        }

    return kombinerteAvslagOgReduksjonsperioder
        .kombiner { grunnlagTidslinje ->
            grunnlagTidslinje.toList().takeIf { it.isNotEmpty() }
        }.tilPerioderIkkeNull()
}

private fun List<Tidslinje<GrunnlagForGjeldendeOgForrigeBehandling>>.utledEksplisitteAvslagsperioder(personerFremstiltKravFor: List<Aktør>): Collection<Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>>> {
    val avslagsperioderPerPerson =
        this
            .map { it.filtrerErAvslagsperiode(personerFremstiltKravFor) }
            .map { tidslinje -> tidslinje.mapVerdi { it?.medVilkårSomHarEksplisitteAvslag() } }
            .flatMap { it.splittVilkårPerPerson() }
            .map { it.slåSammenLikePerioder() }

    val avslagsperioderMedSammeFomOgTom =
        avslagsperioderPerPerson
            .flatMap { it.tilPerioder() }
            .groupBy { Pair(it.fom, it.tom) }

    return avslagsperioderMedSammeFomOgTom
        .map { (fomTomPar, avslagMedSammeFomOgTom) ->
            Periode(
                verdi = avslagMedSammeFomOgTom.mapNotNull { it.verdi },
                fom = fomTomPar.first,
                tom = fomTomPar.second,
            )
        }
}

private fun Tidslinje<GrunnlagForGjeldendeOgForrigeBehandling>.splittVilkårPerPerson(): List<Tidslinje<GrunnlagForGjeldendeOgForrigeBehandling>> =
    tilPerioderIkkeNull()
        .mapNotNull { it.splittOppTilVilkårPerPerson() }
        .flatten()
        .groupBy({ it.first }, { it.second })
        .map { it.value.tilTidslinje() }

private fun Periode<GrunnlagForGjeldendeOgForrigeBehandling>.splittOppTilVilkårPerPerson(): List<Pair<AktørId, Periode<GrunnlagForGjeldendeOgForrigeBehandling>>>? {
    if (verdi.gjeldende == null) return null

    val vilkårPerPerson = verdi.gjeldende!!.vilkårResultaterForVedtaksperiode.groupBy { it.aktørId }

    return vilkårPerPerson.map { (aktørId, vilkårresultaterForPersonIPeriode) ->
        aktørId to
            this.copy(
                verdi =
                    this.verdi.copy(
                        gjeldende =
                            verdi.gjeldende!!.kopier(
                                vilkårResultaterForVedtaksperiode = vilkårresultaterForPersonIPeriode,
                            ),
                    ),
            )
    }
}

private fun Tidslinje<GrunnlagForGjeldendeOgForrigeBehandling>.filtrerErAvslagsperiode(personerFremstiltKravFor: List<Aktør>) = filtrer { it?.gjeldende?.erEksplisittAvslag(personerFremstiltKravFor) == true }

private fun GrunnlagForGjeldendeOgForrigeBehandling.medVilkårSomHarEksplisitteAvslag(): GrunnlagForGjeldendeOgForrigeBehandling =
    copy(
        gjeldende =
            this.gjeldende?.kopier(
                vilkårResultaterForVedtaksperiode =
                    this.gjeldende
                        .vilkårResultaterForVedtaksperiode
                        .filter { it.erEksplisittAvslagPåSøknad },
            ),
    )

/**
 * Ønsker å dra med informasjon om forrige behandling i perioder der forrige behandling var oppfylt, men gjeldende
 * ikke er det.
 **/
private fun kombinerGjeldendeOgForrigeGrunnlag(
    grunnlagTidslinjePerPerson: Map<AktørOgRolleBegrunnelseGrunnlag, Tidslinje<VedtaksperiodeGrunnlagForPerson>>,
    grunnlagTidslinjePerPersonForrigeBehandling: Map<AktørOgRolleBegrunnelseGrunnlag, Tidslinje<VedtaksperiodeGrunnlagForPerson>>,
    personerFremstiltKravFor: List<Aktør>,
): List<Tidslinje<GrunnlagForGjeldendeOgForrigeBehandling>> =
    grunnlagTidslinjePerPerson.map { (aktørId, grunnlagstidslinje) ->
        val grunnlagForrigeBehandling = grunnlagTidslinjePerPersonForrigeBehandling[aktørId]

        val ytelsestyperInnvilgetForrigeBehandlingTidslinje =
            grunnlagForrigeBehandling?.mapVerdi { it?.hentInnvilgedeYtelsestyper() } ?: tomTidslinje()

        val grunnlagTidslinjeMedInnvilgedeYtelsestyperForrigeBehandling =
            grunnlagstidslinje.kombinerMed(ytelsestyperInnvilgetForrigeBehandlingTidslinje) { gjeldendePeriode, innvilgedeYtelsestyperForrigeBehandling ->
                GjeldendeMedInnvilgedeYtelsestyperForrigeBehandling(
                    gjeldendePeriode,
                    innvilgedeYtelsestyperForrigeBehandling,
                )
            }

        grunnlagTidslinjeMedInnvilgedeYtelsestyperForrigeBehandling
            .zipMedNeste(ZipPadding.FØR)
            .mapVerdi {
                val forrigePeriode = it?.first
                val gjeldende = it?.second

                val erReduksjonFraForrigeBehandlingPåMinstEnYtelsestype =
                    erReduksjonFraForrigeBehandlingPåMinstEnYtelsestype(
                        innvilgedeYtelsestyperForrigePeriode = forrigePeriode?.grunnlagForPerson?.hentInnvilgedeYtelsestyper(),
                        innvilgedeYtelsestyperForrigePeriodeForrigeBehandling = forrigePeriode?.innvilgedeYtelsestyperForrigeBehandling,
                        innvilgedeYtelsestyperDennePerioden = gjeldende?.grunnlagForPerson?.hentInnvilgedeYtelsestyper(),
                        innvilgedeYtelsestyperDennePeriodenForrigeBehandling = gjeldende?.innvilgedeYtelsestyperForrigeBehandling,
                    )

                GrunnlagForGjeldendeOgForrigeBehandling(
                    gjeldende = gjeldende?.grunnlagForPerson,
                    erReduksjonSidenForrigeBehandling = erReduksjonFraForrigeBehandlingPåMinstEnYtelsestype,
                )
            }.slåSammenSammenhengendeOpphørsperioder(personerFremstiltKravFor)
    }

data class GjeldendeMedInnvilgedeYtelsestyperForrigeBehandling(
    val grunnlagForPerson: VedtaksperiodeGrunnlagForPerson?,
    val innvilgedeYtelsestyperForrigeBehandling: Set<YtelseType>?,
)

private fun erReduksjonFraForrigeBehandlingPåMinstEnYtelsestype(
    innvilgedeYtelsestyperForrigePeriode: Set<YtelseType>?,
    innvilgedeYtelsestyperForrigePeriodeForrigeBehandling: Set<YtelseType>?,
    innvilgedeYtelsestyperDennePerioden: Set<YtelseType>?,
    innvilgedeYtelsestyperDennePeriodenForrigeBehandling: Set<YtelseType>?,
): Boolean =
    YtelseType.entries.any { ytelseType ->

        if (innvilgedeYtelsestyperDennePerioden == null) {
            innvilgedeYtelsestyperDennePeriodenForrigeBehandling?.contains(ytelseType) ?: false
        } else {
            val ytelseInnvilgetDennePerioden =
                innvilgedeYtelsestyperDennePerioden.contains(ytelseType)
            val ytelseInnvilgetForrigePeriode =
                innvilgedeYtelsestyperForrigePeriode?.contains(ytelseType) ?: false
            val ytelseInnvilgetDennePeriodenForrigeBehandling =
                innvilgedeYtelsestyperDennePeriodenForrigeBehandling?.contains(ytelseType) ?: false
            val ytelseInnvilgetForrigePeriodeForrigeBehandling =
                innvilgedeYtelsestyperForrigePeriodeForrigeBehandling?.contains(ytelseType) ?: false

            !ytelseInnvilgetForrigePeriode &&
                !ytelseInnvilgetDennePerioden &&
                !ytelseInnvilgetForrigePeriodeForrigeBehandling &&
                ytelseInnvilgetDennePeriodenForrigeBehandling
        }
    }

private fun Tidslinje<GrunnlagForGjeldendeOgForrigeBehandling>.slåSammenSammenhengendeOpphørsperioder(personerFremstiltKravFor: List<Aktør>): Tidslinje<GrunnlagForGjeldendeOgForrigeBehandling> {
    val perioder = this.tilPerioderIkkeNull().sortedBy { it.fom }.toList()

    return perioder
        .fold(emptyList()) { acc: List<Periode<GrunnlagForGjeldendeOgForrigeBehandling>>, periode ->
            val sistePeriode = acc.lastOrNull()

            val erVilkårInnvilgetForrigePeriode = sistePeriode?.verdi?.gjeldende is VedtaksperiodeGrunnlagForPersonVilkårInnvilget
            val erVilkårInnvilget = periode.verdi.gjeldende is VedtaksperiodeGrunnlagForPersonVilkårInnvilget

            if (sistePeriode != null &&
                !erVilkårInnvilgetForrigePeriode &&
                !erVilkårInnvilget &&
                periode.verdi.erReduksjonSidenForrigeBehandling != true &&
                periode.verdi.gjeldende?.erEksplisittAvslag(personerFremstiltKravFor) != true &&
                sistePeriode.verdi.gjeldende?.erEksplisittAvslag(personerFremstiltKravFor) != true
            ) {
                acc.dropLast(1) + sistePeriode.copy(tom = periode.tom)
            } else {
                acc + periode
            }
        }.tilTidslinje()
}

fun Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>>.tilVedtaksperiodeMedBegrunnelser(
    vedtak: Vedtak,
    personerFremstiltKravFor: List<Aktør>,
): VedtaksperiodeMedBegrunnelser =
    VedtaksperiodeMedBegrunnelser(
        vedtak = vedtak,
        fom = fom,
        tom = tom,
        type = this.tilVedtaksperiodeType(personerFremstiltKravFor = personerFremstiltKravFor),
    ).let { vedtaksperiode ->
        val begrunnelser =
            this.verdi
                .flatMap { grunnlagForGjeldendeOgForrigeBehandling ->
                    grunnlagForGjeldendeOgForrigeBehandling.gjeldende
                        ?.finnVilkårResultaterSomGjelderPersonIVedtaksperiode(personerFremstiltKravFor)
                        ?.flatMap { it.standardbegrunnelser } ?: emptyList()
                }.toSet()

        vedtaksperiode.begrunnelser.addAll(
            begrunnelser
                .filterIsInstance<Standardbegrunnelse>()
                .map { Vedtaksbegrunnelse(vedtaksperiodeMedBegrunnelser = vedtaksperiode, standardbegrunnelse = it) },
        )

        vedtaksperiode.eøsBegrunnelser.addAll(
            begrunnelser
                .filterIsInstance<EØSStandardbegrunnelse>()
                .map { EØSBegrunnelse(vedtaksperiodeMedBegrunnelser = vedtaksperiode, begrunnelse = it) },
        )

        vedtaksperiode
    }

private fun VedtaksperiodeGrunnlagForPerson.finnVilkårResultaterSomGjelderPersonIVedtaksperiode(personerFremstiltKravFor: List<Aktør>): List<VilkårResultatForVedtaksperiode> =
    if (personerFremstiltKravFor.contains(this.person.aktør) || this.person.type == PersonType.SØKER) {
        this.vilkårResultaterForVedtaksperiode
    } else {
        this.vilkårResultaterForVedtaksperiode.filter { !it.erEksplisittAvslagPåSøknad }
    }

private fun Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>>.tilVedtaksperiodeType(personerFremstiltKravFor: List<Aktør>): Vedtaksperiodetype {
    val erUtbetalingsperiode = this.verdi.any { it.gjeldende?.erInnvilget() == true }
    val erAvslagsperiode = this.verdi.all { it.gjeldende?.erEksplisittAvslag(personerFremstiltKravFor) == true }

    return when {
        erUtbetalingsperiode -> {
            if (this.verdi.any { it.erReduksjonSidenForrigeBehandling }) {
                Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING
            } else {
                Vedtaksperiodetype.UTBETALING
            }
        }

        erAvslagsperiode -> {
            Vedtaksperiodetype.AVSLAG
        }

        else -> {
            Vedtaksperiodetype.OPPHØR
        }
    }
}

data class GrupperingskriterierForVedtaksperioder(
    val fom: LocalDate?,
    val tom: LocalDate?,
    val periodeInneholderInnvilgelse: Boolean,
)

private fun List<Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>>>.slåSammenAvslagOgReduksjonsperioderMedSammeFomOgTom() =
    this
        .groupBy { periode ->
            GrupperingskriterierForVedtaksperioder(
                fom = periode.fom,
                tom = periode.tom,
                periodeInneholderInnvilgelse = periode.verdi.any { it.gjeldende is VedtaksperiodeGrunnlagForPersonVilkårInnvilget },
            )
        }.map { (grupperingskriterier, verdi) ->
            Periode(
                verdi = verdi.map { periode -> periode.verdi }.flatten(),
                fom = grupperingskriterier.fom,
                tom = grupperingskriterier.tom,
            )
        }

fun lagFortsattInnvilgetPeriode(
    vedtak: Vedtak,
): List<VedtaksperiodeMedBegrunnelser> =
    listOf(
        VedtaksperiodeMedBegrunnelser(
            fom = null,
            tom = null,
            vedtak = vedtak,
            type = Vedtaksperiodetype.FORTSATT_INNVILGET,
        ),
    )

fun lagPeriodeForOmregningsbehandling(
    vedtak: Vedtak,
    andelTilkjentYtelser: List<AndelTilkjentYtelse>,
    nåDato: LocalDate,
): List<VedtaksperiodeMedBegrunnelser> {
    val andelerTidslinje: Tidslinje<List<AndelForVedtaksperiode>> =
        andelTilkjentYtelser.tilAndelForVedtaksperiodeTidslinjerPerAktørOgType().values.kombiner { it.toList() }

    val nesteEndringITilkjentYtelse =
        andelerTidslinje
            .tilPerioder()
            .singleOrNull { it.omfatter(nåDato) }
            ?.tom
            ?.toYearMonth()
            ?.sisteDagIInneværendeMåned()

    return listOf(
        VedtaksperiodeMedBegrunnelser(
            fom = nåDato.førsteDagIInneværendeMåned(),
            tom =
                nesteEndringITilkjentYtelse
                    ?: throw Feil("Fant ingen andeler for ${nåDato.tilMånedÅr()}. Autobrev skal ikke brukes for opphør."),
            vedtak = vedtak,
            type = Vedtaksperiodetype.UTBETALING,
        ),
    )
}
