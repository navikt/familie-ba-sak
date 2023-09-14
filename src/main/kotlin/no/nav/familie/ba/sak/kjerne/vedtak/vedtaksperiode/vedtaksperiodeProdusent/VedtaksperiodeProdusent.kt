package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent

import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak.OMREGNING_18ÅR
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak.OMREGNING_6ÅR
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak.OMREGNING_SMÅBARNSTILLEGG
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak.SMÅBARNSTILLEGG
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
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
import no.nav.familie.ba.sak.kjerne.tidslinje.tilTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.EØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import utledEndringstidspunkt
import java.time.LocalDate

fun genererVedtaksperioder(
    grunnlagForVedtakPerioder: BehandlingsGrunnlagForVedtaksperioder,
    grunnlagForVedtakPerioderForrigeBehandling: BehandlingsGrunnlagForVedtaksperioder?,
    vedtak: Vedtak,
    nåDato: LocalDate,
): List<VedtaksperiodeMedBegrunnelser> {
    if (vedtak.behandling.resultat == Behandlingsresultat.FORTSATT_INNVILGET || vedtak.behandling.opprettetÅrsak.erOmregningsårsak()) {
        return lagFortsattInnvilgetPeriode(
            vedtak = vedtak,
            andelTilkjentYtelseer = grunnlagForVedtakPerioder.andelerTilkjentYtelse,
            nåDato = nåDato,
        )
    }

    val grunnlagTidslinjePerPerson = grunnlagForVedtakPerioder.utledGrunnlagTidslinjePerPerson()

    val grunnlagTidslinjePerPersonForrigeBehandling =
        grunnlagForVedtakPerioderForrigeBehandling
            ?.let { grunnlagForVedtakPerioderForrigeBehandling.utledGrunnlagTidslinjePerPerson() }
            ?: emptyMap()

    val perioderSomSkalBegrunnesBasertPåDenneOgForrigeBehandling =
        finnPerioderSomSkalBegrunnes(
            grunnlagTidslinjePerPerson = grunnlagTidslinjePerPerson,
            grunnlagTidslinjePerPersonForrigeBehandling = grunnlagTidslinjePerPersonForrigeBehandling,
            endringstidspunkt = vedtak.behandling.overstyrtEndringstidspunkt ?: utledEndringstidspunkt(
                behandlingsGrunnlagForVedtaksperioder = grunnlagForVedtakPerioder,
                behandlingsGrunnlagForVedtaksperioderForrigeBehandling = grunnlagForVedtakPerioderForrigeBehandling,
            ),
        )

    val vedtaksperioder =
        perioderSomSkalBegrunnesBasertPåDenneOgForrigeBehandling.map { it.tilVedtaksperiodeMedBegrunnelser(vedtak) }

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
        val avslagsperiode: VedtaksperiodeMedBegrunnelser = VedtaksperiodeMedBegrunnelser(
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
): List<Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>, Måned>> {
    val gjeldendeOgForrigeGrunnlagKombinert = kombinerGjeldendeOgForrigeGrunnlag(
        grunnlagTidslinjePerPerson = grunnlagTidslinjePerPerson.mapValues { it.value.vedtaksperiodeGrunnlagForPerson },
        grunnlagTidslinjePerPersonForrigeBehandling = grunnlagTidslinjePerPersonForrigeBehandling.mapValues { it.value.vedtaksperiodeGrunnlagForPerson },
    )

    val sammenslåttePerioderUtenEksplisittAvslag = gjeldendeOgForrigeGrunnlagKombinert
        .slåSammenUtenEksplisitteAvslag()
        .filtrerPåEndringstidspunkt(endringstidspunkt)

    val eksplisitteAvslagsperioder = gjeldendeOgForrigeGrunnlagKombinert.utledEksplisitteAvslagsperioder()

    val overlappendeGenerelleAvslagPerioder = grunnlagTidslinjePerPerson.lagOverlappendeGenerelleAvslagsPerioder()

    return (overlappendeGenerelleAvslagPerioder + sammenslåttePerioderUtenEksplisittAvslag + eksplisitteAvslagsperioder)
        .slåSammenAvslagOgReduksjonsperioderMedSammeFomOgTom()
        .fjernOverflødigeIkkeInnvilgetPerioder()
}

fun List<Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>, Måned>>.fjernOverflødigeIkkeInnvilgetPerioder(): List<Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>, Måned>> {
    val sortertePerioder = this
        .sortedWith(compareBy({ it.fraOgMed }, { it.tilOgMed }))

    val perioderTilOgMedSisteInnvilgede = sortertePerioder.dropLastWhile { periode ->
        periode.innhold == null || periode.innhold.none { it.gjeldende is VedtaksperiodeGrunnlagForPersonVilkårInnvilget }
    }

    val perioderEtterSisteInnvilgedePeriode =
        sortertePerioder.subList(perioderTilOgMedSisteInnvilgede.size, sortertePerioder.size)

    val (eksplisitteAvslagEtterSisteInnvilgedePeriode, opphørEtterSisteInnvilgedePeriode) =
        perioderEtterSisteInnvilgedePeriode
            .filter { it.innhold != null }
            .partition { periode -> periode.innhold!!.any { it.gjeldende?.erEksplisittAvslag() == true } }

    val førsteOpphørEtterSisteInnvilgedePeriode =
        opphørEtterSisteInnvilgedePeriode.firstOrNull()?.copy(tilOgMed = MånedTidspunkt.uendeligLengeTil())

    return (perioderTilOgMedSisteInnvilgede + førsteOpphørEtterSisteInnvilgedePeriode + eksplisitteAvslagEtterSisteInnvilgedePeriode).filterNotNull()
}

private fun Map<AktørOgRolleBegrunnelseGrunnlag, GrunnlagForPersonTidslinjerSplittetPåOverlappendeGenerelleAvslag>.lagOverlappendeGenerelleAvslagsPerioder() =
    map {
        it.value.overlappendeGenerelleAvslagVedtaksperiodeGrunnlagForPerson
    }.kombiner {
        it.map { grunnlagForPerson ->
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

private fun List<Tidslinje<GrunnlagForGjeldendeOgForrigeBehandling, Måned>>.slåSammenUtenEksplisitteAvslag(): Collection<Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>, Måned>> {
    val kombinerteAvslagOgReduksjonsperioder = this.map { grunnlagForDenneOgForrigeBehandlingTidslinje ->
        grunnlagForDenneOgForrigeBehandlingTidslinje.filtrerIkkeNull {
            val gjeldendeErIkkeInnvilgetIkkeAvslag =
                it.gjeldende is VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget && !it.gjeldende.erEksplisittAvslag
            val gjeldendeErInnvilget = it.gjeldende is VedtaksperiodeGrunnlagForPersonVilkårInnvilget
            val gjeldendeErNullForrigeErInnvilget = it.gjeldendeErNullForrigeErInnvilget

            gjeldendeErIkkeInnvilgetIkkeAvslag || gjeldendeErInnvilget || gjeldendeErNullForrigeErInnvilget
        }
    }

    return kombinerteAvslagOgReduksjonsperioder.kombiner { grunnlagTidslinje ->
        grunnlagTidslinje.toList().takeIf { it.isNotEmpty() }
    }.perioder()
}

private fun List<Tidslinje<GrunnlagForGjeldendeOgForrigeBehandling, Måned>>.utledEksplisitteAvslagsperioder(): Collection<Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>, Måned>> {
    val avslagsperioderPerPerson = this.map { it.filtrerErAvslagsperiode() }
        .map { tidslinje -> tidslinje.map { it?.medVilkårSomHarEksplisitteAvslag() } }
        .flatMap { it.splittVilkårPerPerson() }
        .map { it.slåSammenLike() }

    val avslagsperioderMedSammeFomOgTom = avslagsperioderPerPerson
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

private fun Tidslinje<GrunnlagForGjeldendeOgForrigeBehandling, Måned>.splittVilkårPerPerson(): List<Tidslinje<GrunnlagForGjeldendeOgForrigeBehandling, Måned>> {
    return perioder()
        .mapNotNull { it.splittOppTilVilkårPerPerson() }
        .flatten()
        .groupBy({ it.first }, { it.second })
        .map { it.value.tilTidslinje() }
}

private fun Periode<GrunnlagForGjeldendeOgForrigeBehandling, Måned>.splittOppTilVilkårPerPerson(): List<Pair<AktørId, Periode<GrunnlagForGjeldendeOgForrigeBehandling, Måned>>>? {
    if (innhold?.gjeldende == null) return null

    val vilkårPerPerson =
        innhold.gjeldende.vilkårResultaterForVedtaksperiode.groupBy { it.aktørId }

    return vilkårPerPerson.map { (aktørId, vilkårresultaterForPersonIPeriode) ->
        aktørId to this.copy(
            innhold = this.innhold.copy(
                gjeldende = innhold.gjeldende.kopier(
                    vilkårResultaterForVedtaksperiode = vilkårresultaterForPersonIPeriode,
                ),
            ),
        )
    }
}

private fun Tidslinje<GrunnlagForGjeldendeOgForrigeBehandling, Måned>.filtrerErAvslagsperiode() =
    filtrer { it?.gjeldende?.erEksplisittAvslag() == true }

private fun GrunnlagForGjeldendeOgForrigeBehandling.medVilkårSomHarEksplisitteAvslag(): GrunnlagForGjeldendeOgForrigeBehandling {
    return copy(
        gjeldende = this.gjeldende?.kopier(
            vilkårResultaterForVedtaksperiode = this.gjeldende
                .vilkårResultaterForVedtaksperiode
                .filter { it.erEksplisittAvslagPåSøknad == true },
        ),
    )
}

/**
 * Ønsker å dra med informasjon om forrige behandling i perioder der forrige behandling var oppfylt, men gjeldende
 * ikke er det.
 **/
private fun kombinerGjeldendeOgForrigeGrunnlag(
    grunnlagTidslinjePerPerson: Map<AktørOgRolleBegrunnelseGrunnlag, Tidslinje<VedtaksperiodeGrunnlagForPerson, Måned>>,
    grunnlagTidslinjePerPersonForrigeBehandling: Map<AktørOgRolleBegrunnelseGrunnlag, Tidslinje<VedtaksperiodeGrunnlagForPerson, Måned>>,
): List<Tidslinje<GrunnlagForGjeldendeOgForrigeBehandling, Måned>> =
    grunnlagTidslinjePerPerson.map { (aktørId, grunnlagstidslinje) ->
        val grunnlagForrigeBehandling = grunnlagTidslinjePerPersonForrigeBehandling[aktørId]

        grunnlagstidslinje.kombinerMed(grunnlagForrigeBehandling ?: TomTidslinje()) { gjeldende, forrige ->
            val gjeldendeErIkkeOppfylt = gjeldende !is VedtaksperiodeGrunnlagForPersonVilkårInnvilget
            val forrigeErOppfylt = forrige is VedtaksperiodeGrunnlagForPersonVilkårInnvilget

            if (gjeldendeErIkkeOppfylt && forrigeErOppfylt) {
                GrunnlagForGjeldendeOgForrigeBehandling(gjeldende, true)
            } else {
                gjeldende?.let { GrunnlagForGjeldendeOgForrigeBehandling(gjeldende, false) }
            }
        }.slåSammenLike()
    }

fun Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>, Måned>.tilVedtaksperiodeMedBegrunnelser(
    vedtak: Vedtak,
): VedtaksperiodeMedBegrunnelser = VedtaksperiodeMedBegrunnelser(
    vedtak = vedtak,
    fom = fraOgMed.tilDagEllerFørsteDagIPerioden().tilLocalDateEllerNull(),
    tom = tilOgMed.tilLocalDateEllerNull(),
    type = this.tilVedtaksperiodeType(),
).let { vedtaksperiode ->
    val begrunnelser = this.innhold?.flatMap { grunnlagForGjeldendeOgForrigeBehandling ->
        grunnlagForGjeldendeOgForrigeBehandling.gjeldende?.vilkårResultaterForVedtaksperiode
            ?.flatMap { it.standardbegrunnelser } ?: emptyList()
    } ?: emptyList()

    vedtaksperiode.begrunnelser.addAll(
        begrunnelser.filterIsInstance<Standardbegrunnelse>()
            .map { Vedtaksbegrunnelse(vedtaksperiodeMedBegrunnelser = vedtaksperiode, standardbegrunnelse = it) },
    )

    vedtaksperiode.eøsBegrunnelser.addAll(
        begrunnelser.filterIsInstance<EØSStandardbegrunnelse>()
            .map { EØSBegrunnelse(vedtaksperiodeMedBegrunnelser = vedtaksperiode, begrunnelse = it) },
    )

    vedtaksperiode
}

private fun Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>, Måned>.tilVedtaksperiodeType(): Vedtaksperiodetype {
    val erUtbetalingsperiode =
        this.innhold != null && this.innhold.any { it.gjeldende?.erInnvilget() == true }
    val erAvslagsperiode = this.innhold != null && this.innhold.all { it.gjeldende?.erEksplisittAvslag() == true }

    return when {
        erUtbetalingsperiode -> if (this.innhold?.any { it.gjeldendeErNullForrigeErInnvilget } == true) {
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
    this.groupBy { periode ->
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
    andelTilkjentYtelseer: List<AndelTilkjentYtelse>,
    nåDato: LocalDate,
): List<VedtaksperiodeMedBegrunnelser> {
    val behandling = vedtak.behandling
    val erAutobrevFor6År18ÅrEllerSmåbarnstillegg = behandling.opprettetÅrsak in listOf(
        OMREGNING_6ÅR,
        OMREGNING_18ÅR,
        SMÅBARNSTILLEGG,
        OMREGNING_SMÅBARNSTILLEGG,
    )

    val (fom, tom) = if (erAutobrevFor6År18ÅrEllerSmåbarnstillegg) {
        Pair(
            nåDato.førsteDagIInneværendeMåned(),
            finnTomDatoIFørsteUtbetalingsintervallFraInneværendeMåned(behandling.id, andelTilkjentYtelseer, nåDato),
        )
    } else {
        Pair(null, null)
    }

    return listOf(
        VedtaksperiodeMedBegrunnelser(
            fom = fom,
            tom = tom,
            vedtak = vedtak,
            type = Vedtaksperiodetype.FORTSATT_INNVILGET,
        ),
    )
}

private fun finnTomDatoIFørsteUtbetalingsintervallFraInneværendeMåned(
    behandlingId: Long,
    andelTilkjentYtelses: List<AndelTilkjentYtelse>,
    nåDato: LocalDate,
): LocalDate =
    andelTilkjentYtelses
        .filter { it.stønadFom <= nåDato.toYearMonth() && it.stønadTom >= nåDato.toYearMonth() }
        .minByOrNull { it.stønadTom }?.stønadTom?.sisteDagIInneværendeMåned()
        ?: error("Fant ikke andel for tilkjent ytelse inneværende måned for behandling $behandlingId.")
