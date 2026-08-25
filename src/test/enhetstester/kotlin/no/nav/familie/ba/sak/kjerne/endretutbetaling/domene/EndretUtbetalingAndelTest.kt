package no.nav.familie.ba.sak.kjerne.endretutbetaling.domene

import io.mockk.mockk
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.beregnGyldigTom
import no.nav.familie.ba.sak.kjerne.endretutbetaling.beregnGyldigTomPerAktør
import no.nav.familie.ba.sak.kjerne.endretutbetaling.skalSplitteEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.splittEndretUbetalingAndel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

internal class EndretUtbetalingAndelTest {
    @Test
    fun `Sjekk validering med tomme felt`() {
        // Arrange
        val behandling = lagBehandling()
        val endretUtbetalingAndel = EndretUtbetalingAndel(behandlingId = behandling.id)
        endretUtbetalingAndel.begrunnelse = ""

        // Act & Assert
        assertThrows<RuntimeException> {
            endretUtbetalingAndel.validerUtfyltEndring()
        }
    }

    @Test
    fun `Sjekk validering for delt bosted med tomt felt avtaletidpunkt`() {
        // Arrange
        val behandling = lagBehandling()
        val endretUtbetalingAndel = EndretUtbetalingAndel(behandlingId = behandling.id)

        endretUtbetalingAndel.aktører = mutableSetOf(lagAktør())
        endretUtbetalingAndel.prosent = BigDecimal(0)
        endretUtbetalingAndel.fom = YearMonth.of(2020, 10)
        endretUtbetalingAndel.tom = YearMonth.of(2020, 10)
        endretUtbetalingAndel.årsak = Årsak.DELT_BOSTED
        endretUtbetalingAndel.søknadstidspunkt = LocalDate.now()
        endretUtbetalingAndel.begrunnelse = "begrunnelse"

        // Act & Assert
        assertThrows<RuntimeException> {
            endretUtbetalingAndel.validerUtfyltEndring()
        }
    }

    @Test
    fun `Sjekk validering for delt bosted med ikke tomt felt avtaletidpunkt`() {
        // Arrange
        val behandling = lagBehandling()
        val endretUtbetalingAndel = EndretUtbetalingAndel(behandlingId = behandling.id)

        endretUtbetalingAndel.aktører = mutableSetOf(lagAktør())
        endretUtbetalingAndel.prosent = BigDecimal(0)
        endretUtbetalingAndel.fom = YearMonth.of(2020, 10)
        endretUtbetalingAndel.tom = YearMonth.of(2020, 10)
        endretUtbetalingAndel.årsak = Årsak.DELT_BOSTED
        endretUtbetalingAndel.søknadstidspunkt = LocalDate.now()
        endretUtbetalingAndel.avtaletidspunktDeltBosted = LocalDate.now()
        endretUtbetalingAndel.begrunnelse = "begrunnelse"

        // Act & Assert
        assertTrue(endretUtbetalingAndel.validerUtfyltEndring())
    }

    @Test
    fun `Skal sette tom til siste måned med andel tilkjent ytelse hvis tom er null og det ikke finnes noen andre endringsperioder`() {
        // Arrange
        val behandling = lagBehandling()
        val barn1 = lagAktør()
        val barn2 = lagAktør()
        val endretUtbetalingAndel =
            lagEndretUtbetalingAndel(
                behandlingId = behandling.id,
                aktører = setOf(barn1, barn2),
                fom = YearMonth.now(),
                tom = null,
                årsak = Årsak.DELT_BOSTED,
            )

        val sisteTomPåAndelerBarn1 = YearMonth.now().plusMonths(10)
        val sisteTomPåAndelerBarn2 = YearMonth.now().plusMonths(9)
        val andelTilkjentYtelser =
            listOf(
                lagAndelTilkjentYtelse(
                    aktør = barn1,
                    fom = YearMonth.now().minusMonths(4),
                    tom = YearMonth.now().plusMonths(4),
                ),
                lagAndelTilkjentYtelse(
                    aktør = barn1,
                    fom = YearMonth.now().plusMonths(5),
                    tom = sisteTomPåAndelerBarn1,
                ),
                lagAndelTilkjentYtelse(
                    aktør = barn2,
                    fom = YearMonth.now().minusMonths(4),
                    tom = YearMonth.now().plusMonths(4),
                ),
                lagAndelTilkjentYtelse(
                    aktør = barn2,
                    fom = YearMonth.now().plusMonths(5),
                    tom = sisteTomPåAndelerBarn2,
                ),
            )

        // Act
        val nyTom =
            beregnGyldigTom(
                andelTilkjentYtelser = andelTilkjentYtelser,
                endretUtbetalingAndel = endretUtbetalingAndel,
                andreEndredeAndelerPåBehandling = emptyList(),
            )

        // Assert
        val forventetTom = minOf(sisteTomPåAndelerBarn1, sisteTomPåAndelerBarn2)
        assertEquals(forventetTom, nyTom)
    }

