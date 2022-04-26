package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.lagKompetanse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeMedBegrunnelser.slåSammenUtbetalingsperioderMedKompetanse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class UtbetalingsperiodeMedBegrunnelserUtilsTest {

    @Test
    fun `skal splitte opp utbetalingsperioder når det er overlappende kompetanse`() {

        val start = LocalDate.of(2021, 1, 1)
        val split1 = LocalDate.of(2021, 2, 1)
        val split2 = LocalDate.of(2021, 5, 1)
        val split3 = LocalDate.of(2021, 6, 1)
        val split4 = LocalDate.of(2021, 8, 1)

        val utbetalingsperiode1 = lagVedtaksperiodeMedBegrunnelser(
            type = Vedtaksperiodetype.UTBETALING,
            fom = start,
            tom = LocalDate.of(2021, 3, 1).sisteDagIMåned()
        )

        val kompetanse1 = lagKompetanse(fom = split1.toYearMonth(), tom = YearMonth.of(2021, 3))

        val utbetalingsperiode2 = lagVedtaksperiodeMedBegrunnelser(
            type = Vedtaksperiodetype.UTBETALING,
            fom = split2,
            tom = LocalDate.of(2021, 7, 1).sisteDagIMåned()
        )

        val kompetanse2 = lagKompetanse(fom = split3.toYearMonth(), tom = YearMonth.of(2021, 9))

        val utbetalingsperiodeMedReduksjonFraSistIverksatteBehandling = lagVedtaksperiodeMedBegrunnelser(
            type = Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING,
            fom = split4,
            tom = LocalDate.of(2021, 10, 1).sisteDagIMåned()
        )

        val utbetalingsperiodeMedKompetanseSplitter = slåSammenUtbetalingsperioderMedKompetanse(
            utbetalingsperioder = listOf(
                utbetalingsperiode1,
                utbetalingsperiode2,
                utbetalingsperiodeMedReduksjonFraSistIverksatteBehandling
            ),
            kompetanser = listOf(kompetanse1, kompetanse2)
        )

        val splitter = listOf(start, split1, split2, split3, split4)

        Assertions.assertEquals(5, utbetalingsperiodeMedKompetanseSplitter.size)
        utbetalingsperiodeMedKompetanseSplitter.forEachIndexed { index, vedtaksperiodeMedBegrunnelse ->
            Assertions.assertEquals(
                splitter[index],
                vedtaksperiodeMedBegrunnelse.fom
            )
        }
    }
}
