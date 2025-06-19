package no.nav.familie.ba.sak.kjerne.endretutbetaling.domene

import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.endretutbetaling.beregnGyldigTomIFremtiden
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

internal class EndretUtbetalingAndelTest {
    @Test
    fun `Sjekk validering med tomme felt`() {
        val behandling = lagBehandling()
        val endretUtbetalingAndel = EndretUtbetalingAndel(behandlingId = behandling.id)
        endretUtbetalingAndel.begrunnelse = ""

        assertThrows<RuntimeException> {
            endretUtbetalingAndel.validerUtfyltEndring()
        }
    }

    @Test
    fun `Sjekk validering for delt bosted med tomt felt avtaletidpunkt`() {
        val behandling = lagBehandling()
        val endretUtbetalingAndel = EndretUtbetalingAndel(behandlingId = behandling.id)

        endretUtbetalingAndel.personer = mutableSetOf(tilfeldigPerson())
        endretUtbetalingAndel.prosent = BigDecimal(0)
        endretUtbetalingAndel.fom = YearMonth.of(2020, 10)
        endretUtbetalingAndel.tom = YearMonth.of(2020, 10)
        endretUtbetalingAndel.årsak = Årsak.DELT_BOSTED
        endretUtbetalingAndel.søknadstidspunkt = LocalDate.now()
        endretUtbetalingAndel.begrunnelse = "begrunnelse"

        assertThrows<RuntimeException> {
            endretUtbetalingAndel.validerUtfyltEndring()
        }
    }

    @Test
    fun `Sjekk validering for delt bosted med ikke tomt felt avtaletidpunkt`() {
        val behandling = lagBehandling()
        val endretUtbetalingAndel = EndretUtbetalingAndel(behandlingId = behandling.id)

        endretUtbetalingAndel.personer = mutableSetOf(tilfeldigPerson())
        endretUtbetalingAndel.prosent = BigDecimal(0)
        endretUtbetalingAndel.fom = YearMonth.of(2020, 10)
        endretUtbetalingAndel.tom = YearMonth.of(2020, 10)
        endretUtbetalingAndel.årsak = Årsak.DELT_BOSTED
        endretUtbetalingAndel.søknadstidspunkt = LocalDate.now()
        endretUtbetalingAndel.avtaletidspunktDeltBosted = LocalDate.now()
        endretUtbetalingAndel.begrunnelse = "begrunnelse"

        assertTrue(endretUtbetalingAndel.validerUtfyltEndring())
    }

    @Test
    fun `Skal sette tom til siste måned med andel tilkjent ytelse hvis tom er null og det ikke finnes noen andre endringsperioder`() {
        val behandling = lagBehandling()
        val barn1 = lagPerson(type = PersonType.BARN)
        val barn2 = lagPerson(type = PersonType.BARN)
        val endretUtbetalingAndel =
            lagEndretUtbetalingAndel(
                behandlingId = behandling.id,
                personer = setOf(barn1, barn2),
                fom = YearMonth.now(),
                tom = null,
                årsak = Årsak.DELT_BOSTED,
            )

        val sisteTomPåAndelerBarn1 = YearMonth.now().plusMonths(10)
        val sisteTomPåAndelerBarn2 = YearMonth.now().plusMonths(9)
        val andelTilkjentYtelser =
            listOf(
                lagAndelTilkjentYtelse(
                    person = barn1,
                    fom = YearMonth.now().minusMonths(4),
                    tom = YearMonth.now().plusMonths(4),
                ),
                lagAndelTilkjentYtelse(
                    person = barn1,
                    fom = YearMonth.now().plusMonths(5),
                    tom = sisteTomPåAndelerBarn1,
                ),
                lagAndelTilkjentYtelse(
                    person = barn2,
                    fom = YearMonth.now().minusMonths(4),
                    tom = YearMonth.now().plusMonths(4),
                ),
                lagAndelTilkjentYtelse(
                    person = barn2,
                    fom = YearMonth.now().plusMonths(5),
                    tom = sisteTomPåAndelerBarn2,
                ),
            )

        val nyTom =
            beregnGyldigTomIFremtiden(
                andelTilkjentYtelser = andelTilkjentYtelser,
                endretUtbetalingAndel = endretUtbetalingAndel,
                andreEndredeAndelerPåBehandling = emptyList(),
            )

        val forventetTom = minOf(sisteTomPåAndelerBarn1, sisteTomPåAndelerBarn2)
        assertEquals(forventetTom, nyTom)
    }

    @Test
    fun `Skal sette tom til måneden før neste endringsperiode`() {
        val behandling = lagBehandling()
        val barn1 = lagPerson(type = PersonType.BARN)
        val barn2 = lagPerson(type = PersonType.BARN)
        val endretUtbetalingAndel =
            lagEndretUtbetalingAndel(
                behandlingId = behandling.id,
                personer = setOf(barn1, barn2),
                fom = YearMonth.now(),
                tom = null,
                årsak = Årsak.DELT_BOSTED,
            )

        val fomPåEndretAndelBarn1 = YearMonth.now().plusMonths(5)
        val fomPåEndretAndelBarn2 = YearMonth.now().plusMonths(6)
        val andreEndretAndeler =
            listOf(
                lagEndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    personer = setOf(barn1),
                    fom = fomPåEndretAndelBarn1,
                    tom = YearMonth.now().plusMonths(8),
                    årsak = Årsak.DELT_BOSTED,
                ),
                lagEndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    personer = setOf(barn2),
                    fom = fomPåEndretAndelBarn2,
                    tom = YearMonth.now().plusMonths(8),
                    årsak = Årsak.DELT_BOSTED,
                ),
            )

        val andelTilkjentYtelser =
            listOf(
                lagAndelTilkjentYtelse(
                    person = barn1,
                    fom = YearMonth.now().minusMonths(4),
                    tom = YearMonth.now().plusMonths(4),
                ),
                lagAndelTilkjentYtelse(
                    person = barn1,
                    fom = YearMonth.now().plusMonths(5),
                    tom = YearMonth.now().plusMonths(10),
                ),
                lagAndelTilkjentYtelse(
                    person = barn2,
                    fom = YearMonth.now().minusMonths(4),
                    tom = YearMonth.now().plusMonths(4),
                ),
                lagAndelTilkjentYtelse(
                    person = barn2,
                    fom = YearMonth.now().plusMonths(5),
                    tom = YearMonth.now().plusMonths(9),
                ),
            )

        val nyTom =
            beregnGyldigTomIFremtiden(
                andelTilkjentYtelser = andelTilkjentYtelser,
                endretUtbetalingAndel = endretUtbetalingAndel,
                andreEndredeAndelerPåBehandling = andreEndretAndeler,
            )

        val forventetTom = andreEndretAndeler.minOf { it.fom!! }.minusMonths(1)
        assertEquals(forventetTom, nyTom)
    }
}
