package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.rangeTo
import no.nav.familie.ba.sak.kjerne.beregning.Prosent.alt
import no.nav.familie.ba.sak.kjerne.beregning.Prosent.halvparten
import no.nav.familie.ba.sak.kjerne.eøs.util.barn
import no.nav.familie.ba.sak.kjerne.eøs.util.død
import no.nav.familie.ba.sak.kjerne.eøs.util.født
import no.nav.familie.ba.sak.kjerne.tidslinje.util.aug
import no.nav.familie.ba.sak.kjerne.tidslinje.util.des
import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

class BeregnetAndelDslTest {
    @Test
    fun `sjekk at andel bygges riktig`() {
        // Arrange
        val barn = barn født 14.des(2019) død 9.des(2024)

        // Act
        val andel = barn får alt av 1054 i feb(2020)..aug(2020)

        // Assert
        Assertions.assertEquals(barn, andel.person)
        Assertions.assertEquals(BigDecimal.valueOf(100), andel.prosent)
        Assertions.assertEquals(1054, andel.beløp)
        Assertions.assertEquals(1054, andel.sats)
        Assertions.assertEquals(YearMonth.of(2020, 2), andel.stønadFom)
        Assertions.assertEquals(YearMonth.of(2020, 8), andel.stønadTom)
    }

    @Test
    fun `sjekk at halvparten av oddetall rundes opp`() {
        // Arrange
        val barn = barn født 14.des(2019)

        // Act
        val andel = barn får halvparten av 1723

        // Assert
        Assertions.assertEquals(BigDecimal.valueOf(50), andel.prosent)
        Assertions.assertEquals(862, andel.beløp)
    }
}
