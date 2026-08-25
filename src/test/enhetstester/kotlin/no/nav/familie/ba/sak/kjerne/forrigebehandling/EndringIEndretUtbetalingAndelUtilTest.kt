package no.nav.familie.ba.sak.kjerne.forrigebehandling

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

class EndringIEndretUtbetalingAndelUtilTest {
    val jan22 = YearMonth.of(2022, 1)
    val aug22 = YearMonth.of(2022, 8)
    val sep22 = YearMonth.of(2022, 9)
    val des22 = YearMonth.of(2022, 12)

    @Test
    fun `Endring i endret utbetaling andel - skal ikke ha endret periode hvis årsak endres mellom etterbetaling 3 år og 3 mnd`() {
        // Arrange
        val barn = lagAktør()
        val forrigeEndretAndel =
            lagEndretUtbetalingAndel(
                aktører = setOf(barn),
                prosent = BigDecimal.ZERO,
                fom = jan22,
                tom = aug22,
                årsak = Årsak.ETTERBETALING_3ÅR,
                søknadstidspunkt = des22.førsteDagIInneværendeMåned(),
            )

        val nåværendeEndretAndel = forrigeEndretAndel.copy(årsak = Årsak.ETTERBETALING_3MND)

        // Act
        val perioderMedEndring =
            EndringIEndretUtbetalingAndelUtil
                .lagEndringIEndretUbetalingAndelPerPersonTidslinje(
                    forrigeEndretAndelerForPerson = listOf(forrigeEndretAndel),
                    nåværendeEndretAndelerForPerson = listOf(nåværendeEndretAndel),
                ).tilPerioder()
                .filter { it.verdi == true }

        // Assert
        assertTrue(perioderMedEndring.isEmpty())
    }

    @Test
    fun `Endring i endret utbetaling andel - skal ha endret periode hvis årsak endres til noe annet enn etterbetaling`() {
        // Arrange
        val barn = lagAktør()
        val forrigeEndretAndel =
            lagEndretUtbetalingAndel(
                aktører = setOf(barn),
                prosent = BigDecimal.ZERO,
                fom = jan22,
                tom = aug22,
                årsak = Årsak.ETTERBETALING_3ÅR,
                søknadstidspunkt = des22.førsteDagIInneværendeMåned(),
            )

        val nåværendeEndretAndel = forrigeEndretAndel.copy(årsak = Årsak.ALLEREDE_UTBETALT)

        // Act
        val perioderMedEndring =
            EndringIEndretUtbetalingAndelUtil
                .lagEndringIEndretUbetalingAndelPerPersonTidslinje(
                    forrigeEndretAndelerForPerson = listOf(forrigeEndretAndel),
                    nåværendeEndretAndelerForPerson = listOf(nåværendeEndretAndel),
                ).tilPerioder()
                .filter { it.verdi == true }

        // Assert
        assertEquals(1, perioderMedEndring.size)
        assertEquals(jan22, perioderMedEndring.single().fom?.toYearMonth())
        assertEquals(aug22, perioderMedEndring.single().tom?.toYearMonth())
    }

    @Test
    fun `Endring i endret utbetaling andel - skal ikke ha noen endrede perioder hvis kun prosent er endret`() {
        // Arrange
        val barn = lagAktør()
        val forrigeEndretAndel =
            lagEndretUtbetalingAndel(
                aktører = setOf(barn),
                prosent = BigDecimal.ZERO,
                fom = jan22,
                tom = aug22,
                årsak = Årsak.DELT_BOSTED,
                søknadstidspunkt = des22.førsteDagIInneværendeMåned(),
                avtaletidspunktDeltBosted = jan22.førsteDagIInneværendeMåned(),
            )

        val nåværendeEndretAndel = forrigeEndretAndel.copy(prosent = BigDecimal(100))

        // Act
        val perioderMedEndring =
            EndringIEndretUtbetalingAndelUtil
                .lagEndringIEndretUbetalingAndelPerPersonTidslinje(
                    forrigeEndretAndelerForPerson = listOf(forrigeEndretAndel),
                    nåværendeEndretAndelerForPerson = listOf(nåværendeEndretAndel),
                ).tilPerioder()
                .filter { it.verdi == true }

        // Assert
        assertTrue(perioderMedEndring.isEmpty())
    }

