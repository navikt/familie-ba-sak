package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeMedBegrunnelser

import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.lagKompetanse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.YearMonth

class UtbetalingsperiodeMedBegrunnelserUtilsTest {

    @Test
    fun `skal splitte opp utbetalingsperioder når det er overlappende kompetanse`() {

        val periode1 = MånedPeriode(YearMonth.of(2021, 1), YearMonth.of(2021, 1))
        val periode2 = MånedPeriode(YearMonth.of(2021, 2), YearMonth.of(2021, 3))
        val periode3 = MånedPeriode(YearMonth.of(2021, 5), YearMonth.of(2021, 5))
        val periode4 = MånedPeriode(YearMonth.of(2021, 6), YearMonth.of(2021, 7))
        val periode5 = MånedPeriode(YearMonth.of(2021, 8), YearMonth.of(2021, 10))

        val utbetalingsperiode1 = lagVedtaksperiodeMedBegrunnelser(
            type = Vedtaksperiodetype.UTBETALING,
            fom = periode1.fom.førsteDagIInneværendeMåned(),
            tom = periode2.tom.atEndOfMonth(),
        )

        val kompetanse1 = lagKompetanse(fom = periode2.fom, tom = periode2.tom)

        val utbetalingsperiode2 = lagVedtaksperiodeMedBegrunnelser(
            type = Vedtaksperiodetype.UTBETALING,
            fom = periode3.fom.førsteDagIInneværendeMåned(),
            tom = periode4.tom.atEndOfMonth()
        )

        val kompetanse2 = lagKompetanse(fom = periode4.fom, tom = periode5.tom)

        val utbetalingsperiodeMedReduksjonFraSistIverksatteBehandling = lagVedtaksperiodeMedBegrunnelser(
            type = Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING,
            fom = periode5.fom.førsteDagIInneværendeMåned(),
            tom = periode5.tom.atEndOfMonth()
        )

        val utbetalingsperiodeMedKompetanseSplitter = slåSammenUtbetalingsperioderMedKompetanse(
            utbetalingsperioder = listOf(
                utbetalingsperiode1,
                utbetalingsperiode2,
                utbetalingsperiodeMedReduksjonFraSistIverksatteBehandling
            ),
            kompetanser = listOf(kompetanse1, kompetanse2)
        )

        val forventedePerioder =
            listOf(periode1, periode2, periode3, periode4, periode5)

        Assertions.assertEquals(5, utbetalingsperiodeMedKompetanseSplitter.size)
        utbetalingsperiodeMedKompetanseSplitter
            .zip(forventedePerioder)
            .forEach { (utbetalingsperiode, forventetPeriode) ->
                Assertions.assertEquals(forventetPeriode.fom.førsteDagIInneværendeMåned(), utbetalingsperiode.fom)
                Assertions.assertEquals(forventetPeriode.tom.atEndOfMonth(), utbetalingsperiode.tom)
            }
    }
}
