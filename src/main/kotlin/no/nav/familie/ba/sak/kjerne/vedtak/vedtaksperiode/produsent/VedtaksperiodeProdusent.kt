package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrer
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
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
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.mapIkkeNull
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat

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

    val søker = grunnlagForVedtakPerioder.persongrunnlag.søker
    val ordinæreVilkårForSøkerForskjøvetTidslinje =
        hentOrdinæreVilkårForSøkerForskjøvetTidslinje(søker, grunnlagForVedtakPerioder.personResultater)

    val perioderSomSkalBegrunnesBasertPåDenneOgForrigeBehandling =
        finnPerioderSomSkalBegrunnes(
            grunnlagTidslinjePerPerson = grunnlagTidslinjePerPerson,
            grunnlagTidslinjePerPersonForrigeBehandling = grunnlagTidslinjePerPersonForrigeBehandling,
            vilkårResultaterSøker = ordinæreVilkårForSøkerForskjøvetTidslinje,
            søker = søker
        )

    return perioderSomSkalBegrunnesBasertPåDenneOgForrigeBehandling.map { it.tilVedtaksperiodeMedBegrunnelser(vedtak) }
}

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
    grunnlagTidslinjePerPersonForrigeBehandling: Map<AktørId, Tidslinje<GrunnlagForPerson, Måned>>,
    vilkårResultaterSøker: Tidslinje<List<VilkårResultat>, Måned>,
    søker: Person
): List<Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>, Måned>> {
    val gjeldendeOgForrigeGrunnlagKombinert = kombinerGjeldendeOgForrigeGrunnlag(
        grunnlagTidslinjePerPerson = grunnlagTidslinjePerPerson,
        grunnlagTidslinjePerPersonForrigeBehandling = grunnlagTidslinjePerPersonForrigeBehandling
    )

    val sammenslåttePerioderUtenEksplisittAvslag =
        gjeldendeOgForrigeGrunnlagKombinert.slåSammenUtenEksplisitteAvslag()
    val eksplisitteAvslagsperioder =
        gjeldendeOgForrigeGrunnlagKombinert.utledEksplisitteAvslagsperioder()

    val perioderSomSkalBegrunnesUtenSøkerSineAvslag =
        (eksplisitteAvslagsperioder + sammenslåttePerioderUtenEksplisittAvslag)
            .slåSammenAvslagOgReduksjonsperioderMedSammeFomOgTom()

    return perioderSomSkalBegrunnesUtenSøkerSineAvslag.fyllInnSøkerSineAvslag(vilkårResultaterSøker, søker)
}

private fun List<Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>, Måned>>.fyllInnSøkerSineAvslag(
    vilkårResultaterSøker: Tidslinje<List<VilkårResultat>, Måned>,
    søker: Person
): List<Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>, Måned>> {
    val vilkårResultaterSøkerAvslagTidslinje = vilkårResultaterSøker
        .mapIkkeNull { it.tilVilkårResultaterForVedtaksperiode() }
        .filtrerIkkeNull { vilkårResultaterIPeriode -> vilkårResultaterIPeriode.any { it.erEksplisittAvslagPåSøknad == true } }
        .mapIkkeNull { vilkårResultaterIPeriode -> vilkårResultaterIPeriode.filter { it.erEksplisittAvslagPåSøknad == true } }

    val grunnlagForSøkersOrdinæreVilkårMedAvslag = vilkårResultaterSøkerAvslagTidslinje.mapIkkeNull {
        GrunnlagForGjeldendeOgForrigeBehandling(
            gjeldende = GrunnlagForPersonIkkeInnvilget(
                person = søker,
                vilkårResultaterForVedtaksperiode = it
            ),
            personHarRettIForrigeBehandling = null
        )
    }

    return fold(grunnlagForSøkersOrdinæreVilkårMedAvslag.mapIkkeNull { listOf(it) }) { acc, periode ->
        acc.kombinerMed(listOf(periode).tilTidslinje()) { grunnlagSøker, grunnlagBarn ->
            if (grunnlagSøker == null) {
                grunnlagBarn
            } else {
                val grunnlagForBarnMedEksplisitteAvslag = grunnlagBarn
                    ?.map { grunnlagForGjeldendeOgForrigeBehandling ->
                        fjernVilkårResultaterSomIkkeErEksplisittAvslag(
                            grunnlagForGjeldendeOgForrigeBehandling,
                            søker
                        )
                    }
                    ?.filter { it.gjeldende?.erEksplisittAvslag() == true }
                    .takeIf { !it.isNullOrEmpty() } ?: emptyList()

                grunnlagSøker + grunnlagForBarnMedEksplisitteAvslag
            }
        }
    }.perioder().toList()
}

