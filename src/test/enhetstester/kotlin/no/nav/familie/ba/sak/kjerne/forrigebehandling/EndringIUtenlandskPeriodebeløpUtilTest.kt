package no.nav.familie.ba.sak.kjerne.forrigebehandling

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagUtenlandskPeriodebeløp
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

class EndringIUtenlandskPeriodebeløpUtilTest {
    private val barn1Aktør = randomAktør()
    val jan22 = YearMonth.of(2022, 1)
    val mai22 = YearMonth.of(2022, 5)

    @Test
    fun `Endring i utenlandskperiode beløp - skal ikke returnere noen endrede perioder når ingenting endrer seg`() {
        // Arrange
        val forrigeBehandling = lagBehandling()
        val nåværendeBehandling = lagBehandling()
        val forrigeUtenlandskPeriodebeløp =
            lagUtenlandskPeriodebeløp(
                behandlingId = forrigeBehandling.id,
                barnAktører = setOf(barn1Aktør),
                valutakode = "NOK",
                beløp = BigDecimal(500),
                intervall = Intervall.MÅNEDLIG,
                utbetalingsland = "NORGE",
                fom = jan22,
                tom = mai22,
            )

        val nåværendeUtenlandskPeriodebeløp = forrigeUtenlandskPeriodebeløp.copy().apply { behandlingId = nåværendeBehandling.id }

        // Act
        val perioderMedEndring =
            EndringIUtenlandskPeriodebeløpUtil
                .lagEndringIUtenlandskPeriodebeløpForPersonTidslinje(
                    nåværendeUtenlandskPeriodebeløpForPerson = listOf(forrigeUtenlandskPeriodebeløp),
                    forrigeUtenlandskPeriodebeløpForPerson = listOf(nåværendeUtenlandskPeriodebeløp),
                ).tilPerioder()
                .filter { it.verdi == true }

        // Assert
        Assertions.assertTrue(perioderMedEndring.isEmpty())
    }

    @Test
    fun `Endring i utenlandskperiode beløp - skal returnere endret periode når intervall endrer seg`() {
        // Arrange
        val forrigeBehandling = lagBehandling()
        val nåværendeBehandling = lagBehandling()
        val forrigeUtenlandskPeriodebeløp =
            lagUtenlandskPeriodebeløp(
                behandlingId = forrigeBehandling.id,
                barnAktører = setOf(barn1Aktør),
                valutakode = "NOK",
                beløp = BigDecimal(500),
                intervall = Intervall.MÅNEDLIG,
                utbetalingsland = "NORGE",
                fom = jan22,
                tom = mai22,
            )

        val nåværendeUtenlandskPeriodebeløp = forrigeUtenlandskPeriodebeløp.copy(intervall = Intervall.ÅRLIG).apply { behandlingId = nåværendeBehandling.id }

        // Act
        val perioderMedEndring =
            EndringIUtenlandskPeriodebeløpUtil
                .lagEndringIUtenlandskPeriodebeløpForPersonTidslinje(
                    nåværendeUtenlandskPeriodebeløpForPerson = listOf(forrigeUtenlandskPeriodebeløp),
                    forrigeUtenlandskPeriodebeløpForPerson = listOf(nåværendeUtenlandskPeriodebeløp),
                ).tilPerioder()
                .filter { it.verdi == true }

        // Assert
        Assertions.assertEquals(1, perioderMedEndring.size)
        Assertions.assertEquals(jan22, perioderMedEndring.single().fom?.toYearMonth())
        Assertions.assertEquals(mai22, perioderMedEndring.single().tom?.toYearMonth())
    }

