import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.lagVertikaleSegmenter
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinjerPerPerson
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrer
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.leftJoin
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.slåSammenLike
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat

@Deprecated("Skal utfases. Bruk hentPerioderMedUtbetaling")
fun hentPerioderMedUtbetalingGammel(
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    vedtak: Vedtak
) = andelerTilkjentYtelse.lagVertikaleSegmenter()
    .map { (segmenter, _) ->
        VedtaksperiodeMedBegrunnelser(
            fom = segmenter.fom,
            tom = segmenter.tom,
            vedtak = vedtak,
            type = Vedtaksperiodetype.UTBETALING
        )
    }

fun hentPerioderMedUtbetaling(
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    vedtak: Vedtak,
    forskjøvetVilkårResultatTidslinjeMap: Map<Aktør, Tidslinje<Iterable<VilkårResultat>, Måned>>
): List<VedtaksperiodeMedBegrunnelser> {
    val utdypendeVilkårsvurderingTidslinje =
        forskjøvetVilkårResultatTidslinjeMap
            // Slår sammen vilkårResultatene og beholder kun dataene vi trenger
            .tilVilkårResulaterKombinertPerAktørTidslinjer()
            // Slår sammen tidslinjene til en tidslijne med map av aktør til VilkårResulaterKombinert
            .kombinerUtenNull { it.filterNotNull().toMap() }
            .filtrer { !it.isNullOrEmpty() }
            // Slår sammen like perioder
            .slåSammenLike()

    return andelerTilkjentYtelse
        .tilTidslinjerPerPerson().values
        .kombinerUtenNull { it }
        .filtrer { !it?.toList().isNullOrEmpty() }
        .leftJoin(utdypendeVilkårsvurderingTidslinje) { andelerTilkjentYtelseIPeriode, utdypendeVilkårIPeriode ->
            Pair(andelerTilkjentYtelseIPeriode, utdypendeVilkårIPeriode)
        }
        .filtrerIkkeNull()
        .perioder()
        .map {
            VedtaksperiodeMedBegrunnelser(
                fom = it.fraOgMed.tilYearMonthEllerNull()?.førsteDagIInneværendeMåned(),
                tom = it.tilOgMed.tilYearMonthEllerNull()?.sisteDagIInneværendeMåned(),
                vedtak = vedtak,
                type = Vedtaksperiodetype.UTBETALING
            )
        }
}

private data class VilkårResulaterKombinert(
    val utdypendeVilkårsvurderinger: Set<UtdypendeVilkårsvurdering>,
    val regelverk: Regelverk?
)

private fun Map<Aktør, Tidslinje<Iterable<VilkårResultat>, Måned>>.tilVilkårResulaterKombinertPerAktørTidslinjer():
    List<Tidslinje<Pair<Aktør, VilkårResulaterKombinert>?, Måned>> =
    this.map { (aktør, vilkårsvurderingTidslinje) ->
        vilkårsvurderingTidslinje.map { vilkårResultater ->
            vilkårResultater?.let {
                Pair(
                    aktør,
                    VilkårResulaterKombinert(
                        utdypendeVilkårsvurderinger = hentSetAvVilkårsVurderinger(vilkårResultater),
                        regelverk = hentRegelverkPersonErVurdertEtterIPeriode(vilkårResultater)
                    )
                )
            }
        }
    }

private fun hentSetAvVilkårsVurderinger(vilkårResultater: Iterable<VilkårResultat>) =
    vilkårResultater.flatMap { it.utdypendeVilkårsvurderinger }.toSet()

private fun hentRegelverkPersonErVurdertEtterIPeriode(vilkårResultater: Iterable<VilkårResultat>) =
    vilkårResultater
        .map { it.vurderesEtter }
        .reduce { acc, regelverk ->
            when {
                acc == null -> regelverk
                regelverk == null -> acc
                regelverk != acc -> throw Feil("Mer enn ett regelverk på person i periode")
                else -> acc
            }
        }
