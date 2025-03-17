package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.kjerne.tidslinje.util.somBoolskTidslinje
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.YearMonth

class BehandlingStegUtilsKtTest {
    @Nested
    inner class KastFeilVedEndringEtterTest {
        @Test
        fun `Skal kaste feil om det er endring etter migreringsdatoen til første behandling`() {
            val startdato = YearMonth.of(2023, 2)
            val endringTidslinje = "TTTFFFF".somBoolskTidslinje(startdato)

            assertThrows<FunksjonellFeil> {
                endringTidslinje.kastFeilVedEndringEtter(startdato, lagBehandling())
            }
        }

        @Test
        fun `Skal ikke kaste feil om det ikke er endring etter migreringsdatoen til første behandling`() {
            val startdato = YearMonth.of(2023, 2)
            val treMånederEtterStartdato = startdato.plusMonths(3)

            val endringTidslinje = "TTTFFFF".somBoolskTidslinje(startdato)

            assertDoesNotThrow {
                endringTidslinje.kastFeilVedEndringEtter(treMånederEtterStartdato, lagBehandling())
            }
        }
    }
}