    @Test
    fun `Endring i utenlandskperiode beløp - skal returnere endret periode når beløp endrer seg`() {
        // Arrange
        val forrigeBehandling = lagBehandling()
        val nåværendeBehandling = lagBehandling()
        val forrigeUtenlandskPeriodebeløp =
            lagUtenlandskPeriodebeløp(
                behandlingId = forrigeBehandling.id,
                barnAktører = setOf(barn1Aktør),
                valutakode = "NOK",
                beløp = BigDecimal(500),
                intervall = Intervall.MÅNEDLIG,
                utbetalingsland = "NORGE",
                fom = jan22,
                tom = mai22,
            )

        val nåværendeUtenlandskPeriodebeløp = forrigeUtenlandskPeriodebeløp.copy(beløp = BigDecimal.valueOf(200)).apply { behandlingId = nåværendeBehandling.id }

        // Act
        val perioderMedEndring =
            EndringIUtenlandskPeriodebeløpUtil
                .lagEndringIUtenlandskPeriodebeløpForPersonTidslinje(
                    nåværendeUtenlandskPeriodebeløpForPerson = listOf(forrigeUtenlandskPeriodebeløp),
                    forrigeUtenlandskPeriodebeløpForPerson = listOf(nåværendeUtenlandskPeriodebeløp),
                ).tilPerioder()
                .filter { it.verdi == true }

        // Assert
        Assertions.assertEquals(1, perioderMedEndring.size)
        Assertions.assertEquals(jan22, perioderMedEndring.single().fom?.toYearMonth())
        Assertions.assertEquals(mai22, perioderMedEndring.single().tom?.toYearMonth())
    }

    @Test
    fun `Endring i utenlandskperiode beløp - skal returnere endret periode når utbetalingsland endrer seg`() {
        // Arrange
        val forrigeBehandling = lagBehandling()
        val nåværendeBehandling = lagBehandling()
        val forrigeUtenlandskPeriodebeløp =
            lagUtenlandskPeriodebeløp(
                behandlingId = forrigeBehandling.id,
                barnAktører = setOf(barn1Aktør),
                valutakode = "NOK",
                beløp = BigDecimal(500),
                intervall = Intervall.MÅNEDLIG,
                utbetalingsland = "NORGE",
                fom = jan22,
                tom = mai22,
            )

        val nåværendeUtenlandskPeriodebeløp = forrigeUtenlandskPeriodebeløp.copy(utbetalingsland = "SVERIGE").apply { behandlingId = nåværendeBehandling.id }

        // Act
        val perioderMedEndring =
            EndringIUtenlandskPeriodebeløpUtil
                .lagEndringIUtenlandskPeriodebeløpForPersonTidslinje(
                    nåværendeUtenlandskPeriodebeløpForPerson = listOf(forrigeUtenlandskPeriodebeløp),
                    forrigeUtenlandskPeriodebeløpForPerson = listOf(nåværendeUtenlandskPeriodebeløp),
                ).tilPerioder()
                .filter { it.verdi == true }

        // Assert
        Assertions.assertEquals(1, perioderMedEndring.size)
        Assertions.assertEquals(jan22, perioderMedEndring.single().fom?.toYearMonth())
        Assertions.assertEquals(mai22, perioderMedEndring.single().tom?.toYearMonth())
    }

    @Test
    fun `Endring i utenlandskperiode beløp - skal returnere endret periode når valutakode endrer seg`() {
        // Arrange
        val forrigeBehandling = lagBehandling()
        val nåværendeBehandling = lagBehandling()
        val forrigeUtenlandskPeriodebeløp =
            lagUtenlandskPeriodebeløp(
                behandlingId = forrigeBehandling.id,
                barnAktører = setOf(barn1Aktør),
                valutakode = "NOK",
                beløp = BigDecimal(500),
                intervall = Intervall.MÅNEDLIG,
                utbetalingsland = "NORGE",
                fom = jan22,
                tom = mai22,
            )

        val nåværendeUtenlandskPeriodebeløp = forrigeUtenlandskPeriodebeløp.copy(valutakode = "SEK").apply { behandlingId = nåværendeBehandling.id }

        // Act
        val perioderMedEndring =
            EndringIUtenlandskPeriodebeløpUtil
                .lagEndringIUtenlandskPeriodebeløpForPersonTidslinje(
                    nåværendeUtenlandskPeriodebeløpForPerson = listOf(forrigeUtenlandskPeriodebeløp),
                    forrigeUtenlandskPeriodebeløpForPerson = listOf(nåværendeUtenlandskPeriodebeløp),
                ).tilPerioder()
                .filter { it.verdi == true }

        // Assert
        Assertions.assertEquals(1, perioderMedEndring.size)
        Assertions.assertEquals(jan22, perioderMedEndring.single().fom?.toYearMonth())
        Assertions.assertEquals(mai22, perioderMedEndring.single().tom?.toYearMonth())
    }
}
