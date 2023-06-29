import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.outerJoin
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonthEllerUendeligFortid
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.GrunnlagForPerson
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.GrunnlagForPersonIkkeInnvilget
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.GrunnlagForPersonInnvilget
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.GrunnlagForVedtaksperioder
import java.time.LocalDate

fun utledEndringstidspunkt(
    grunnlagForVedtaksperioder: GrunnlagForVedtaksperioder,
    grunnlagForVedtaksperioderForrigeBehandling: GrunnlagForVedtaksperioder?,
): LocalDate {
    val grunnlagTidslinjePerPerson =
        grunnlagForVedtaksperioder.utledGrunnlagTidslinjePerPerson().mapValues { it.value.grunnlagForPerson }
    val grunnlagTidslinjePerPersonForrigeBehandling =
        grunnlagForVedtaksperioderForrigeBehandling?.utledGrunnlagTidslinjePerPerson()
            ?.mapValues { it.value.grunnlagForPerson } ?: emptyMap()

    val erPeriodeLikSammePeriodeIForrigeBehandlingTidslinjer =
        grunnlagTidslinjePerPerson.outerJoin(grunnlagTidslinjePerPersonForrigeBehandling) { grunnlagForVedtaksperiode, grunnlagForVedtaksperiodeForrigeBehandling ->
            grunnlagForVedtaksperiode.erLik(grunnlagForVedtaksperiodeForrigeBehandling)
        }

    return erPeriodeLikSammePeriodeIForrigeBehandlingTidslinjer.finnTidligsteForskjell() ?: TIDENES_ENDE
}

private fun Map<Aktør, Tidslinje<Boolean, Måned>>.finnTidligsteForskjell() = this
    .minOfOrNull { (_, erPeriodeLikTidslinje) ->
        erPeriodeLikTidslinje.perioder()
            .filter { it.innhold == false }
            .minOfOrNull { it.fraOgMed.tilYearMonthEllerUendeligFortid().førsteDagIInneværendeMåned() } ?: TIDENES_ENDE
    }

private fun GrunnlagForPerson?.erLik(
    grunnlagForVedtaksperiodeForrigeBehandling: GrunnlagForPerson?,
): Boolean = when (this) {
    is GrunnlagForPersonInnvilget ->
        grunnlagForVedtaksperiodeForrigeBehandling is GrunnlagForPersonInnvilget &&
            this.vilkårResultaterForVedtaksperiode.toSet() == grunnlagForVedtaksperiodeForrigeBehandling.vilkårResultaterForVedtaksperiode.toSet() &&
            this.kompetanse == grunnlagForVedtaksperiodeForrigeBehandling.kompetanse &&
            this.endretUtbetalingAndel == grunnlagForVedtaksperiodeForrigeBehandling.endretUtbetalingAndel &&
            this.overgangsstønad == grunnlagForVedtaksperiodeForrigeBehandling.overgangsstønad

    is GrunnlagForPersonIkkeInnvilget ->
        grunnlagForVedtaksperiodeForrigeBehandling is GrunnlagForPersonIkkeInnvilget &&
            this.vilkårResultaterForVedtaksperiode.toSet() == grunnlagForVedtaksperiodeForrigeBehandling.vilkårResultaterForVedtaksperiode.toSet()

    null -> true
}
