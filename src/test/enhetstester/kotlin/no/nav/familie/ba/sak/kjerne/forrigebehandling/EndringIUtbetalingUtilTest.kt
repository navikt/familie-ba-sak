package no.nav.familie.ba.sak.kjerne.forrigebehandling

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.time.YearMonth

class EndringIUtbetalingUtilTest {
    val jan22 = YearMonth.of(2022, 1)
    val mai22 = YearMonth.of(2022, 5)
    val aug22 = YearMonth.of(2022, 8)
    val sep22 = YearMonth.of(2022, 9)
    val des22 = YearMonth.of(2022, 12)

    @Test
    fun `Endring i beløp - Skal returnere periode med endring når ny andel med beløp større enn 0 er lagt til`() {
        // Arrange
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør
        val barn2Aktør = lagPerson(type = PersonType.BARN).aktør

        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )
        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = des22,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )

        // Act
        val perioderMedEndring =
            EndringIUtbetalingUtil
                .lagEndringIUtbetalingTidslinje(
                    nåværendeAndeler = nåværendeAndeler,
                    forrigeAndeler = forrigeAndeler,
                ).tilPerioder()
                .filter { it.verdi == true }

        // Assert
        Assertions.assertEquals(1, perioderMedEndring.size)
        Assertions.assertEquals(sep22, perioderMedEndring.single().fom?.toYearMonth())
        Assertions.assertEquals(des22, perioderMedEndring.single().tom?.toYearMonth())

        // Act
        val endringstidspunkt =
            EndringIUtbetalingUtil.utledEndringstidspunktForUtbetalingsbeløp(
                nåværendeAndeler = nåværendeAndeler,
                forrigeAndeler = forrigeAndeler,
            )

        // Assert
        Assertions.assertEquals(sep22, endringstidspunkt)
    }

    @Test
    fun `Endring i beløp - Skal ikke gi noen perioder med endring hvis andelene er helt like forrige behandling og nå`() {
        // Arrange
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør
        val barn2Aktør = lagPerson(type = PersonType.BARN).aktør

        val andeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn1Aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )

        // Act
        val perioderMedEndring =
            EndringIUtbetalingUtil
                .lagEndringIUtbetalingTidslinje(
                    nåværendeAndeler = andeler,
                    forrigeAndeler = andeler,
                ).tilPerioder()
                .filter { it.verdi == true }

        // Assert
        Assertions.assertTrue(perioderMedEndring.isEmpty())

        // Act
        val endringstidspunkt =
            EndringIUtbetalingUtil.utledEndringstidspunktForUtbetalingsbeløp(
                nåværendeAndeler = andeler,
                forrigeAndeler = andeler,
            )

        // Assert
        Assertions.assertNull(endringstidspunkt)
    }

    @Test
    fun `Endring i beløp - Skal returnere periode med endring hvis utvidet ikke er endret men småbarnstillegg kun er lagt på`() {
        // Arrange
        val søker = lagPerson(type = PersonType.SØKER).aktør
        val barn2Aktør = lagPerson(type = PersonType.BARN).aktør

        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = søker,
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                ),
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )
        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = søker,
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                ),
                lagAndelTilkjentYtelse(
                    fom = mai22,
                    tom = aug22,
                    beløp = 630,
                    aktør = søker,
                    ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ),
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn2Aktør,
                ),
            )

        // Act
        val perioderMedEndring =
            EndringIUtbetalingUtil
                .lagEndringIUtbetalingTidslinje(
                    nåværendeAndeler = nåværendeAndeler,
                    forrigeAndeler = forrigeAndeler,
                ).tilPerioder()
                .filter { it.verdi == true }

        // Assert
        Assertions.assertEquals(1, perioderMedEndring.size)
        Assertions.assertEquals(mai22, perioderMedEndring.single().fom?.toYearMonth())
        Assertions.assertEquals(aug22, perioderMedEndring.single().tom?.toYearMonth())

        // Act
        val endringstidspunkt =
            EndringIUtbetalingUtil.utledEndringstidspunktForUtbetalingsbeløp(
                nåværendeAndeler = nåværendeAndeler,
                forrigeAndeler = forrigeAndeler,
            )

        // Assert
        Assertions.assertEquals(mai22, endringstidspunkt)
    }

    @Test
    fun `Endring i beløp - Skal returnere periode med endring hvis andel med beløp større enn 0 er fjernet`() {
        // Arrange
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør
        val barn2Aktør = lagPerson(type = PersonType.BARN).aktør

        val andelBarn1 =
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn1Aktør,
            )
        val andelBarn2 =
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn2Aktør,
            )

        // Act
        val perioderMedEndring =
            EndringIUtbetalingUtil
                .lagEndringIUtbetalingTidslinje(
                    nåværendeAndeler = listOf(andelBarn2),
                    forrigeAndeler = listOf(andelBarn2, andelBarn1),
                ).tilPerioder()
                .filter { it.verdi == true }

        // Assert
        Assertions.assertEquals(1, perioderMedEndring.size)
        Assertions.assertEquals(jan22, perioderMedEndring.single().fom?.toYearMonth())
        Assertions.assertEquals(aug22, perioderMedEndring.single().tom?.toYearMonth())

        // Act
        val endringstidspunkt =
            EndringIUtbetalingUtil.utledEndringstidspunktForUtbetalingsbeløp(
                nåværendeAndeler = listOf(andelBarn2),
                forrigeAndeler = listOf(andelBarn2, andelBarn1),
            )

        // Assert
        Assertions.assertEquals(jan22, endringstidspunkt)
    }

    @Test
    fun `Endring i beløp - Skal ikke returnere periode med endring hvis andel med 0 i beløp er fjernet`() {
        // Arrange
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør
        val barn2Aktør = lagPerson(type = PersonType.BARN).aktør

        val andelBarn1 =
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 0,
                aktør = barn1Aktør,
            )
        val andelBarn2 =
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn2Aktør,
            )

        // Act
        val perioderMedEndring =
            EndringIUtbetalingUtil
                .lagEndringIUtbetalingTidslinje(
                    nåværendeAndeler = listOf(andelBarn2),
                    forrigeAndeler = listOf(andelBarn2, andelBarn1),
                ).tilPerioder()
                .filter { it.verdi == true }

        // Assert
        Assertions.assertTrue(perioderMedEndring.isEmpty())

        // Act
        val endringstidspunkt =
            EndringIUtbetalingUtil.utledEndringstidspunktForUtbetalingsbeløp(
                nåværendeAndeler = listOf(andelBarn2),
                forrigeAndeler = listOf(andelBarn2, andelBarn1),
            )

        // Assert
        Assertions.assertNull(endringstidspunkt)
    }

    enum class EndretFelt {
        SATS,
        PROSENT,
        KALKULERT_UTBETALINGSBELØP,
        NASJONALT_PERIODEBELØP,
        DIFFERANSEBEREGNET_PERIODEBELØP,
        BELØP_UTEN_ENDRET_UTBETALING,
    }

    @ParameterizedTest
    @EnumSource(EndretFelt::class)
    fun `skal finne aktør med endring i andel når kun ett felt er endret`(endretFelt: EndretFelt) {
        // Arrange
        val aktør = lagPerson(type = PersonType.BARN).aktør

        val forrigeAndel =
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = des22,
                aktør = aktør,
                beløp = 1054,
                sats = 1054,
                prosent = BigDecimal(100),
                nasjonaltPeriodebeløp = 1054,
                differanseberegnetPeriodebeløp = 500,
                beløpUtenEndretUtbetaling = 1054,
                kalkulertUtbetalingsbeløp = 1054,
            )

        val nåværendeAndel =
            when (endretFelt) {
                EndretFelt.SATS -> forrigeAndel.copy(sats = 2000)
                EndretFelt.PROSENT -> forrigeAndel.copy(prosent = BigDecimal(50))
                EndretFelt.KALKULERT_UTBETALINGSBELØP -> forrigeAndel.copy(kalkulertUtbetalingsbeløp = 2000)
                EndretFelt.NASJONALT_PERIODEBELØP -> forrigeAndel.copy(nasjonaltPeriodebeløp = 2000)
                EndretFelt.DIFFERANSEBEREGNET_PERIODEBELØP -> forrigeAndel.copy(differanseberegnetPeriodebeløp = 501)
                EndretFelt.BELØP_UTEN_ENDRET_UTBETALING -> forrigeAndel.copy(beløpUtenEndretUtbetaling = 2000)
            }

        // Act
        val aktørerMedEndring =
            EndringIUtbetalingUtil.finnAktørerMedEndringIAndeler(
                nåværendeAndeler = listOf(nåværendeAndel),
                forrigeAndeler = listOf(forrigeAndel),
            )

        // Assert
        assertThat(aktørerMedEndring).containsExactly(aktør)
    }

    @Test
    fun `skal ikke finne aktør med endring i andel når andelene er identiske`() {
        // Arrange
        val aktør = lagPerson(type = PersonType.BARN).aktør

        val andel =
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = des22,
                aktør = aktør,
                beløp = 1054,
                sats = 1054,
                prosent = BigDecimal(100),
                nasjonaltPeriodebeløp = 1054,
                differanseberegnetPeriodebeløp = 500,
                beløpUtenEndretUtbetaling = 1054,
                kalkulertUtbetalingsbeløp = 1054,
            )

        // Act
        val aktørerMedEndring =
            EndringIUtbetalingUtil.finnAktørerMedEndringIAndeler(
                nåværendeAndeler = listOf(andel),
                forrigeAndeler = listOf(andel),
            )

        // Assert
        assertThat(aktørerMedEndring).isEmpty()
    }
}
