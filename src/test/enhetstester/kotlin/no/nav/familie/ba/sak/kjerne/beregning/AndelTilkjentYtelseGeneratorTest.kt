package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagEndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.kjerne.beregning.AndelTilkjentYtelseGenerator.oppdaterAndelerMedEndretUtbetalingAndeler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

class AndelTilkjentYtelseGeneratorTest {
    @Test
    fun `endret utbetalingsandel skal overstyre andel`() {
        val person = lagPerson()
        val behandling = lagBehandling()
        val fom = YearMonth.of(2018, 1)
        val tom = YearMonth.of(2019, 1)
        val utbetalingsandeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = fom,
                    tom = tom,
                    person = person,
                    behandling = behandling,
                ),
            )

        val endretProsent = BigDecimal.ZERO

        val endretUtbetalingAndeler =
            listOf(
                lagEndretUtbetalingAndelMedAndelerTilkjentYtelse(
                    person = person,
                    fom = fom,
                    tom = tom,
                    prosent = endretProsent,
                    behandlingId = behandling.id,
                ),
            )

        val andelerTIlkjentYtelse =
            oppdaterAndelerMedEndretUtbetalingAndeler(
                utbetalingsandeler,
                endretUtbetalingAndeler,
                utbetalingsandeler.first().tilkjentYtelse,
            )

        assertEquals(1, andelerTIlkjentYtelse.size)
        assertEquals(endretProsent, andelerTIlkjentYtelse.single().prosent)
        assertEquals(1, andelerTIlkjentYtelse.single().endreteUtbetalinger.size)
    }

    @Test
    fun `endret utbetalingsandel koble endrede andeler til riktig endret utbetalingandel`() {
        val person = lagPerson()
        val behandling = lagBehandling()
        val fom1 = YearMonth.of(2018, 1)
        val tom1 = YearMonth.of(2018, 11)

        val fom2 = YearMonth.of(2019, 1)
        val tom2 = YearMonth.of(2019, 11)

        val utbetalingsandeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = fom1,
                    tom = tom1,
                    person = person,
                    behandling = behandling,
                ),
                lagAndelTilkjentYtelse(
                    fom = fom2,
                    tom = tom2,
                    person = person,
                    behandling = behandling,
                ),
            )

        val endretProsent = BigDecimal.ZERO

        val endretUtbetalingAndel =
            lagEndretUtbetalingAndelMedAndelerTilkjentYtelse(
                person = person,
                fom = fom1,
                tom = tom2,
                prosent = endretProsent,
                behandlingId = behandling.id,
            )

        val endretUtbetalingAndeler =
            listOf(
                endretUtbetalingAndel,
                lagEndretUtbetalingAndelMedAndelerTilkjentYtelse(
                    person = person,
                    fom = tom2.nesteMåned(),
                    prosent = endretProsent,
                    behandlingId = behandling.id,
                ),
            )

        val andelerTIlkjentYtelse =
            oppdaterAndelerMedEndretUtbetalingAndeler(
                utbetalingsandeler,
                endretUtbetalingAndeler,
                utbetalingsandeler.first().tilkjentYtelse,
            )

        assertEquals(2, andelerTIlkjentYtelse.size)
        andelerTIlkjentYtelse.forEach { assertEquals(endretProsent, it.prosent) }
        andelerTIlkjentYtelse.forEach { assertEquals(1, it.endreteUtbetalinger.size) }
        andelerTIlkjentYtelse.forEach {
            assertEquals(
                endretUtbetalingAndel.id,
                it.endreteUtbetalinger.single().id,
            )
        }
    }
}
