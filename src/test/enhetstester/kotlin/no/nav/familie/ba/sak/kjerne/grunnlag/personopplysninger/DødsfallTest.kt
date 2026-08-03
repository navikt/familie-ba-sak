package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DødsfallTest {
    private val dødsfall =
        Dødsfall(
            id = 0,
            person = tilfeldigPerson(),
            dødsfallDato = LocalDate.now(),
            dødsfallAdresse = "Testveien 1",
            dødsfallPostnummer = "1234",
            dødsfallPoststed = "Teststed",
            manuellRegistrert = false,
        )

    @Nested
    inner class Equals {
        @Test
        fun `skal returnere false hvis person er ulik`() {
            // Arrange
            val annenDødsfall = dødsfall.copy(person = tilfeldigPerson())

            // Act & Assert
            assert(dødsfall != annenDødsfall)
        }

        @Test
        fun `skal returnere false hvis dødsfallDato er ulik`() {
            // Arrange
            val annenDødsfall = dødsfall.copy(dødsfallDato = LocalDate.now().minusDays(1))

            // Act & Assert
            assert(dødsfall != annenDødsfall)
        }

        @Test
        fun `skal returnere false hvis dødsfallAdresse er ulik`() {
            // Arrange
            val annenDødsfall = dødsfall.copy(dødsfallAdresse = "Testveien 2")

            // Act & Assert
            assert(dødsfall != annenDødsfall)
        }

        @Test
        fun `skal returnere false hvis dødsfallPostnummer er ulik`() {
            // Arrange
            val annenDødsfall = dødsfall.copy(dødsfallPostnummer = "4321")

            // Act & Assert
            assert(dødsfall != annenDødsfall)
        }

        @Test
        fun `skal returnere false hvis dødsfallPoststed er ulik`() {
            // Arrange
            val annenDødsfall = dødsfall.copy(dødsfallPoststed = "Ingensteds")

            // Act & Assert
            assert(dødsfall != annenDødsfall)
        }

        @Test
        fun `skal returnere false hvis manuellRegistrert er ulik`() {
            // Arrange
            val annenDødsfall = dødsfall.copy(manuellRegistrert = true)

            // Act & Assert
            assert(dødsfall != annenDødsfall)
        }

        @Test
        fun `skal returnere true hvis alle felter er like`() {
            // Arrange
            val annenDødsfall = dødsfall.copy(id = 1)

            // Act & Assert
            assert(dødsfall == annenDødsfall)
        }
    }
}
