package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrer
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TomTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.slåSammenLike
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
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

fun genererVedtaksperioder(
    grunnlagForVedtakPerioder: GrunnlagForVedtaksperioder,
    grunnlagForVedtakPerioderForrigeBehandling: GrunnlagForVedtaksperioder?,
    vedtak: Vedtak,
): List<VedtaksperiodeMedBegrunnelser> {
    val grunnlagTidslinjePerPerson = grunnlagForVedtakPerioder.utledGrunnlagTidslinjePerPerson()

    val grunnlagTidslinjePerPersonForrigeBehandling =
        grunnlagForVedtakPerioderForrigeBehandling
            ?.let { grunnlagForVedtakPerioderForrigeBehandling.utledGrunnlagTidslinjePerPerson() }
            ?: emptyMap()

    val perioderSomSkalBegrunnesBasertPåDenneOgForrigeBehandling =
        finnPerioderSomSkalBegrunnes(
            grunnlagTidslinjePerPerson = grunnlagTidslinjePerPerson,
            grunnlagTidslinjePerPersonForrigeBehandling = grunnlagTidslinjePerPersonForrigeBehandling,
        )

    return perioderSomSkalBegrunnesBasertPåDenneOgForrigeBehandling.map { it.tilVedtaksperiodeMedBegrunnelser(vedtak) }
}

fun finnPerioderSomSkalBegrunnes(
    grunnlagTidslinjePerPerson: Map<AktørId, Tidslinje<GrunnlagForPerson, Måned>>,
    grunnlagTidslinjePerPersonForrigeBehandling: Map<AktørId, Tidslinje<GrunnlagForPerson, Måned>>,
): List<Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>, Måned>> {
    val gjeldendeOgForrigeGrunnlagKombinert = kombinerGjeldendeOgForrigeGrunnlag(
        grunnlagTidslinjePerPerson = grunnlagTidslinjePerPerson,
        grunnlagTidslinjePerPersonForrigeBehandling = grunnlagTidslinjePerPersonForrigeBehandling,
    )

    val sammenslåttePerioderUtenEksplisittAvslag = gjeldendeOgForrigeGrunnlagKombinert.slåSammenUtenEksplisitteAvslag()
    val eksplisitteAvslagsperioder = gjeldendeOgForrigeGrunnlagKombinert.utledEksplisitteAvslagsperioder()

    return (eksplisitteAvslagsperioder + sammenslåttePerioderUtenEksplisittAvslag).slåSammenAvslagOgReduksjonsperioderMedSammeFomOgTom()
}

private fun List<Tidslinje<GrunnlagForGjeldendeOgForrigeBehandling, Måned>>.slåSammenUtenEksplisitteAvslag(): Collection<Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>, Måned>> {
    val kombinerteAvslagOgReduksjonsperioder = this.map { grunnlagForDenneOgForrigeBehandlingTidslinje ->
        grunnlagForDenneOgForrigeBehandlingTidslinje.filtrer {
            val gjeldendeErIkkeInnvilgetIkkeAvslag =
                it?.gjeldende is GrunnlagForPersonIkkeInnvilget && !it.gjeldende.erEksplisittAvslag
            val gjeldendeErInnvilget = it?.gjeldende is GrunnlagForPersonInnvilget
            val gjeldendeErNullForrigeErInnvilget = it?.gjeldende == null && it?.personHarRettIForrigeBehandling == true

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
    grunnlagTidslinjePerPerson: Map<AktørId, Tidslinje<GrunnlagForPerson, Måned>>,
    grunnlagTidslinjePerPersonForrigeBehandling: Map<AktørId, Tidslinje<GrunnlagForPerson, Måned>>,
): List<Tidslinje<GrunnlagForGjeldendeOgForrigeBehandling, Måned>> =
    grunnlagTidslinjePerPerson.map { (aktørId, grunnlagstidslinje) ->
        val grunnlagForrigeBehandling = grunnlagTidslinjePerPersonForrigeBehandling[aktørId]

        grunnlagstidslinje.kombinerMed(grunnlagForrigeBehandling ?: TomTidslinje()) { gjeldende, forrige ->
            val gjeldendeErIkkeOppfylt = gjeldende !is GrunnlagForPersonInnvilget
            val forrigeErOppfylt = forrige is GrunnlagForPersonInnvilget

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
    type = if (this.innhold == null) {
        Vedtaksperiodetype.OPPHØR
    } else if (this.innhold.any { it.gjeldende is GrunnlagForPersonInnvilget }) {
        Vedtaksperiodetype.UTBETALING
    } else if (this.innhold.all { it.gjeldende?.erEksplisittAvslag() == true }) {
        Vedtaksperiodetype.AVSLAG
    } else {
        Vedtaksperiodetype.OPPHØR
    },
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
            periodeInneholderInnvilgelse = periode.innhold?.any { it.gjeldende is GrunnlagForPersonInnvilget } == true,
        )
    }.map { (grupperingskriterier, verdi) ->
        Periode(
            fraOgMed = grupperingskriterier.fom,
            tilOgMed = grupperingskriterier.tom,
            innhold = verdi.mapNotNull { periode -> periode.innhold }.flatten(),
        )
    }
