package no.nav.familie.ba.sak.kjerne.beregning.domene

import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.randomAktør
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

class PatchetAndelTilkjentYtelseTest {
    @Nested
    inner class TilPatchetAndelTilkjentYtelse {
        @Test
        fun `skal mappe andel tilkjent ytelse til patchet andel tilkjent ytelse`() {
            // Arrange
            val behandling = lagBehandling()
            val aktør = randomAktør()
            val andelTilkjentYtelse =
                AndelTilkjentYtelse(
                    id = 1,
                    behandlingId = behandling.id,
                    tilkjentYtelse = lagTilkjentYtelse(behandling),
                    aktør = aktør,
                    kalkulertUtbetalingsbeløp = 1766,
                    stønadFom = YearMonth.of(2025, 2),
                    stønadTom = YearMonth.of(2025, 7),
                    type = YtelseType.ORDINÆR_BARNETRYGD,
                    sats = 1766,
                    prosent = BigDecimal.valueOf(100),
                    kildeBehandlingId = 1,
                    periodeOffset = 0,
                    forrigePeriodeOffset = null,
                    nasjonaltPeriodebeløp = null,
                    differanseberegnetPeriodebeløp = null,
                )

            // Act
            val patchetAndelTilkjentYtelse = andelTilkjentYtelse.tilPatchetAndelTilkjentYtelse()

            // Assert
            assertThat(patchetAndelTilkjentYtelse.id).isEqualTo(andelTilkjentYtelse.id)
            assertThat(patchetAndelTilkjentYtelse.behandlingId).isEqualTo(andelTilkjentYtelse.behandlingId)
            assertThat(patchetAndelTilkjentYtelse.tilkjentYtelseId).isEqualTo(andelTilkjentYtelse.tilkjentYtelse.id)
            assertThat(patchetAndelTilkjentYtelse.aktørId).isEqualTo(andelTilkjentYtelse.aktør.aktørId)
            assertThat(patchetAndelTilkjentYtelse.kalkulertUtbetalingsbeløp).isEqualTo(andelTilkjentYtelse.kalkulertUtbetalingsbeløp)
            assertThat(patchetAndelTilkjentYtelse.stønadFom).isEqualTo(andelTilkjentYtelse.stønadFom)
            assertThat(patchetAndelTilkjentYtelse.stønadTom).isEqualTo(andelTilkjentYtelse.stønadTom)
            assertThat(patchetAndelTilkjentYtelse.type).isEqualTo(andelTilkjentYtelse.type)
            assertThat(patchetAndelTilkjentYtelse.sats).isEqualTo(andelTilkjentYtelse.sats)
            assertThat(patchetAndelTilkjentYtelse.prosent).isEqualTo(andelTilkjentYtelse.prosent)
            assertThat(patchetAndelTilkjentYtelse.kildeBehandlingId).isEqualTo(andelTilkjentYtelse.kildeBehandlingId)
            assertThat(patchetAndelTilkjentYtelse.periodeOffset).isEqualTo(andelTilkjentYtelse.periodeOffset)
            assertThat(patchetAndelTilkjentYtelse.forrigePeriodeOffset).isEqualTo(andelTilkjentYtelse.forrigePeriodeOffset)
            assertThat(patchetAndelTilkjentYtelse.nasjonaltPeriodebeløp).isEqualTo(andelTilkjentYtelse.nasjonaltPeriodebeløp)
            assertThat(patchetAndelTilkjentYtelse.differanseberegnetPeriodebeløp).isEqualTo(andelTilkjentYtelse.differanseberegnetPeriodebeløp)
        }
    }
}
