package no.nav.familie.ba.sak.kjerne.grunnlag.overgangsstønad

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.SmåbarnstilleggGenerator
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.slåSammenTidligerePerioder
import no.nav.familie.ba.sak.kjerne.beregning.domene.splitFramtidigePerioderFraForrigeBehandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinje
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringIUtbetalingUtil
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.kontrakter.felles.ef.EksternPeriode
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import java.time.LocalDate

fun vedtakOmOvergangsstønadPåvirkerFagsak(
    småbarnstilleggGenerator: SmåbarnstilleggGenerator,
    nyePerioderMedFullOvergangsstønad: List<InternPeriodeOvergangsstønad>,
    forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
    barnasAktørerOgFødselsdatoer: List<Pair<Aktør, LocalDate>>,
): Boolean {
    val (forrigeSmåbarnstilleggAndeler, forrigeAndelerIkkeSmåbarnstillegg) = forrigeAndelerTilkjentYtelse.partition { it.erSmåbarnstillegg() }

    val (forrigeUtvidetAndeler, forrigeBarnasAndeler) = forrigeAndelerIkkeSmåbarnstillegg.partition { it.erUtvidet() }

    val nyeSmåbarnstilleggAndeler =
        småbarnstilleggGenerator.lagSmåbarnstilleggAndeler(
            perioderMedFullOvergangsstønad = nyePerioderMedFullOvergangsstønad,
            barnasAndeler = forrigeBarnasAndeler,
            utvidetAndeler = forrigeUtvidetAndeler,
            barnasAktørerOgFødselsdatoer = barnasAktørerOgFødselsdatoer,
        )

    return nyeSmåbarnstilleggAndeler.førerTilEndringIUtbetalingFraForrigeBehandling(
        forrigeAndeler = forrigeSmåbarnstilleggAndeler,
    )
}

private fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.førerTilEndringIUtbetalingFraForrigeBehandling(
    forrigeAndeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
): Boolean {
    val endringstidslinje =
        EndringIUtbetalingUtil.lagEndringIUtbetalingTidslinje(
            nåværendeAndeler = this.map { it.andel },
            forrigeAndeler = forrigeAndeler.map { it.andel },
        )

    return endringstidslinje.tilPerioder().any { it.verdi == true }
}

fun List<InternPeriodeOvergangsstønad>.splittOgSlåSammen(
    overgangsstønadPerioderFraForrigeBehandling: List<InternPeriodeOvergangsstønad>,
    dagensDato: LocalDate,
) = this
    .slåSammenTidligerePerioder(dagensDato)
    .splitFramtidigePerioderFraForrigeBehandling(overgangsstønadPerioderFraForrigeBehandling, dagensDato)

private enum class Endring {
    INGEN_ENDRING,
    MISTET_PERIODE,
    FÅTT_PERIODE,
}

fun erEndringIOvergangsstønadFramITid(
    perioderMedFullOvergangsstønadForrigeBehandling: List<InternPeriodeOvergangsstønad>,
    perioderMedFullOvergangsstønad: List<EksternPeriode>,
    dagensDato: LocalDate,
): Boolean {
    val overgangsstønadForrigeBehandlingTidslinje = perioderMedFullOvergangsstønadForrigeBehandling.tilTidslinje()
    val overgangsstønadTidslinje = perioderMedFullOvergangsstønad.tilTidslinje()

    val endringTidslinje =
        overgangsstønadForrigeBehandlingTidslinje.kombinerMed(overgangsstønadTidslinje) { overgangsstønadFraForrigeBehandling, overgangsstønad ->
            when {
                overgangsstønadFraForrigeBehandling == null && overgangsstønad != null -> Endring.FÅTT_PERIODE
                overgangsstønadFraForrigeBehandling != null && overgangsstønad == null -> Endring.MISTET_PERIODE
                else -> Endring.INGEN_ENDRING
            }
        }

    val endringsperioder = endringTidslinje.tilPerioder().filter { it.verdi != Endring.INGEN_ENDRING }
    val erEndringFramITid = endringsperioder.all { it.fom != null && it.fom!!.toYearMonth().isAfter(dagensDato.toYearMonth().plusMonths(1)) }
    return erEndringFramITid
}

@JvmName("tilTidslinjeEksternPeriode")
private fun List<EksternPeriode>.tilTidslinje(): Tidslinje<EksternPeriode> = map { Periode(verdi = it, fom = it.fomDato, tom = it.tomDato) }.tilTidslinje()
