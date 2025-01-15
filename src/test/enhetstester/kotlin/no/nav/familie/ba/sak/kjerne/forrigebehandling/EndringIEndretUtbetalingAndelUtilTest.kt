package no.nav.familie.ba.sak.kjerne.forrigebehandling

import lagEndretUtbetalingAndel
import lagPerson
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonth
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
    fun `Endring i endret utbetaling andel - skal ha endret periode hvis årsak er endret`() {
        val barn = lagPerson(type = PersonType.BARN)
        val forrigeEndretAndel =
            lagEndretUtbetalingAndel(
                person = barn,
                prosent = BigDecimal.ZERO,
                fom = jan22,
                tom = aug22,
                årsak = Årsak.ETTERBETALING_3ÅR,
                søknadstidspunkt = des22.førsteDagIInneværendeMåned(),
            )

        val nåværendeEndretAndel = forrigeEndretAndel.copy(årsak = Årsak.ALLEREDE_UTBETALT)

        val perioderMedEndring =
            EndringIEndretUtbetalingAndelUtil
                .lagEndringIEndretUbetalingAndelPerPersonTidslinje(
                    forrigeEndretAndelerForPerson = listOf(forrigeEndretAndel),
                    nåværendeEndretAndelerForPerson = listOf(nåværendeEndretAndel),
                ).perioder()
                .filter { it.innhold == true }

        assertEquals(1, perioderMedEndring.size)
        assertEquals(jan22, perioderMedEndring.single().fraOgMed.tilYearMonth())
        assertEquals(aug22, perioderMedEndring.single().tilOgMed.tilYearMonth())
    }

    @Test
    fun `Endring i endret utbetaling andel - skal ikke ha noen endrede perioder hvis kun prosent er endret`() {
        val barn = lagPerson(type = PersonType.BARN)
        val forrigeEndretAndel =
            lagEndretUtbetalingAndel(
                person = barn,
                prosent = BigDecimal.ZERO,
                fom = jan22,
                tom = aug22,
                årsak = Årsak.DELT_BOSTED,
                søknadstidspunkt = des22.førsteDagIInneværendeMåned(),
                avtaletidspunktDeltBosted = jan22.førsteDagIInneværendeMåned(),
            )

        val nåværendeEndretAndel = forrigeEndretAndel.copy(prosent = BigDecimal(100))

        val perioderMedEndring =
            EndringIEndretUtbetalingAndelUtil
                .lagEndringIEndretUbetalingAndelPerPersonTidslinje(
                    forrigeEndretAndelerForPerson = listOf(forrigeEndretAndel),
                    nåværendeEndretAndelerForPerson = listOf(nåværendeEndretAndel),
                ).perioder()
                .filter { it.innhold == true }

        assertTrue(perioderMedEndring.isEmpty())
    }

    @Test
    fun `Endring i endret utbetaling andel - skal returnere endret periode hvis et av to barn har endring på årsak`() {
        val barn1 = lagPerson(type = PersonType.BARN)
        val barn2 = lagPerson(type = PersonType.BARN)

        val forrigeEndretAndelBarn1 =
            lagEndretUtbetalingAndel(
                person = barn1,
                prosent = BigDecimal.ZERO,
                fom = jan22,
                tom = aug22,
                årsak = Årsak.DELT_BOSTED,
                søknadstidspunkt = des22.førsteDagIInneværendeMåned(),
                avtaletidspunktDeltBosted = jan22.førsteDagIInneværendeMåned(),
            )

        val forrigeEndretAndelBarn2 =
            lagEndretUtbetalingAndel(
                person = barn2,
                prosent = BigDecimal.ZERO,
                fom = jan22,
                tom = aug22,
                årsak = Årsak.ETTERBETALING_3ÅR,
                søknadstidspunkt = des22.førsteDagIInneværendeMåned(),
            )

        val perioderMedEndring =
            listOf(barn1, barn2)
                .map {
                    EndringIEndretUtbetalingAndelUtil.lagEndringIEndretUbetalingAndelPerPersonTidslinje(
                        forrigeEndretAndelerForPerson = listOf(forrigeEndretAndelBarn1, forrigeEndretAndelBarn2).filter { endretAndel -> endretAndel.person == it },
                        nåværendeEndretAndelerForPerson = listOf(forrigeEndretAndelBarn1, forrigeEndretAndelBarn2.copy(årsak = Årsak.ALLEREDE_UTBETALT)).filter { endretAndel -> endretAndel.person == it },
                    )
                }.flatMap { it.perioder() }
                .filter { it.innhold == true }

        assertEquals(1, perioderMedEndring.size)
        assertEquals(jan22, perioderMedEndring.single().fraOgMed.tilYearMonth())
        assertEquals(aug22, perioderMedEndring.single().tilOgMed.tilYearMonth())
    }

    @Test
    fun `Endring i endret utbetaling andel - skal noen endrede perioder hvis eneste endring er at perioden blir lenger`() {
        val barn = lagPerson(type = PersonType.BARN)
        val forrigeEndretAndel =
            lagEndretUtbetalingAndel(
                person = barn,
                prosent = BigDecimal.ZERO,
                fom = jan22,
                tom = aug22,
                årsak = Årsak.DELT_BOSTED,
                søknadstidspunkt = des22.førsteDagIInneværendeMåned(),
                avtaletidspunktDeltBosted = jan22.førsteDagIInneværendeMåned(),
            )

        val nåværendeEndretAndel = forrigeEndretAndel.copy(tom = des22)

        val perioderMedEndring =
            EndringIEndretUtbetalingAndelUtil
                .lagEndringIEndretUbetalingAndelPerPersonTidslinje(
                    forrigeEndretAndelerForPerson = listOf(forrigeEndretAndel),
                    nåværendeEndretAndelerForPerson = listOf(nåværendeEndretAndel),
                ).perioder()
                .filter { it.innhold == true }

        assertEquals(1, perioderMedEndring.size)
        assertEquals(sep22, perioderMedEndring.single().fraOgMed.tilYearMonth())
        assertEquals(des22, perioderMedEndring.single().tilOgMed.tilYearMonth())
    }

    @Test
    fun `Endring i endret utbetaling andel - skal ha endrede perioder hvis endringsperiode oppstår i nåværende behandling`() {
        val barn = lagPerson(type = PersonType.BARN)
        val nåværendeEndretAndel =
            lagEndretUtbetalingAndel(
                person = barn,
                prosent = BigDecimal.ZERO,
                fom = jan22,
                tom = aug22,
                årsak = Årsak.DELT_BOSTED,
                søknadstidspunkt = des22.førsteDagIInneværendeMåned(),
                avtaletidspunktDeltBosted = jan22.førsteDagIInneværendeMåned(),
            )

        val perioderMedEndring =
            EndringIEndretUtbetalingAndelUtil
                .lagEndringIEndretUbetalingAndelPerPersonTidslinje(
                    forrigeEndretAndelerForPerson = emptyList(),
                    nåværendeEndretAndelerForPerson = listOf(nåværendeEndretAndel),
                ).perioder()
                .filter { it.innhold == true }

        assertEquals(1, perioderMedEndring.size)
        assertEquals(jan22, perioderMedEndring.single().fraOgMed.tilYearMonth())
        assertEquals(aug22, perioderMedEndring.single().tilOgMed.tilYearMonth())
    }
}
