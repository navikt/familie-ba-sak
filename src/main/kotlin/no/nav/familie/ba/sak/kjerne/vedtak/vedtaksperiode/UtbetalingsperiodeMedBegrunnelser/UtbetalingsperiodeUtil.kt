import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinjerPerPersonOgType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrer
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.leftJoin
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.slåSammenLike
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilFørsteDagIMåneden
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilLocalDateEllerNull
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilSisteDagIMåneden
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.tilTidslinjeForSplitt
import java.time.LocalDate

@Deprecated("Erstattes av hentPerioderMedUtbetaling")
fun hentPerioderMedUtbetalingDeprecated(
    andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
    vedtak: Vedtak,
    forskjøvetVilkårResultatTidslinjeMap: Map<Aktør, Tidslinje<List<VilkårResultat>, Måned>>
): List<VedtaksperiodeMedBegrunnelser> {
    val splittkriterierForVedtaksperiodeTidslinje =
        forskjøvetVilkårResultatTidslinjeMap
            .tilSplittkriterierForVedtaksperiodeTidslinjer()
            .kombinerUtenNull { it.filterNotNull().toMap() }
            .filtrer { !it.isNullOrEmpty() }
            .slåSammenLike()

    return andelerTilkjentYtelse
        .tilTidslinjerPerPersonOgType().values
        .kombinerUtenNull { it }
        .filtrer { !it?.toList().isNullOrEmpty() }
        .leftJoin(splittkriterierForVedtaksperiodeTidslinje) { andelerTilkjentYtelseIPeriode, utdypendeVilkårIPeriode ->
            Pair(andelerTilkjentYtelseIPeriode, utdypendeVilkårIPeriode)
        }
        .filtrerIkkeNull()
        .perioder()
        .map {
            VedtaksperiodeMedBegrunnelser(
                fom = it.fraOgMed.tilFørsteDagIMåneden().tilLocalDateEllerNull(),
                tom = it.tilOgMed.tilSisteDagIMåneden().tilLocalDateEllerNull(),
                vedtak = vedtak,
                type = Vedtaksperiodetype.UTBETALING
            )
        }
}

fun hentPerioderMedUtbetaling(
    andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
    vedtak: Vedtak,
    personResultater: Set<PersonResultat>,
    personerOgFødselsdatoer: Map<Aktør, LocalDate>
): List<VedtaksperiodeMedBegrunnelser> {
    val tidslinjeForSplitt = personResultater.tilTidslinjeForSplitt(personerOgFødselsdatoer)

    val alleAndelerKombinertTidslinje = andelerTilkjentYtelse
        .tilTidslinjerPerPersonOgType().values
        .kombinerUtenNull { it }
        .filtrer { !it?.toList().isNullOrEmpty() }

    val andelerSplittetOppTidslinje =
        alleAndelerKombinertTidslinje.kombinerMed(tidslinjeForSplitt) { andelerIPeriode, splittVilkårIPeriode ->
            when (andelerIPeriode) {
                null -> null
                else -> Pair(andelerIPeriode, splittVilkårIPeriode)
            }
        }.filtrerIkkeNull()

    return andelerSplittetOppTidslinje
        .perioder()
        .map {
            VedtaksperiodeMedBegrunnelser(
                fom = it.fraOgMed.tilFørsteDagIMåneden().tilLocalDateEllerNull(),
                tom = it.tilOgMed.tilSisteDagIMåneden().tilLocalDateEllerNull(),
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
