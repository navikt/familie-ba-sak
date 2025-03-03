package no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg

import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.datagenerator.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.YearMonth

class AutovedtakSmåbarnstilleggUtilsTest {
    @Test
    fun `Skal legge til innvilgelsesbegrunnelse for småbarnstillegg`() {
        val vedtaksperiodeMedBegrunnelser =
            lagVedtaksperiodeMedBegrunnelser(
                fom = LocalDate.now().førsteDagIInneværendeMåned(),
                tom = LocalDate.now().plusMonths(3).sisteDagIMåned(),
                type = Vedtaksperiodetype.UTBETALING,
            )

        val oppdatertVedtaksperiodeMedBegrunnelser =
            finnAktuellVedtaksperiodeOgLeggTilSmåbarnstilleggbegrunnelse(
                vedtaksperioderMedBegrunnelser =
                    listOf(
                        vedtaksperiodeMedBegrunnelser,
                    ),
                innvilgetMånedPeriode =
                    MånedPeriode(
                        fom = YearMonth.now(),
                        tom = vedtaksperiodeMedBegrunnelser.tom!!.toYearMonth(),
                    ),
                redusertMånedPeriode = null,
            )

        assertNotNull(oppdatertVedtaksperiodeMedBegrunnelser)
        assertTrue(oppdatertVedtaksperiodeMedBegrunnelser.begrunnelser.any { it.standardbegrunnelse == Standardbegrunnelse.INNVILGET_SMÅBARNSTILLEGG })
        assertTrue(oppdatertVedtaksperiodeMedBegrunnelser.begrunnelser.none { it.standardbegrunnelse == Standardbegrunnelse.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_FULL_OVERGANGSSTØNAD })
    }

    @Test
    fun `Skal legge til reduksjonsbegrunnelse for småbarnstillegg`() {
        val vedtaksperiodeMedBegrunnelser =
            lagVedtaksperiodeMedBegrunnelser(
                fom = LocalDate.now().nesteMåned().førsteDagIInneværendeMåned(),
                tom = LocalDate.now().plusMonths(3).sisteDagIMåned(),
                type = Vedtaksperiodetype.UTBETALING,
            )

        val oppdatertVedtaksperiodeMedBegrunnelser =
            finnAktuellVedtaksperiodeOgLeggTilSmåbarnstilleggbegrunnelse(
                vedtaksperioderMedBegrunnelser =
                    listOf(
                        vedtaksperiodeMedBegrunnelser,
                    ),
                innvilgetMånedPeriode = null,
                redusertMånedPeriode =
                    MånedPeriode(
                        fom = YearMonth.now().nesteMåned(),
                        tom = vedtaksperiodeMedBegrunnelser.tom!!.toYearMonth(),
                    ),
            )

        assertNotNull(oppdatertVedtaksperiodeMedBegrunnelser)
        assertTrue(oppdatertVedtaksperiodeMedBegrunnelser.begrunnelser.none { it.standardbegrunnelse == Standardbegrunnelse.INNVILGET_SMÅBARNSTILLEGG })
        assertTrue(oppdatertVedtaksperiodeMedBegrunnelser.begrunnelser.any { it.standardbegrunnelse == Standardbegrunnelse.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_FULL_OVERGANGSSTØNAD })
    }

    @Test
    fun `Skal legge til reduksjonsbegrunnelse fra inneværende måned for småbarnstillegg`() {
        val vedtaksperiodeMedBegrunnelser =
            lagVedtaksperiodeMedBegrunnelser(
                fom = LocalDate.now().førsteDagIInneværendeMåned(),
                tom = LocalDate.now().plusMonths(3).sisteDagIMåned(),
                type = Vedtaksperiodetype.UTBETALING,
            )

        val oppdatertVedtaksperiodeMedBegrunnelser =
            finnAktuellVedtaksperiodeOgLeggTilSmåbarnstilleggbegrunnelse(
                vedtaksperioderMedBegrunnelser =
                    listOf(
                        vedtaksperiodeMedBegrunnelser,
                    ),
                innvilgetMånedPeriode = null,
                redusertMånedPeriode =
                    MånedPeriode(
                        fom = YearMonth.now(),
                        tom = vedtaksperiodeMedBegrunnelser.tom!!.toYearMonth(),
                    ),
            )

        assertNotNull(oppdatertVedtaksperiodeMedBegrunnelser)
        assertTrue(oppdatertVedtaksperiodeMedBegrunnelser.begrunnelser.none { it.standardbegrunnelse == Standardbegrunnelse.INNVILGET_SMÅBARNSTILLEGG })
        assertTrue(oppdatertVedtaksperiodeMedBegrunnelser.begrunnelser.any { it.standardbegrunnelse == Standardbegrunnelse.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_FULL_OVERGANGSSTØNAD })
    }

    @Test
    fun `Skal kaste feil om det ikke finnes innvilget eller redusert periode å begrunne`() {
        val vedtaksperiodeMedBegrunnelser =
            lagVedtaksperiodeMedBegrunnelser(
                fom = LocalDate.now().nesteMåned().førsteDagIInneværendeMåned(),
                tom = LocalDate.now().plusMonths(3).sisteDagIMåned(),
                type = Vedtaksperiodetype.UTBETALING,
            )

        assertThrows<VedtaksperiodefinnerSmåbarnstilleggFeil> {
            finnAktuellVedtaksperiodeOgLeggTilSmåbarnstilleggbegrunnelse(
                vedtaksperioderMedBegrunnelser =
                    listOf(
                        vedtaksperiodeMedBegrunnelser,
                    ),
                innvilgetMånedPeriode = null,
                redusertMånedPeriode = null,
            )
        }
    }
}
