package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent

import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilAndelForVedtaksperiodeTidslinjerPerAktørOgType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrer
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TomTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.slåSammenLike
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilDagEllerFørsteDagIPerioden
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilLocalDateEllerNull
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonth
import no.nav.familie.ba.sak.kjerne.tidslinje.tilTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.ZipPadding
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.zipMedNeste
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.EØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.utledEndringstidspunkt
import java.time.LocalDate

fun genererVedtaksperioder(
    grunnlagForVedtakPerioder: BehandlingsGrunnlagForVedtaksperioder,
    grunnlagForVedtakPerioderForrigeBehandling: BehandlingsGrunnlagForVedtaksperioder?,
    vedtak: Vedtak,
    nåDato: LocalDate,
    personerFremstiltKravFor: List<Aktør>,
): List<VedtaksperiodeMedBegrunnelser> {
    if (vedtak.behandling.opprettetÅrsak.erOmregningsårsak()) {
        return lagPeriodeForOmregningsbehandling(
            vedtak = vedtak,
            nåDato = nåDato,
            andelTilkjentYtelser = grunnlagForVedtakPerioder.andelerTilkjentYtelse,
        )
    }

    if (vedtak.behandling.resultat == Behandlingsresultat.FORTSATT_INNVILGET) {
        return lagFortsattInnvilgetPeriode(vedtak = vedtak)
    }

    val grunnlagTidslinjePerPersonForrigeBehandling =
        grunnlagForVedtakPerioderForrigeBehandling
            ?.let { grunnlagForVedtakPerioderForrigeBehandling.utledGrunnlagTidslinjePerPerson() }
            ?: emptyMap()

    val grunnlagTidslinjePerPerson = grunnlagForVedtakPerioder.utledGrunnlagTidslinjePerPerson()

    val perioderSomSkalBegrunnesBasertPåDenneOgForrigeBehandling =
        finnPerioderSomSkalBegrunnes(
            grunnlagTidslinjePerPerson = grunnlagTidslinjePerPerson,
            grunnlagTidslinjePerPersonForrigeBehandling = grunnlagTidslinjePerPersonForrigeBehandling,
            endringstidspunkt =
                vedtak.behandling.overstyrtEndringstidspunkt ?: utledEndringstidspunkt(
                    behandlingsGrunnlagForVedtaksperioder = grunnlagForVedtakPerioder,
                    behandlingsGrunnlagForVedtaksperioderForrigeBehandling = grunnlagForVedtakPerioderForrigeBehandling,
                ),
            personerFremstiltKravFor = personerFremstiltKravFor,
        )

    val vedtaksperioder =
        perioderSomSkalBegrunnesBasertPåDenneOgForrigeBehandling.map { it.tilVedtaksperiodeMedBegrunnelser(vedtak, personerFremstiltKravFor) }

    return if (grunnlagForVedtakPerioder.uregistrerteBarn.isNotEmpty()) {
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
): List<Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>, Måned>> {
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

fun List<Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>, Måned>>.slåSammenSammenhengendeOpphørsperioder(): List<Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>, Måned>> {
    val sortertePerioder =
        this
            .sortedWith(compareBy({ it.fraOgMed }, { it.tilOgMed }))

    return sortertePerioder.fold(emptyList()) { acc: List<Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>, Måned>>, dennePerioden ->
        val forrigePeriode = acc.lastOrNull()

        if (forrigePeriode != null &&
            !forrigePeriode.erPersonMedInnvilgedeVilkårIPeriode() &&
            !dennePerioden.erPersonMedInnvilgedeVilkårIPeriode()
        ) {
            acc.dropLast(1) + forrigePeriode.copy(tilOgMed = dennePerioden.tilOgMed)
        } else {
            acc + dennePerioden
        }
    }
}

fun List<Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>, Måned>>.leggTilUendelighetPåSisteOpphørsPeriode(personerFremstiltKravFor: List<Aktør>): List<Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>, Måned>> {
    val sortertePerioder =
        this
            .sortedWith(compareBy({ it.fraOgMed }, { it.tilOgMed }))

    val sistePeriode = sortertePerioder.lastOrNull()
    val sistePeriodeInneholderEksplisittAvslag =
        sistePeriode?.innhold?.any { it.gjeldende?.erEksplisittAvslag(personerFremstiltKravFor) == true } == true
    return if (sistePeriode != null &&
        !sistePeriode.erPersonMedInnvilgedeVilkårIPeriode() &&
        !sistePeriodeInneholderEksplisittAvslag
    ) {
        sortertePerioder.dropLast(1) + sistePeriode.copy(tilOgMed = MånedTidspunkt.uendeligLengeTil())
    } else {
        sortertePerioder
    }
}

private fun Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>, Måned>.erPersonMedInnvilgedeVilkårIPeriode() =
    innhold != null && innhold.any { it.gjeldende is VedtaksperiodeGrunnlagForPersonVilkårInnvilget }

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
    }.perioder()

private fun Collection<Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>, Måned>>.filtrerPåEndringstidspunkt(
    endringstidspunkt: LocalDate,
) = this.filter {
    (it.tilOgMed.tilLocalDateEllerNull() ?: TIDENES_ENDE).isSameOrAfter(endringstidspunkt)
}

private fun List<Tidslinje<GrunnlagForGjeldendeOgForrigeBehandling, Måned>>.slåSammenUtenEksplisitteAvslag(personerFremstiltKravFor: List<Aktør>): Collection<Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>, Måned>> {
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
        }.perioder()
}