private fun fjernVilkårResultaterSomIkkeErEksplisittAvslag(
    grunnlagForGjeldendeOgForrigeBehandling: GrunnlagForGjeldendeOgForrigeBehandling,
    søker: Person
): GrunnlagForGjeldendeOgForrigeBehandling {
    val gjeldendeGrunnlagForPerson = grunnlagForGjeldendeOgForrigeBehandling.gjeldende

    return grunnlagForGjeldendeOgForrigeBehandling.copy(
        gjeldende = gjeldendeGrunnlagForPerson
            ?.kopier(
                vilkårResultaterForVedtaksperiode = gjeldendeGrunnlagForPerson.vilkårResultaterForVedtaksperiode
                    .filter {
                        it.erEksplisittAvslagPåSøknad == true &&
                            (it.aktørId != søker.aktør.aktørId || it.vilkårType == Vilkår.UTVIDET_BARNETRYGD)
                    }
            )
    )
}

private fun List<VilkårResultat>.tilVilkårResultaterForVedtaksperiode(): List<VilkårResultatForVedtaksperiode> =
    this.map { VilkårResultatForVedtaksperiode(it) }

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

    return kombinerteAvslagOgReduksjonsperioder.kombiner { it.toList().takeIf { it.isNotEmpty() } }.perioder()
}

private fun List<Tidslinje<GrunnlagForGjeldendeOgForrigeBehandling, Måned>>.utledEksplisitteAvslagsperioder(): Collection<Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>, Måned>> =
    this.map { grunnlagForDenneOgForrigeBehandlingTidslinje ->
        grunnlagForDenneOgForrigeBehandlingTidslinje.filtrer {
            it?.gjeldende?.erEksplisittAvslag() == true
        }
    }.map { grunnlagForPersonTidslinje ->
        grunnlagForPersonTidslinje.map { it?.let { listOf(it) } }
    }.flatMap { it.perioder() }

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
                GrunnlagForGjeldendeOgForrigeBehandling(gjeldende, true)
            } else {
                gjeldende?.let { GrunnlagForGjeldendeOgForrigeBehandling(gjeldende, false) }
            }
        }.slåSammenLike()
    }

fun Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>, Måned>.tilVedtaksperiodeMedBegrunnelser(
    vedtak: Vedtak
) = VedtaksperiodeMedBegrunnelser(
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
    }
)

data class GrupperingskriterierForVedtaksperioder(
    val fom: Tidspunkt<Måned>,
    val tom: Tidspunkt<Måned>,
    val periodeInneholderInnvilgelse: Boolean
)

private fun List<Periode<List<GrunnlagForGjeldendeOgForrigeBehandling>, Måned>>.slåSammenAvslagOgReduksjonsperioderMedSammeFomOgTom() =
    this.groupBy { periode ->
        GrupperingskriterierForVedtaksperioder(
            fom = periode.fraOgMed,
            tom = periode.tilOgMed,
            periodeInneholderInnvilgelse = periode.innhold?.any { it.gjeldende is GrunnlagForPersonInnvilget } == true
        )
    }.map { (grupperingskriterier, verdi) ->
        Periode(
            fraOgMed = grupperingskriterier.fom,
            tilOgMed = grupperingskriterier.tom,
            innhold = verdi.mapNotNull { periode -> periode.innhold }.flatten()
        )
    }
