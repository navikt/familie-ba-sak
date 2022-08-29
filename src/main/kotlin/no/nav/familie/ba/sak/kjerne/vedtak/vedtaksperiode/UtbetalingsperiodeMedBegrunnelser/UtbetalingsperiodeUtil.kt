
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
            .tilUtdypendeVilkårsvurderingTidslinjer()
            .kombinerUtenNull { it.toMap() }
            .filtrer { !it.isNullOrEmpty() }
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

private fun Map<Aktør, Tidslinje<Iterable<VilkårResultat>, Måned>>.tilUtdypendeVilkårsvurderingTidslinjer():
    List<Tidslinje<Pair<Aktør, Set<UtdypendeVilkårsvurdering>>, Måned>> =
    this.map { (aktør, vilkårsvurderingTidslinje) ->
        vilkårsvurderingTidslinje.map { vilkårResultater ->
            Pair(
                aktør,
                vilkårResultater?.flatMap { it.utdypendeVilkårsvurderinger }?.toSet() ?: emptySet()
            )
        }
    }
