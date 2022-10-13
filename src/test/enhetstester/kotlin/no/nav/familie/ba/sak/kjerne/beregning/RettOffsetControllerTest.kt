package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.lagUtbetalingsoppdrag
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Opphør
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

internal class RettOffsetControllerTest {

    @Test
    fun `test at vi fanger opp feil tilbakestilling`() {
        val andeler = mutableSetOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2020, 1),
                tom = YearMonth.of(2020, 6),
                periodeIdOffset = 0,
                forrigeperiodeIdOffset = null
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2020, 7),
                tom = YearMonth.of(2020, 11),
                periodeIdOffset = 1,
                forrigeperiodeIdOffset = 0
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2019, 2),
                tom = YearMonth.of(2020, 4),
                periodeIdOffset = 2,
                forrigeperiodeIdOffset = null
            )
        )

        val utbetalingsoppdrag = lagUtbetalingsoppdrag(
            listOf(
                lagUtbetalingsperiode(1, LocalDate.of(2020, 1, 1), true),
                lagUtbetalingsperiode(2, null, false),
                lagUtbetalingsperiode(3, null, false),
                lagUtbetalingsperiode(4, null, false)
            )
        )

        val tilkjentYtelse = TilkjentYtelse(
            behandling = lagBehandling(),
            opprettetDato = LocalDate.now(),
            endretDato = LocalDate.now(),
            utbetalingsoppdrag = objectMapper.writeValueAsString(utbetalingsoppdrag),
            andelerTilkjentYtelse = andeler
        )

        assertTrue(harFeilUtbetalingsoppdragMhpAndeler(tilkjentYtelse))
    }

    @Test
    fun `test at vi ikke reagerer på riktig utbetalingsoppdrag`() {
        val andeler = mutableSetOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2020, 1),
                tom = YearMonth.of(2020, 6),
                periodeIdOffset = 3,
                forrigeperiodeIdOffset = 1
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2020, 7),
                tom = YearMonth.of(2020, 11),
                periodeIdOffset = 4,
                forrigeperiodeIdOffset = 3
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2019, 2),
                tom = YearMonth.of(2020, 4),
                periodeIdOffset = 2,
                forrigeperiodeIdOffset = null
            )
        )

        val utbetalingsoppdrag = lagUtbetalingsoppdrag(
            listOf(
                lagUtbetalingsperiode(1, LocalDate.of(2020, 1, 1), true),
                lagUtbetalingsperiode(2, null, false),
                lagUtbetalingsperiode(3, null, false),
                lagUtbetalingsperiode(4, null, false)
            )
        )

        val tilkjentYtelse = TilkjentYtelse(
            behandling = lagBehandling(),
            opprettetDato = LocalDate.now(),
            endretDato = LocalDate.now(),
            utbetalingsoppdrag = objectMapper.writeValueAsString(utbetalingsoppdrag),
            andelerTilkjentYtelse = andeler
        )

        assertFalse(harFeilUtbetalingsoppdragMhpAndeler(tilkjentYtelse))
    }

    private fun lagUtbetalingsperiode(periodeId: Long, opphørsdato: LocalDate?, erEndring: Boolean) =
        Utbetalingsperiode(
            erEndringPåEksisterendePeriode = erEndring,
            opphør = opphørsdato?.let { Opphør(it) },
            periodeId = periodeId,
            datoForVedtak = LocalDate.now(),
            klassifisering = "BATR",
            vedtakdatoFom = LocalDate.now().minusMonths(2).førsteDagIInneværendeMåned(),
            vedtakdatoTom = LocalDate.now().minusMonths(1).sisteDagIMåned(),
            sats = BigDecimal("1054"),
            satsType = Utbetalingsperiode.SatsType.MND,
            utbetalesTil = "",
            behandlingId = 0
        )
}
