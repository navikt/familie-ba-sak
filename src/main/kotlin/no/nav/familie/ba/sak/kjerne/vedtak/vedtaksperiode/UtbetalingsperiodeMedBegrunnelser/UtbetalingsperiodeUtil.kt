import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinjerPerPerson
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinjerPerPersonOgType
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
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.tilTidslinjeForSplittForPerson
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
                fom = it.fraOgMed.tilYearMonthEllerNull()?.førsteDagIInneværendeMåned(),
                tom = it.tilOgMed.tilYearMonthEllerNull()?.sisteDagIInneværendeMåned(),
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
    val splittTidslinjerPerPerson =
        personResultater.associate { it.aktør to it.tilTidslinjeForSplittForPerson(fødselsdato = personerOgFødselsdatoer[it.aktør]) }

    val andelerTidslinjerPerPerson = andelerTilkjentYtelse
        .tilTidslinjerPerPerson()

    val andelerSplittetOppTidslinjer = andelerTidslinjerPerPerson
        .leftJoin(splittTidslinjerPerPerson) { andelerIPeriode, splittVilkårIPeriode ->
            when (andelerIPeriode) {
                null -> null
                else -> Pair(andelerIPeriode, splittVilkårIPeriode)
            }
        }.map { (_, tidslinje) -> tidslinje.filtrerIkkeNull().slåSammenLike() }

    val kombinertTidslinje = andelerSplittetOppTidslinjer
        .kombinerUtenNull { it }.filtrer { !it?.toList().isNullOrEmpty() }

    return kombinertTidslinje
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
