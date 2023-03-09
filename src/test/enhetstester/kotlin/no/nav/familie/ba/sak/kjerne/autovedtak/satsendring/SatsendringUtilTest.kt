package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class SatsendringUtilTest {

    @Test
    fun `Skal returnere true dersom vi har siste sats`() {
        val andelerMedSisteSats = SatsType.values()
            .filter { it != SatsType.FINN_SVAL }
            .map {
                val sisteSats = SatsService.finnSisteSatsFor(it)
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = sisteSats.gyldigFom.toYearMonth(),
                    tom = sisteSats.gyldigTom.toYearMonth(),
                    sats = sisteSats.beløp,
                    ytelseType = it.tilYtelseType()
                )
            }

        assertTrue(andelerMedSisteSats.erOppdatertMedSisteSatsForAlleSatstyper())
    }

    @Test
    fun `Skal returnere true dersom vi har siste sats selv om alle perioder er fram i tid`() {
        val andelerMedSisteSats = SatsType.values()
            .filter { it != SatsType.FINN_SVAL }
            .map {
                val sisteSats = SatsService.finnSisteSatsFor(it)
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = sisteSats.gyldigFom.toYearMonth().plusYears(1),
                    tom = sisteSats.gyldigFom.toYearMonth().plusYears(1),
                    sats = sisteSats.beløp,
                    ytelseType = it.tilYtelseType()
                )
            }

        assertTrue(andelerMedSisteSats.erOppdatertMedSisteSatsForAlleSatstyper())
    }

    @Test
    fun `Skal returnere false dersom vi ikke har siste sats`() {
        SatsType.values()
            .filter { it != SatsType.FINN_SVAL }
            .forEach {
                val sisteSats = SatsService.finnSisteSatsFor(it)
                val andelerMedFeilSats = listOf(
                    lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                        fom = sisteSats.gyldigFom.toYearMonth(),
                        tom = sisteSats.gyldigTom.toYearMonth(),
                        sats = sisteSats.beløp - 1,
                        ytelseType = it.tilYtelseType()
                    )
                )

                assertFalse(andelerMedFeilSats.erOppdatertMedSisteSatsForAlleSatstyper())
            }
    }

    @Test
    fun `Skal ignorere andeler som kommer før siste sats`() {
        SatsType.values()
            .filter { it != SatsType.FINN_SVAL }
            .forEach {
                val sisteSats = SatsService.finnSisteSatsFor(it)
                val andelerSomErFørSisteSats = listOf(
                    lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                        fom = sisteSats.gyldigFom.toYearMonth().minusMonths(100),
                        tom = sisteSats.gyldigFom.toYearMonth().minusMonths(1),
                        sats = sisteSats.beløp - 1,
                        ytelseType = it.tilYtelseType()
                    )
                )

                assertTrue(andelerSomErFørSisteSats.erOppdatertMedSisteSatsForAlleSatstyper())
            }
    }



    @Test
    fun `Skal ikke returnere false dersom vi ikke har siste sats, men de er redusert til 0 prosent`() {
        SatsType.values()
            .filter { it != SatsType.FINN_SVAL }
            .forEach {
                val sisteSats = SatsService.finnSisteSatsFor(it)
                val andelerMedFeilSats = listOf(
                    lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                        fom = sisteSats.gyldigFom.toYearMonth(),
                        tom = sisteSats.gyldigTom.toYearMonth(),
                        sats = sisteSats.beløp - 1,
                        prosent = BigDecimal.ZERO,
                        ytelseType = it.tilYtelseType()
                    )
                )

                assertTrue(andelerMedFeilSats.erOppdatertMedSisteSatsForAlleSatstyper())
            }
    }
}
