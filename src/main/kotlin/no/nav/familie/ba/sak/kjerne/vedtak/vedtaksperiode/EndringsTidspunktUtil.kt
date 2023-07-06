import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.innholdForTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.outerJoin
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunktEllerUendeligSent
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonthEllerUendeligFortid
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.logger
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.GrunnlagForPerson
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.GrunnlagForPersonIkkeInnvilget
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.GrunnlagForPersonInnvilget
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.GrunnlagForVedtaksperioder
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.sammenlignUtenFomOgTom
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

    val (aktørMedFørsteEndring: Aktør?, datoTidligsteForskjell: LocalDate) =
        erPeriodeLikSammePeriodeIForrigeBehandlingTidslinjer.finnTidligsteForskjell() ?: Pair(null, TIDENES_ENDE)

    loggEndringstidspunktOgEndringer(
        grunnlagTidslinjePerPerson = grunnlagTidslinjePerPerson,
        grunnlagTidslinjePerPersonForrigeBehandling = grunnlagTidslinjePerPersonForrigeBehandling,
        aktørMedFørsteForandring = aktørMedFørsteEndring,
        datoTidligsteForskjell = datoTidligsteForskjell,
    )

    return datoTidligsteForskjell
}

private fun loggEndringstidspunktOgEndringer(
    grunnlagTidslinjePerPerson: Map<Aktør, Tidslinje<GrunnlagForPerson, Måned>>,
    grunnlagTidslinjePerPersonForrigeBehandling: Map<Aktør, Tidslinje<GrunnlagForPerson, Måned>>,
    aktørMedFørsteForandring: Aktør?,
    datoTidligsteForskjell: LocalDate,
) {
    val grunnlagDenneBehandlingen = grunnlagTidslinjePerPerson[aktørMedFørsteForandring]
    val grunnlagForrigeBehandling = grunnlagTidslinjePerPersonForrigeBehandling[aktørMedFørsteForandring]

    val grunnlagIPeriodeMedEndring = grunnlagDenneBehandlingen?.innholdForTidspunkt(
        datoTidligsteForskjell.toYearMonth().tilTidspunktEllerUendeligSent(),
    )?.innhold
    val grunnlagIPeriodeMedEndringForrigeBehanlding = grunnlagForrigeBehandling?.innholdForTidspunkt(
        datoTidligsteForskjell.toYearMonth().tilTidspunktEllerUendeligSent(),
    )?.innhold

    val endringer = mutableListOf<String>()

    when (grunnlagIPeriodeMedEndring) {
        is GrunnlagForPersonInnvilget -> {
            if (grunnlagIPeriodeMedEndringForrigeBehanlding is GrunnlagForPersonInnvilget) {
                if (grunnlagIPeriodeMedEndring.vilkårResultaterForVedtaksperiode.sammenlignUtenFomOgTom(
                        grunnlagIPeriodeMedEndringForrigeBehanlding.vilkårResultaterForVedtaksperiode,
                    )
                ) {
                    endringer.add("Endring i vilkårene")
                }
                if (grunnlagIPeriodeMedEndring.kompetanse == grunnlagIPeriodeMedEndringForrigeBehanlding.kompetanse) {
                    endringer.add("Endring i kompetansen")
                }
                if (grunnlagIPeriodeMedEndring.endretUtbetalingAndel == grunnlagIPeriodeMedEndringForrigeBehanlding.endretUtbetalingAndel) {
                    endringer.add("Endring i de endrede utbetalingene")
                }
                if (grunnlagIPeriodeMedEndring.overgangsstønad == grunnlagIPeriodeMedEndringForrigeBehanlding.overgangsstønad) {
                    endringer.add("Endring i overgangsstønaden")
                }
                if (grunnlagIPeriodeMedEndring.andeler.toSet() == grunnlagIPeriodeMedEndringForrigeBehanlding.andeler.toSet()) {
                    endringer.add("Endring i andelene")
                }
            } else {
                endringer.add("Perioden var ikke innvilget i forrige behandling")
            }
        }

        is GrunnlagForPersonIkkeInnvilget ->
            if (grunnlagIPeriodeMedEndringForrigeBehanlding is GrunnlagForPersonIkkeInnvilget) {
                if (grunnlagIPeriodeMedEndring.vilkårResultaterForVedtaksperiode.sammenlignUtenFomOgTom(
                        grunnlagIPeriodeMedEndringForrigeBehanlding.vilkårResultaterForVedtaksperiode,
                    )
                ) {
                    endringer.add("Endring i vilkårene")
                }
            } else {
                endringer.add("Perioden var innvilget i forrige behandling, men er det ikke nå lenger")
            }

        null -> endringer.add("Det er ingen vilkår på denne behandlingen i dette tidsrommet")
    }

    logger.info(
        "Endringstidspunktet for behandlingen er $datoTidligsteForskjell. Se Secure logs for å se hvem endringen er for. Dette er endringene:\n" +
            endringer.joinToString("\n"),
    )
    secureLogger.info("Ved endringstidspunktet $datoTidligsteForskjell er det endring for $aktørMedFørsteForandring")
}

private fun Map<Aktør, Tidslinje<Boolean, Måned>>.finnTidligsteForskjell() = this
    .map { (aktør, erPeriodeLikTidslinje) ->
        val førsteEndringForAktør = erPeriodeLikTidslinje.perioder()
            .filter { it.innhold == false }
            .minOfOrNull { it.fraOgMed.tilYearMonthEllerUendeligFortid().førsteDagIInneværendeMåned() }
            ?: TIDENES_ENDE

        aktør to førsteEndringForAktør
    }.minByOrNull { it.second }

private fun GrunnlagForPerson?.erLik(
    grunnlagForVedtaksperiodeForrigeBehandling: GrunnlagForPerson?,
): Boolean = when (this) {
    is GrunnlagForPersonInnvilget ->
        grunnlagForVedtaksperiodeForrigeBehandling is GrunnlagForPersonInnvilget &&
            this.vilkårResultaterForVedtaksperiode.sammenlignUtenFomOgTom(
                grunnlagForVedtaksperiodeForrigeBehandling.vilkårResultaterForVedtaksperiode,
            ) &&
            this.kompetanse == grunnlagForVedtaksperiodeForrigeBehandling.kompetanse &&
            this.endretUtbetalingAndel == grunnlagForVedtaksperiodeForrigeBehandling.endretUtbetalingAndel &&
            this.overgangsstønad == grunnlagForVedtaksperiodeForrigeBehandling.overgangsstønad &&
            this.andeler.toSet() == grunnlagForVedtaksperiodeForrigeBehandling.andeler.toSet()

    is GrunnlagForPersonIkkeInnvilget ->
        grunnlagForVedtaksperiodeForrigeBehandling is GrunnlagForPersonIkkeInnvilget &&
            this.vilkårResultaterForVedtaksperiode.toSet() == grunnlagForVedtaksperiodeForrigeBehandling.vilkårResultaterForVedtaksperiode.toSet()

    null -> true
}