    @Test
    fun `Endring i endret utbetaling andel - skal ikke ha noen endrede perioder hvis kun søknadstidspunkt er endret og den var allerede satt før`() {
        // Arrange
        val barn = lagAktør()
        val forrigeEndretAndel =
            lagEndretUtbetalingAndel(
                aktører = setOf(barn),
                prosent = BigDecimal.ZERO,
                fom = jan22,
                tom = aug22,
                årsak = Årsak.DELT_BOSTED,
                søknadstidspunkt = des22.førsteDagIInneværendeMåned(),
                avtaletidspunktDeltBosted = jan22.førsteDagIInneværendeMåned(),
            )

        val nåværendeEndretAndel = forrigeEndretAndel.copy(søknadstidspunkt = jan22.førsteDagIInneværendeMåned())

        // Act
        val perioderMedEndring =
            EndringIEndretUtbetalingAndelUtil
                .lagEndringIEndretUbetalingAndelPerPersonTidslinje(
                    forrigeEndretAndelerForPerson = listOf(forrigeEndretAndel),
                    nåværendeEndretAndelerForPerson = listOf(nåværendeEndretAndel),
                ).tilPerioder()
                .filter { it.verdi == true }

        // Assert
        assertTrue(perioderMedEndring.isEmpty())
    }

    @Test
    fun `Endring i endret utbetaling andel - skal ha noen endrede perioder hvis søknadstidspunkt ikke var satt tidligere men er nå satt`() {
        // Arrange
        val barn = lagAktør()
        val forrigeEndretAndel =
            lagEndretUtbetalingAndel(
                aktører = setOf(barn),
                prosent = BigDecimal.ZERO,
                fom = jan22,
                tom = aug22,
                årsak = Årsak.DELT_BOSTED,
                søknadstidspunkt = null,
                avtaletidspunktDeltBosted = jan22.førsteDagIInneværendeMåned(),
            )

        val nåværendeEndretAndel = forrigeEndretAndel.copy(søknadstidspunkt = jan22.førsteDagIInneværendeMåned())

        // Act
        val perioderMedEndring =
            EndringIEndretUtbetalingAndelUtil
                .lagEndringIEndretUbetalingAndelPerPersonTidslinje(
                    forrigeEndretAndelerForPerson = listOf(forrigeEndretAndel),
                    nåværendeEndretAndelerForPerson = listOf(nåværendeEndretAndel),
                ).tilPerioder()
                .filter { it.verdi == true }

        // Assert
        assertEquals(1, perioderMedEndring.size)
        assertEquals(jan22, perioderMedEndring.single().fom?.toYearMonth())
        assertEquals(aug22, perioderMedEndring.single().tom?.toYearMonth())
    }

    @Test
    fun `Endring i endret utbetaling andel - skal ikke returnere endret periode hvis et av to barn kun har endring mellom etterbetalingsårsaker`() {
        // Arrange
        val barn1 = lagAktør()
        val barn2 = lagAktør()

        val forrigeEndretAndelBarn1 =
            lagEndretUtbetalingAndel(
                aktører = setOf(barn1),
                prosent = BigDecimal.ZERO,
                fom = jan22,
                tom = aug22,
                årsak = Årsak.DELT_BOSTED,
                søknadstidspunkt = des22.førsteDagIInneværendeMåned(),
                avtaletidspunktDeltBosted = jan22.førsteDagIInneværendeMåned(),
            )

        val forrigeEndretAndelBarn2 =
            lagEndretUtbetalingAndel(
                aktører = setOf(barn2),
                prosent = BigDecimal.ZERO,
                fom = jan22,
                tom = aug22,
                årsak = Årsak.ETTERBETALING_3ÅR,
                søknadstidspunkt = des22.førsteDagIInneværendeMåned(),
            )

        // Act
        val perioderMedEndring =
            listOf(barn1, barn2)
                .map { aktør ->
                    EndringIEndretUtbetalingAndelUtil.lagEndringIEndretUbetalingAndelPerPersonTidslinje(
                        forrigeEndretAndelerForPerson = listOf(forrigeEndretAndelBarn1, forrigeEndretAndelBarn2).filter { endretAndel -> endretAndel.aktører.contains(aktør) },
                        nåværendeEndretAndelerForPerson = listOf(forrigeEndretAndelBarn1, forrigeEndretAndelBarn2.copy(årsak = Årsak.ETTERBETALING_3MND)).filter { endretAndel -> endretAndel.aktører.contains(aktør) },
                    )
                }.flatMap { it.tilPerioder() }
                .filter { it.verdi == true }

        // Assert
        assertTrue(perioderMedEndring.isEmpty())
    }

