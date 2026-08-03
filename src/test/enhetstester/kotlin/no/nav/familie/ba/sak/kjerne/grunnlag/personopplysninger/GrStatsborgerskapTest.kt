package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GrStatsborgerskapTest {
    private val statsborgerskap =
        GrStatsborgerskap(
            id = 0,
            person = tilfeldigPerson(),
            gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.of(2000, 1, 1), tom = LocalDate.of(2000, 12, 31)),
            landkode = "NO",
            medlemskap = Medlemskap.NORDEN,
        )

    @Nested
    inner class Equals {
        @Test
        fun `skal returnere false hvis person er ulik`() {
            // Arrange
            val annetStatsborgerskap = statsborgerskap.copy(person = tilfeldigPerson())

            // Act & Assert
            assert(statsborgerskap != annetStatsborgerskap)
        }

        @Test
        fun `skal returnere false hvis periode er ulik`() {
            // Arrange
            val annetStatsborgerskap = statsborgerskap.copy(gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.of(2001, 1, 1), tom = LocalDate.of(2001, 12, 31)))

            // Act & Assert
            assert(statsborgerskap != annetStatsborgerskap)
        }

        @Test
        fun `skal returnere false hvis landkode er ulik`() {
            // Arrange
            val annetStatsborgerskap = statsborgerskap.copy(landkode = "SE")

            // Act & Assert
            assert(statsborgerskap != annetStatsborgerskap)
        }

        @Test
        fun `skal returnere false hvis medlemskap er ulik`() {
            // Arrange
            val annetStatsborgerskap = statsborgerskap.copy(medlemskap = Medlemskap.EØS)

            // Act & Assert
            assert(statsborgerskap != annetStatsborgerskap)
        }

        @Test
        fun `skal returnere true hvis alle felter er like`() {
            // Arrange
            val annetStatsborgerskap = statsborgerskap.copy(id = 1)

            // Act & Assert
            assert(statsborgerskap == annetStatsborgerskap)
        }
    }
}