private fun List<Tidslinje<GrunnlagForGjeldendeOgForrigeBehandling, Måned>>.utledEksplisitteAvslagsperioder(personerFremstiltKravFor: List<Aktør>): Collection<Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>, Måned>> {
    val avslagsperioderPerPerson =
        this
            .map { it.filtrerErAvslagsperiode(personerFremstiltKravFor) }
            .map { tidslinje -> tidslinje.map { it?.medVilkårSomHarEksplisitteAvslag() } }
            .flatMap { it.splittVilkårPerPerson() }
            .map { it.slåSammenLike() }

    val avslagsperioderMedSammeFomOgTom =
        avslagsperioderPerPerson
            .flatMap { it.perioder() }
            .groupBy { Pair(it.fraOgMed, it.tilOgMed) }

    return avslagsperioderMedSammeFomOgTom
        .map { (fomTomPar, avslagMedSammeFomOgTom) ->
            Periode(
                fraOgMed = fomTomPar.first,
                tilOgMed = fomTomPar.second,
                innhold = avslagMedSammeFomOgTom.mapNotNull { it.innhold },
            )
        }
}

private fun Tidslinje<GrunnlagForGjeldendeOgForrigeBehandling, Måned>.splittVilkårPerPerson(): List<Tidslinje<GrunnlagForGjeldendeOgForrigeBehandling, Måned>> =
    perioder()
        .mapNotNull { it.splittOppTilVilkårPerPerson() }
        .flatten()
        .groupBy({ it.first }, { it.second })
        .map { it.value.tilTidslinje() }

private fun Periode<GrunnlagForGjeldendeOgForrigeBehandling, Måned>.splittOppTilVilkårPerPerson(): List<Pair<AktørId, Periode<GrunnlagForGjeldendeOgForrigeBehandling, Måned>>>? {
    if (innhold?.gjeldende == null) return null

    val vilkårPerPerson =
        innhold.gjeldende.vilkårResultaterForVedtaksperiode.groupBy { it.aktørId }

    return vilkårPerPerson.map { (aktørId, vilkårresultaterForPersonIPeriode) ->
        aktørId to
            this.copy(
                innhold =
                    this.innhold.copy(
                        gjeldende =
                            innhold.gjeldende.kopier(
                                vilkårResultaterForVedtaksperiode = vilkårresultaterForPersonIPeriode,
                            ),
                    ),
            )
    }
}

