import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinjerPerPerson
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrer
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNull
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.tilSplittkriterierTidslinje

fun hentPerioderMedUtbetaling(
    andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
    vedtak: Vedtak,
    personResultater: Set<PersonResultat>
): List<VedtaksperiodeMedBegrunnelser> {
    val splittkriterierForVedtaksperiodeTidslinje = personResultater.tilSplittkriterierTidslinje()

    val alleAndelerKombinertTidslinje = andelerTilkjentYtelse
        .tilTidslinjerPerPerson().values
        .kombinerUtenNull { it }
        .filtrer { !it?.toList().isNullOrEmpty() }

    val andelerSplittetOppTidslinje = alleAndelerKombinertTidslinje
        .kombinerMed(splittkriterierForVedtaksperiodeTidslinje) { andelerTilkjentYtelseIPeriode, utdypendeVilkårIPeriode ->
            when (andelerTilkjentYtelseIPeriode) {
                null -> null
                else -> Pair(andelerTilkjentYtelseIPeriode, utdypendeVilkårIPeriode)
            }
        }
        .filtrerIkkeNull()

    return andelerSplittetOppTidslinje
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

private data class SplittkriterierForVedtaksperiode(
    val utdypendeVilkårsvurderinger: Set<UtdypendeVilkårsvurdering>,
    val regelverk: Regelverk?
)

private fun Map<Aktør, Tidslinje<List<VilkårResultat>, Måned>>.tilSplittkriterierForVedtaksperiodeTidslinjer():
    List<Tidslinje<Pair<Aktør, SplittkriterierForVedtaksperiode>?, Måned>> =
    this.map { (aktør, vilkårsvurderingTidslinje) ->
        vilkårsvurderingTidslinje.map { vilkårResultater ->
            vilkårResultater?.let {
                Pair(
                    aktør,
                    SplittkriterierForVedtaksperiode(
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
                regelverk != acc -> throw Feil("Mer enn ett regelverk på person i periode: $regelverk, $acc")
                else -> acc
            }
        }
