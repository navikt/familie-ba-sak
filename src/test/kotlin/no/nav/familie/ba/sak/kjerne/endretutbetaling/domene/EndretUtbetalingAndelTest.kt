package no.nav.familie.ba.sak.kjerne.endretutbetaling.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.tilfeldigPerson
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

internal class EndretUtbetalingAndelTest {

    @Test
    fun `Sjekk validering med tome felt`() {
        val behandling = lagBehandling();
        val endretUtbetalingAndel = EndretUtbetalingAndel(behandlingId = behandling.id)

        org.junit.jupiter.api.assertThrows<Feil> {
            endretUtbetalingAndel.validerUtfyltEndring();
        }
    }

    @Test
    fun `Sjekk validering med ikke tome felt`() {
        val behandling = lagBehandling();
        val endretUtbetalingAndel = EndretUtbetalingAndel(behandlingId = behandling.id)

        endretUtbetalingAndel.person = tilfeldigPerson()
        endretUtbetalingAndel.prosent = BigDecimal(0)
        endretUtbetalingAndel.fom = YearMonth.of(2020, 10)
        endretUtbetalingAndel.tom = YearMonth.of(2020, 10)
        endretUtbetalingAndel.årsak = Årsak.EØS_SEKUNDÆRLAND
        endretUtbetalingAndel.søknadstidspunkt = LocalDate.now()

        assertTrue(endretUtbetalingAndel.validerUtfyltEndring())
    }

    @Test
    fun `Sjekk validering for delt bosted med tomt felt avtaletidpunkt`() {
        val behandling = lagBehandling();
        val endretUtbetalingAndel = EndretUtbetalingAndel(behandlingId = behandling.id)

        endretUtbetalingAndel.person = tilfeldigPerson()
        endretUtbetalingAndel.prosent = BigDecimal(0)
        endretUtbetalingAndel.fom = YearMonth.of(2020, 10)
        endretUtbetalingAndel.tom = YearMonth.of(2020, 10)
        endretUtbetalingAndel.årsak = Årsak.DELT_BOSTED
        endretUtbetalingAndel.søknadstidspunkt = LocalDate.now()

        org.junit.jupiter.api.assertThrows<Feil> {
            endretUtbetalingAndel.validerUtfyltEndring();
        }
    }

    @Test
    fun `Sjekk validering for delt bosted med ikke tomt felt avtaletidpunkt`() {
        val behandling = lagBehandling();
        val endretUtbetalingAndel = EndretUtbetalingAndel(behandlingId = behandling.id)

        endretUtbetalingAndel.person = tilfeldigPerson()
        endretUtbetalingAndel.prosent = BigDecimal(0)
        endretUtbetalingAndel.fom = YearMonth.of(2020, 10)
        endretUtbetalingAndel.tom = YearMonth.of(2020, 10)
        endretUtbetalingAndel.årsak = Årsak.DELT_BOSTED
        endretUtbetalingAndel.søknadstidspunkt = LocalDate.now()
        endretUtbetalingAndel.avtaletidspunktDeltBosted = LocalDate.now()

        assertTrue(endretUtbetalingAndel.validerUtfyltEndring())
    }
}