private fun Tidslinje<GrunnlagForGjeldendeOgForrigeBehandling, Måned>.filtrerErAvslagsperiode(personerFremstiltKravFor: List<Aktør>) =
    filtrer { it?.gjeldende?.erEksplisittAvslag(personerFremstiltKravFor) == true }

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
    grunnlagTidslinjePerPerson: Map<AktørOgRolleBegrunnelseGrunnlag, Tidslinje<VedtaksperiodeGrunnlagForPerson, Måned>>,
    grunnlagTidslinjePerPersonForrigeBehandling: Map<AktørOgRolleBegrunnelseGrunnlag, Tidslinje<VedtaksperiodeGrunnlagForPerson, Måned>>,
    personerFremstiltKravFor: List<Aktør>,
): List<Tidslinje<GrunnlagForGjeldendeOgForrigeBehandling, Måned>> =
    grunnlagTidslinjePerPerson.map { (aktørId, grunnlagstidslinje) ->
        val grunnlagForrigeBehandling = grunnlagTidslinjePerPersonForrigeBehandling[aktørId]

        val ytelsestyperInnvilgetForrigeBehandlingTidslinje =
            grunnlagForrigeBehandling?.map { it?.hentInnvilgedeYtelsestyper() } ?: TomTidslinje()

        val grunnlagTidslinjeMedInnvilgedeYtelsestyperForrigeBehandling =
            grunnlagstidslinje.kombinerMed(ytelsestyperInnvilgetForrigeBehandlingTidslinje) { gjeldendePeriode, innvilgedeYtelsestyperForrigeBehandling ->
                GjeldendeMedInnvilgedeYtelsestyperForrigeBehandling(
                    gjeldendePeriode,
                    innvilgedeYtelsestyperForrigeBehandling,
                )
            }

        grunnlagTidslinjeMedInnvilgedeYtelsestyperForrigeBehandling
            .zipMedNeste(ZipPadding.FØR)
            .map {
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
        val ytelseInnvilgetDennePerioden =
            innvilgedeYtelsestyperDennePerioden?.contains(ytelseType) ?: false
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

private fun Tidslinje<GrunnlagForGjeldendeOgForrigeBehandling, Måned>.slåSammenSammenhengendeOpphørsperioder(personerFremstiltKravFor: List<Aktør>): Tidslinje<GrunnlagForGjeldendeOgForrigeBehandling, Måned> {
    val perioder = this.perioder().sortedBy { it.fraOgMed }.toList()

    return perioder
        .fold(emptyList()) { acc: List<Periode<GrunnlagForGjeldendeOgForrigeBehandling, Måned>>, periode ->
            val sistePeriode = acc.lastOrNull()

            val erVilkårInnvilgetForrigePeriode =
                sistePeriode?.innhold?.gjeldende is VedtaksperiodeGrunnlagForPersonVilkårInnvilget
            val erVilkårInnvilget = periode.innhold?.gjeldende is VedtaksperiodeGrunnlagForPersonVilkårInnvilget

            if (sistePeriode != null &&
                !erVilkårInnvilgetForrigePeriode &&
                !erVilkårInnvilget &&
                periode.innhold?.erReduksjonSidenForrigeBehandling != true &&
                periode.innhold?.gjeldende?.erEksplisittAvslag(personerFremstiltKravFor) != true &&
                sistePeriode.innhold?.gjeldende?.erEksplisittAvslag(personerFremstiltKravFor) != true
            ) {
                acc.dropLast(1) + sistePeriode.copy(tilOgMed = periode.tilOgMed)
            } else {
                acc + periode
            }
        }.tilTidslinje()
}

fun Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>, Måned>.tilVedtaksperiodeMedBegrunnelser(
    vedtak: Vedtak,
    personerFremstiltKravFor: List<Aktør>,
): VedtaksperiodeMedBegrunnelser =
    VedtaksperiodeMedBegrunnelser(
        vedtak = vedtak,
        fom = fraOgMed.tilDagEllerFørsteDagIPerioden().tilLocalDateEllerNull(),
        tom = tilOgMed.tilLocalDateEllerNull(),
        type = this.tilVedtaksperiodeType(personerFremstiltKravFor = personerFremstiltKravFor),
    ).let { vedtaksperiode ->
        val begrunnelser =
            this.innhold?.flatMap { grunnlagForGjeldendeOgForrigeBehandling ->
                grunnlagForGjeldendeOgForrigeBehandling.gjeldende
                    ?.finnVilkårResultaterSomGjelderPersonIVedtaksperiode(personerFremstiltKravFor)
                    ?.flatMap { it.standardbegrunnelser } ?: emptyList()
            }?.toSet() ?: emptyList()

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
    this.vilkårResultaterForVedtaksperiode.filter { !it.erEksplisittAvslagPåSøknad || personerFremstiltKravFor.contains(this.person.aktør) }

private fun Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>, Måned>.tilVedtaksperiodeType(personerFremstiltKravFor: List<Aktør>): Vedtaksperiodetype {
    val erUtbetalingsperiode =
        this.innhold != null && this.innhold.any { it.gjeldende?.erInnvilget() == true }
    val erAvslagsperiode = this.innhold != null && this.innhold.all { it.gjeldende?.erEksplisittAvslag(personerFremstiltKravFor) == true }

    return when {
        erUtbetalingsperiode ->
            if (this.innhold?.any { it.erReduksjonSidenForrigeBehandling } == true) {
                Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING
            } else {
                Vedtaksperiodetype.UTBETALING
            }

        erAvslagsperiode -> Vedtaksperiodetype.AVSLAG

        else -> Vedtaksperiodetype.OPPHØR
    }
}

data class GrupperingskriterierForVedtaksperioder(
    val fom: Tidspunkt<Måned>,
    val tom: Tidspunkt<Måned>,
    val periodeInneholderInnvilgelse: Boolean,
)

private fun List<Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>, Måned>>.slåSammenAvslagOgReduksjonsperioderMedSammeFomOgTom() =
    this
        .groupBy { periode ->
            GrupperingskriterierForVedtaksperioder(
                fom = periode.fraOgMed,
                tom = periode.tilOgMed,
                periodeInneholderInnvilgelse = periode.innhold?.any { it.gjeldende is VedtaksperiodeGrunnlagForPersonVilkårInnvilget } == true,
            )
        }.map { (grupperingskriterier, verdi) ->
            Periode(
                fraOgMed = grupperingskriterier.fom,
                tilOgMed = grupperingskriterier.tom,
                innhold = verdi.mapNotNull { periode -> periode.innhold }.flatten(),
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
    val andelerTidslinje: Tidslinje<List<AndelForVedtaksperiode>, Måned> =
        andelTilkjentYtelser.tilAndelForVedtaksperiodeTidslinjerPerAktørOgType().values.kombiner { it.toList() }

    val nesteEndringITilkjentYtelse =
        andelerTidslinje
            .perioder()
            .singleOrNull { it.periodeInneholder(nåDato) }
            ?.tilOgMed
            ?.tilYearMonth()
            ?.sisteDagIInneværendeMåned()

    return listOf(
        VedtaksperiodeMedBegrunnelser(
            fom = nåDato.førsteDagIInneværendeMåned(),
            tom =
                nesteEndringITilkjentYtelse
                    ?: error("Fant ingen andeler for ${nåDato.tilMånedÅr()}. Autobrev skal ikke brukes for opphør."),
            vedtak = vedtak,
            type = Vedtaksperiodetype.UTBETALING,
        ),
    )
}

private fun <T> Periode<T, Måned>.periodeInneholder(
    nåDato: LocalDate,
) = this.fraOgMed.tilYearMonth() <= nåDato.toYearMonth() && this.tilOgMed.tilYearMonth() >= nåDato.toYearMonth()
