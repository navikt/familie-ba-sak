package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrer
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TomTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.slåSammenLike
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilDagEllerFørsteDagIPerioden
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilLocalDateEllerNull
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype

typealias AktørId = String

/**
 * Vi ønsker å ha en kombinert tidslinje med alle innvilgede perioder og ikke-innvilgede som sammenfaller på dato.
 *
 * I tillegg vil vi ha ikke-innvilgede perioder som ikke sammenfaller på dato som egne tidslinjer
 *
 * Vi ønsker ikke å splitte opp perioder som ikke er innvilgede, men vi ønsker å slå dem sammen med innvilgede perioder
 * med samme fom og tom.
 *
 * Se src/test/resources/kjerne/vedtak.vedtaksperiode/VilkårPerBarnBlirSlåttSammenTilPerioderSomSkalBegrunnesIVedtak.png
 *
 * I eksempelet kan man se at alle innvilgede perioder blir slått sammen. I tillegg blir også de ikke-innvilgede * periodene med samme fom og tom slått sammen med de innvilgede periodene, men de resterende ikke-innvilgede blir * stående for seg.
 **/
fun finnPerioderSomSkalBegrunnes(
    grunnlagTidslinjePerPerson: Map<AktørId, Tidslinje<GrunnlagForPerson, Måned>>,
    grunnlagTidslinjePerPersonForrigeBehandling: Map<AktørId, Tidslinje<GrunnlagForPerson, Måned>>
): List<Periode<List<GrunnlagForGjeldendeOgForrigeBehandling?>, Måned>> {
    val gjeldendeOgForrigeGrunnlagKombinert = kombinerGjeldendeOgForrigeGrunnlag(
        grunnlagTidslinjePerPerson = grunnlagTidslinjePerPerson,
        grunnlagTidslinjePerPersonForrigeBehandling = grunnlagTidslinjePerPersonForrigeBehandling
    )

    val sammenslåtteInnvilgedePerioder = gjeldendeOgForrigeGrunnlagKombinert.utledSammenslåttePerioder()
    val ikkeInnvilgedePerioder = gjeldendeOgForrigeGrunnlagKombinert.utledIkkeinnvilgedePerioder()

    return (ikkeInnvilgedePerioder + sammenslåtteInnvilgedePerioder).slåSammenPerioderMedSammeFomOgTom()
}

private fun List<Tidslinje<GrunnlagForGjeldendeOgForrigeBehandling, Måned>>.utledSammenslåttePerioder() = this
    .map { grunnlagForDenneOgForrigeBehandlingTidslinje ->
        grunnlagForDenneOgForrigeBehandlingTidslinje.filtrer { it?.gjeldende is GrunnlagForPersonInnvilget }
    }.kombiner { if (it.toList().isNotEmpty()) it else null }
    .perioder()

private fun List<Tidslinje<GrunnlagForGjeldendeOgForrigeBehandling, Måned>>.utledIkkeinnvilgedePerioder() = this
    .map { grunnlagForDenneOgForrigeBehandlingTidslinje ->
        grunnlagForDenneOgForrigeBehandlingTidslinje.filtrer {
            val gjeldendeErIkkeInnvilget = it?.gjeldende is GrunnlagForPersonIkkeInnvilget
            val gjeldendeErNullForrigeErInnvilget = it?.gjeldende == null && it?.forrige is GrunnlagForPersonInnvilget

            gjeldendeErIkkeInnvilget || gjeldendeErNullForrigeErInnvilget
        }
    }.map { grunnlagForPersonTidslinje -> grunnlagForPersonTidslinje.map { listOf(it) } }
    .flatMap { it.perioder() }

/**
 * Ønsker å dra med informasjon om forrige behandling i perioder der forrige behandling var oppfylt, men gjeldende
 * ikke er det.
 **/
private fun kombinerGjeldendeOgForrigeGrunnlag(
    grunnlagTidslinjePerPerson: Map<AktørId, Tidslinje<GrunnlagForPerson, Måned>>,
    grunnlagTidslinjePerPersonForrigeBehandling: Map<AktørId, Tidslinje<GrunnlagForPerson, Måned>>
): List<Tidslinje<GrunnlagForGjeldendeOgForrigeBehandling, Måned>> =
    grunnlagTidslinjePerPerson.map { (aktørId, grunnlagstidslinje) ->
        val grunnlagForrigeBehandling = grunnlagTidslinjePerPersonForrigeBehandling[aktørId]

        grunnlagstidslinje.kombinerMed(grunnlagForrigeBehandling ?: TomTidslinje()) { gjeldende, forrige ->
            val gjeldendeErIkkeOppfylt = gjeldende !is GrunnlagForPersonInnvilget
            val forrigeErOppfylt = forrige is GrunnlagForPersonInnvilget

            if (gjeldendeErIkkeOppfylt && forrigeErOppfylt) {
                GrunnlagForGjeldendeOgForrigeBehandling(gjeldende, forrige)
            } else {
                GrunnlagForGjeldendeOgForrigeBehandling(gjeldende, null)
            }
        }.slåSammenLike()
    }

fun genererVedtaksperioder(
    grunnlagForVedtakPerioder: GrunnlagForVedtaksperioder,
    grunnlagForVedtakPerioderForrigeBehandling: GrunnlagForVedtaksperioder?,
    vedtak: Vedtak
): List<VedtaksperiodeMedBegrunnelser> {
    val grunnlagTidslinjePerPerson = grunnlagForVedtakPerioder.utledGrunnlagTidslinjePerPerson()

    val grunnlagTidslinjePerPersonForrigeBehandling =
        grunnlagForVedtakPerioderForrigeBehandling
            ?.let { grunnlagForVedtakPerioderForrigeBehandling.utledGrunnlagTidslinjePerPerson() }
            ?: emptyMap()

    val perioderSomSkalBegrunnesBasertPåDenneOgForrigeBehandling =
        finnPerioderSomSkalBegrunnes(grunnlagTidslinjePerPerson, grunnlagTidslinjePerPersonForrigeBehandling)

    return perioderSomSkalBegrunnesBasertPåDenneOgForrigeBehandling.map { it.tilVedtaksperiodeMedBegrunnelser(vedtak) }
}

fun Periode<List<GrunnlagForGjeldendeOgForrigeBehandling?>, Måned>.tilVedtaksperiodeMedBegrunnelser(
    vedtak: Vedtak
) = VedtaksperiodeMedBegrunnelser(
    vedtak = vedtak,
    fom = fraOgMed.tilDagEllerFørsteDagIPerioden().tilLocalDate(),
    tom = tilOgMed.tilLocalDateEllerNull(),
    type = if (innhold?.any { it?.gjeldende is GrunnlagForPersonInnvilget } == true) {
        Vedtaksperiodetype.UTBETALING
    } else {
        Vedtaksperiodetype.OPPHØR
    }
)

private fun <T> List<Periode<out Iterable<T>, Måned>>.slåSammenPerioderMedSammeFomOgTom() =
    this.groupBy { Pair(it.fraOgMed, it.tilOgMed) }
        .map {
            Periode(
                fraOgMed = it.key.first,
                tilOgMed = it.key.second,
                innhold = it.value.mapNotNull { periode -> periode.innhold }.flatten()
            )
        }






