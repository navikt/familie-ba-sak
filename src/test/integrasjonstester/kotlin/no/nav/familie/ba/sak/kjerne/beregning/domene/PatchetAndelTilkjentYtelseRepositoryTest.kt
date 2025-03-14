package no.nav.familie.ba.sak.kjerne.beregning.domene

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.YearMonth

class PatchetAndelTilkjentYtelseRepositoryTest(
    @Autowired
    private val patchetAndelTilkjentYtelseRepository: PatchetAndelTilkjentYtelseRepository,
) : AbstractSpringIntegrationTest() {
    @Test
    fun `Skal kunne lagre og hente patchede andeler tilkjent ytelse`() {
        // Arrange
        val patchetAndelTilkjentYtelse =
            PatchetAndelTilkjentYtelse(
                id = 1,
                behandlingId = 1,
                tilkjentYtelseId = 1,
                aktørId = "1234567891012",
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
        assertDoesNotThrow { patchetAndelTilkjentYtelseRepository.saveAndFlush(patchetAndelTilkjentYtelse) }
        val lagredePatchetAndelTilkjentYtelse = patchetAndelTilkjentYtelseRepository.findAll()

        // Assert
        assertThat(lagredePatchetAndelTilkjentYtelse).hasSize(1)
        assertThat(lagredePatchetAndelTilkjentYtelse.first()).isEqualTo(patchetAndelTilkjentYtelse)
    }
}
