package no.nav.familie.ba.sak.integrasjoner.økonomi

import io.mockk.mockk
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.kontrakter.felles.oppdrag.Opphør
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

internal class ØkonomiServiceTest {
    val søker = randomFnr()
    val behandling = lagBehandling()
    val vedtakDato = LocalDate.now()
    val økonomiService = ØkonomiService(mockk(), mockk(), mockk(), mockk(), mockk())

    @Test
    fun `valider opphør med lovlige perioder`() {
        val fom = LocalDate.now().førsteDagIInneværendeMåned()
        val tom = LocalDate.now().sisteDagIMåned()
        val opphørDato = LocalDate.now().sisteDagIMåned()

        val utbetalingsPeriode = listOf(
            lagEksternUtbetalingsperiode(
                opphør = Opphør(opphørDato), fom = fom.minusMonths(10), tom = tom.minusMonths(8)
            ),
            lagEksternUtbetalingsperiode(
                fom = fom.minusMonths(7), tom = tom.minusMonths(6)
            ),
            lagEksternUtbetalingsperiode(
                fom = fom.minusMonths(5), tom = tom.minusMonths(4)
            ),
        )

        // Test at validering ikke feiler.
        økonomiService.validerOpphørsoppdrag(
            lagEksternUtbetalingsoppdrag(utbetalingsPeriode)
        )
    }

    @Test
    fun `valider opphør med løpende utbetalingsperioder som skal kaste feil`() {
        val fom = LocalDate.now().førsteDagIInneværendeMåned()
        val tom = LocalDate.now().sisteDagIMåned()
        val opphørDato = LocalDate.now().sisteDagIMåned()

        val utbetalingsPeriode = listOf(
            lagEksternUtbetalingsperiode(
                opphør = Opphør(opphørDato), fom = fom.minusMonths(10), tom = tom.minusMonths(8)
            ),
            lagEksternUtbetalingsperiode(
                fom = fom.minusMonths(7), tom = tom.minusMonths(6)
            ),
            lagEksternUtbetalingsperiode(
                fom = fom.minusMonths(5), tom = tom.plusMonths(1)
            ),
        )
        assertThrows<IllegalStateException> {
            økonomiService.validerOpphørsoppdrag(
                lagEksternUtbetalingsoppdrag(utbetalingsPeriode)
            )
        }
    }

    private fun lagEksternUtbetalingsoppdrag(utbetalingsPeriode: List<Utbetalingsperiode>) =
        Utbetalingsoppdrag(
            Utbetalingsoppdrag.KodeEndring.ENDR,
            "BA",
            "123",
            "123",
            "123",
            avstemmingTidspunkt = LocalDateTime.now(),
            utbetalingsperiode = utbetalingsPeriode
        )

    private fun lagEksternUtbetalingsperiode(opphør: Opphør? = null, fom: LocalDate, tom: LocalDate) =
        Utbetalingsperiode(
            false,
            opphør,
            1,
            null,
            vedtakDato,
            "BATR",
            fom,
            tom,
            BigDecimal(1054),
            Utbetalingsperiode.SatsType.MND,
            søker,
            behandling.id
        )
}
