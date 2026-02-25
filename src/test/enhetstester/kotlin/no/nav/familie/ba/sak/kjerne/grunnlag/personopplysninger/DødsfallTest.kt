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
            val annenDødsfall = dødsfall.copy(person = tilfeldigPerson())
            assert(dødsfall != annenDødsfall)
        }

        @Test
        fun `skal returnere false hvis dødsfallDato er ulik`() {
            val annenDødsfall = dødsfall.copy(dødsfallDato = LocalDate.now().minusDays(1))
            assert(dødsfall != annenDødsfall)
        }

        @Test
        fun `skal returnere false hvis dødsfallAdresse er ulik`() {
            val annenDødsfall = dødsfall.copy(dødsfallAdresse = "Testveien 2")
            assert(dødsfall != annenDødsfall)
        }

        @Test
        fun `skal returnere false hvis dødsfallPostnummer er ulik`() {
            val annenDødsfall = dødsfall.copy(dødsfallPostnummer = "4321")
            assert(dødsfall != annenDødsfall)
        }

        @Test
        fun `skal returnere false hvis dødsfallPoststed er ulik`() {
            val annenDødsfall = dødsfall.copy(dødsfallPoststed = "Ingensteds")
            assert(dødsfall != annenDødsfall)
        }

        @Test
        fun `skal returnere false hvis manuellRegistrert er ulik`() {
            val annenDødsfall = dødsfall.copy(manuellRegistrert = true)
            assert(dødsfall != annenDødsfall)
        }

        @Test
        fun `skal returnere true hvis alle felter er like`() {
            val annenDødsfall = dødsfall.copy(id = 1)
            assert(dødsfall == annenDødsfall)
        }
    }
}