    @Test
    fun `Skal sette tom til måneden før neste endringsperiode`() {
        // Arrange
        val behandling = lagBehandling()
        val barn1 = lagAktør()
        val barn2 = lagAktør()
        val endretUtbetalingAndel =
            lagEndretUtbetalingAndel(
                behandlingId = behandling.id,
                aktører = setOf(barn1, barn2),
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
                    aktører = setOf(barn1),
                    fom = fomPåEndretAndelBarn1,
                    tom = YearMonth.now().plusMonths(8),
                    årsak = Årsak.DELT_BOSTED,
                ),
                lagEndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    aktører = setOf(barn2),
                    fom = fomPåEndretAndelBarn2,
                    tom = YearMonth.now().plusMonths(8),
                    årsak = Årsak.DELT_BOSTED,
                ),
            )

        val andelTilkjentYtelser =
            listOf(
                lagAndelTilkjentYtelse(
                    aktør = barn1,
                    fom = YearMonth.now().minusMonths(4),
                    tom = YearMonth.now().plusMonths(4),
                ),
                lagAndelTilkjentYtelse(
                    aktør = barn1,
                    fom = YearMonth.now().plusMonths(5),
                    tom = YearMonth.now().plusMonths(10),
                ),
                lagAndelTilkjentYtelse(
                    aktør = barn2,
                    fom = YearMonth.now().minusMonths(4),
                    tom = YearMonth.now().plusMonths(4),
                ),
                lagAndelTilkjentYtelse(
                    aktør = barn2,
                    fom = YearMonth.now().plusMonths(5),
                    tom = YearMonth.now().plusMonths(9),
                ),
            )

        // Act
        val nyTom =
            beregnGyldigTom(
                andelTilkjentYtelser = andelTilkjentYtelser,
                endretUtbetalingAndel = endretUtbetalingAndel,
                andreEndredeAndelerPåBehandling = andreEndretAndeler,
            )

        // Assert
        val forventetTom = andreEndretAndeler.minOf { it.fom!! }.minusMonths(1)
        assertEquals(forventetTom, nyTom)
    }

    @Test
    fun `Skal sette tom til siste måned med andel tilkjent ytelse per aktør hvis tom er null og det ikke finnes noen andre endringsperioder`() {
        // Arrange
        val behandling = lagBehandling()
        val barn1 = lagAktør()
        val barn2 = lagAktør()

        val nyEndretUtbetalingAndel =
            lagEndretUtbetalingAndel(
                behandlingId = behandling.id,
                aktører = setOf(barn1, barn2),
                fom = YearMonth.of(2025, 6),
                tom = null,
                årsak = Årsak.ALLEREDE_UTBETALT,
            )

        val eksisterendeEndretUtbetalingAndel =
            lagEndretUtbetalingAndel(
                behandlingId = behandling.id,
                aktører = setOf(barn1),
                fom = YearMonth.of(2025, 8),
                tom = YearMonth.of(2025, 10),
                årsak = Årsak.ENDRE_MOTTAKER,
            )

        val sisteTomPåAndelerBarn1 = YearMonth.of(2025, 11)
        val sisteTomPåAndelerBarn2 = YearMonth.of(2025, 12)
        val andelTilkjentYtelser =
            listOf(
                lagAndelTilkjentYtelse(
                    aktør = barn1,
                    fom = YearMonth.of(2025, 1),
                    tom = sisteTomPåAndelerBarn1,
                ),
                lagAndelTilkjentYtelse(
                    aktør = barn2,
                    fom = YearMonth.of(2025, 1),
                    tom = sisteTomPåAndelerBarn2,
                ),
            )

        // Act
        val faktiskTomPerAktør =
            beregnGyldigTomPerAktør(
                endretUtbetalingAndel = nyEndretUtbetalingAndel,
                andelTilkjentYtelser = andelTilkjentYtelser,
                andreEndredeAndelerPåBehandling = listOf(eksisterendeEndretUtbetalingAndel),
            )

        // Assert
        val forventetTomPerAktør =
            mapOf(
                barn1 to eksisterendeEndretUtbetalingAndel.fom?.minusMonths(1),
                barn2 to sisteTomPåAndelerBarn2,
            )
        assertEquals(forventetTomPerAktør, faktiskTomPerAktør)
    }

    @Nested
    inner class SkalSplitteEndretUtbetalingAndel {
        private val endretUtbetalingAndel = EndretUtbetalingAndel(behandlingId = 0)

        @Test
        fun `skal returnere false hvis tom ikke er null`() {
            // Arrange
            val endretUtbetalingAndel = endretUtbetalingAndel.copy(tom = YearMonth.of(2025, 12))

            // Act & Assert
            assertThat(
                skalSplitteEndretUtbetalingAndel(
                    endretUtbetalingAndel = endretUtbetalingAndel,
                    gyldigTomDatoPerAktør = mockk(),
                ),
            ).isFalse()
        }

        @Test
        fun `skal returnere false hvis begge personer har samme gyldigTomDato`() {
            // Arrange
            val aktør1 = lagAktør()
            val aktør2 = lagAktør()

            val gyldigTomEtterDagensDatoPerAktør =
                mapOf(
                    aktør1 to YearMonth.of(2025, 6),
                    aktør2 to YearMonth.of(2025, 6),
                )

            // Act & Assert
            assertThat(
                skalSplitteEndretUtbetalingAndel(
                    endretUtbetalingAndel = endretUtbetalingAndel,
                    gyldigTomDatoPerAktør = gyldigTomEtterDagensDatoPerAktør,
                ),
            ).isFalse()
        }

        @Test
        fun `skal returnere true hvis tom-dato er null og gyldigTomDato inneholder flere datoer`() {
            // Arrange
            val aktør1 = lagAktør()
            val aktør2 = lagAktør()

            val gyldigTomEtterDagensDatoPerAktør =
                mapOf(
                    aktør1 to YearMonth.of(2025, 6),
                    aktør2 to YearMonth.of(2025, 7),
                )

            // Act & Assert
            assertThat(
                skalSplitteEndretUtbetalingAndel(
                    endretUtbetalingAndel = endretUtbetalingAndel,
                    gyldigTomDatoPerAktør = gyldigTomEtterDagensDatoPerAktør,
                ),
            ).isTrue()
        }
    }

    @Nested
    inner class SplittEndretUbetalingAndel {
        @Test
        fun `skal splitte andel med gyldig tom per aktør`() {
            // Arrange
            val aktør1 = lagAktør()
            val aktør2 = lagAktør()
            val aktør3 = lagAktør()

            val endretUtbetalingAndel =
                EndretUtbetalingAndel(
                    id = 1,
                    behandlingId = 0,
                    aktører = mutableSetOf(aktør1, aktør2, aktør3),
                    prosent = BigDecimal.ZERO,
                    fom = YearMonth.of(2025, 1),
                    tom = null,
                    årsak = Årsak.ENDRE_MOTTAKER,
                    avtaletidspunktDeltBosted = null,
                    søknadstidspunkt = LocalDate.of(2025, 1, 1),
                    begrunnelse = "Begrunnelse",
                )

            val gyldigTomEtterDagensDatoPerAktør =
                mapOf(
                    aktør1 to YearMonth.of(2025, 10),
                    aktør2 to YearMonth.of(2025, 12),
                    aktør3 to YearMonth.of(2025, 12),
                )

            // Act
            val splittedeAndeler =
                splittEndretUbetalingAndel(
                    endretUtbetalingAndel = endretUtbetalingAndel,
                    gyldigTomEtterDagensDatoPerAktør = gyldigTomEtterDagensDatoPerAktør,
                )

            // Assert
            assertThat(splittedeAndeler).hasSize(2)

            val (førsteAndel, andreAndel) = splittedeAndeler

            assertThat(førsteAndel)
                .usingRecursiveComparison()
                .ignoringFields("id", "fom", "tom", "aktører", "opprettetTidspunkt", "endretTidspunkt")
                .isEqualTo(endretUtbetalingAndel)

            assertThat(førsteAndel.id).isEqualTo(0)
            assertThat(førsteAndel.fom).isEqualTo(YearMonth.of(2025, 1))
            assertThat(førsteAndel.tom).isEqualTo(YearMonth.of(2025, 10))
            assertThat(førsteAndel.aktører).containsExactlyInAnyOrder(aktør1, aktør2, aktør3)

            assertThat(andreAndel)
                .usingRecursiveComparison()
                .ignoringFields("id", "fom", "tom", "aktører", "opprettetTidspunkt", "endretTidspunkt")
                .isEqualTo(endretUtbetalingAndel)

            assertThat(andreAndel.id).isEqualTo(0)
            assertThat(andreAndel.fom).isEqualTo(YearMonth.of(2025, 11))
            assertThat(andreAndel.tom).isEqualTo(YearMonth.of(2025, 12))
            assertThat(andreAndel.aktører).containsExactlyInAnyOrder(aktør2, aktør3)
        }
    }
}