    @Test
    fun `Endring i endret utbetaling andel - skal returnere endret periode hvis et barn har reell endring selv om et annet barn kun har endring mellom etterbetalingsårsaker`() {
        // Arrange
        val barn1 = lagAktør()
        val barn2 = lagAktør()

        val forrigeEndretAndelBarn1 =
            lagEndretUtbetalingAndel(
                aktører = setOf(barn1),
                prosent = BigDecimal.ZERO,
                fom = jan22,
                tom = aug22,
                årsak = Årsak.DELT_BOSTED,
                søknadstidspunkt = des22.førsteDagIInneværendeMåned(),
                avtaletidspunktDeltBosted = jan22.førsteDagIInneværendeMåned(),
            )

        val forrigeEndretAndelBarn2 =
            lagEndretUtbetalingAndel(
                aktører = setOf(barn2),
                prosent = BigDecimal.ZERO,
                fom = jan22,
                tom = aug22,
                årsak = Årsak.ETTERBETALING_3ÅR,
                søknadstidspunkt = des22.førsteDagIInneværendeMåned(),
            )

        // Act
        val perioderMedEndring =
            listOf(barn1, barn2)
                .map {
                    EndringIEndretUtbetalingAndelUtil.lagEndringIEndretUbetalingAndelPerPersonTidslinje(
                        forrigeEndretAndelerForPerson = listOf(forrigeEndretAndelBarn1, forrigeEndretAndelBarn2).filter { endretAndel -> endretAndel.aktører.contains(it) },
                        nåværendeEndretAndelerForPerson = listOf(forrigeEndretAndelBarn1.copy(årsak = Årsak.ALLEREDE_UTBETALT), forrigeEndretAndelBarn2.copy(årsak = Årsak.ETTERBETALING_3MND)).filter { endretAndel -> endretAndel.aktører.contains(it) },
                    )
                }.flatMap { it.tilPerioder() }
                .filter { it.verdi == true }

        // Assert
        assertEquals(1, perioderMedEndring.size)
        assertEquals(jan22, perioderMedEndring.single().fom?.toYearMonth())
        assertEquals(aug22, perioderMedEndring.single().tom?.toYearMonth())
    }

    @Test
    fun `Endring i endret utbetaling andel - skal noen endrede perioder hvis eneste endring er at perioden blir lenger`() {
        // Arrange
        val barn = lagAktør()
        val forrigeEndretAndel =
            lagEndretUtbetalingAndel(
                aktører = setOf(barn),
                prosent = BigDecimal.ZERO,
                fom = jan22,
                tom = aug22,
                årsak = Årsak.DELT_BOSTED,
                søknadstidspunkt = des22.førsteDagIInneværendeMåned(),
                avtaletidspunktDeltBosted = jan22.førsteDagIInneværendeMåned(),
            )

        val nåværendeEndretAndel = forrigeEndretAndel.copy(tom = des22)

        // Act
        val perioderMedEndring =
            EndringIEndretUtbetalingAndelUtil
                .lagEndringIEndretUbetalingAndelPerPersonTidslinje(
                    forrigeEndretAndelerForPerson = listOf(forrigeEndretAndel),
                    nåværendeEndretAndelerForPerson = listOf(nåværendeEndretAndel),
                ).tilPerioder()
                .filter { it.verdi == true }

        // Assert
        assertEquals(1, perioderMedEndring.size)
        assertEquals(sep22, perioderMedEndring.single().fom?.toYearMonth())
        assertEquals(des22, perioderMedEndring.single().tom?.toYearMonth())
    }

    @Test
    fun `Endring i endret utbetaling andel - skal ha endrede perioder hvis endringsperiode oppstår i nåværende behandling`() {
        // Arrange
        val barn = lagAktør()
        val nåværendeEndretAndel =
            lagEndretUtbetalingAndel(
                aktører = setOf(barn),
                prosent = BigDecimal.ZERO,
                fom = jan22,
                tom = aug22,
                årsak = Årsak.DELT_BOSTED,
                søknadstidspunkt = des22.førsteDagIInneværendeMåned(),
                avtaletidspunktDeltBosted = jan22.førsteDagIInneværendeMåned(),
            )

        // Act
        val perioderMedEndring =
            EndringIEndretUtbetalingAndelUtil
                .lagEndringIEndretUbetalingAndelPerPersonTidslinje(
                    forrigeEndretAndelerForPerson = emptyList(),
                    nåværendeEndretAndelerForPerson = listOf(nåværendeEndretAndel),
                ).tilPerioder()
                .filter { it.verdi == true }

        // Assert
        assertEquals(1, perioderMedEndring.size)
        assertEquals(jan22, perioderMedEndring.single().fom?.toYearMonth())
        assertEquals(aug22, perioderMedEndring.single().tom?.toYearMonth())
    }
}
