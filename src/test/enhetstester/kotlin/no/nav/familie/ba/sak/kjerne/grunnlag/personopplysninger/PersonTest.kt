package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.datagenerator.lagGrArbeidsforhold
import no.nav.familie.ba.sak.datagenerator.lagGrOpphold
import no.nav.familie.ba.sak.datagenerator.lagGrSivilstand
import no.nav.familie.ba.sak.datagenerator.lagGrStatsborgerskap
import no.nav.familie.ba.sak.datagenerator.lagGrVegadresseBostedsadresse
import no.nav.familie.ba.sak.datagenerator.lagGrVegadresseDeltBosted
import no.nav.familie.ba.sak.datagenerator.lagGrVegadresseOppholdsadresse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.randomAktør
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PersonTest {
    @Nested
    inner class PersonopplysningerErLike {
        private val aktivtPersonopplysningGrunnlag = lagPersonopplysningGrunnlag(id = 0)
        private val nyttPersonopplysningGrunnlag = lagPersonopplysningGrunnlag(id = 1)

        private val person = lagPerson(personopplysningGrunnlag = aktivtPersonopplysningGrunnlag)
        private val personMedNyId = { person.copy(id = 1, personopplysningGrunnlag = nyttPersonopplysningGrunnlag) }

        @Test
        fun `skal returnere true når alle felter er identiske`() {
            assertThat(person.personopplysningerErLike(personMedNyId())).isTrue()
        }

        @Test
        fun `skal returnere false når aktør er forskjellig`() {
            // Arrange
            val nyPerson = personMedNyId().copy(aktør = randomAktør())

            // Act & Assert
            assertThat(person.personopplysningerErLike(nyPerson)).isFalse()
        }

        @Test
        fun `skal returnere false når navn er forskjellig`() {
            // Arrange
            val nyPerson = personMedNyId().copy(navn = "Annet Navn")

            // Act & Assert
            assertThat(person.personopplysningerErLike(nyPerson)).isFalse()
        }

        @Test
        fun `skal returnere false når fødselsdato er forskjellig`() {
            // Arrange
            val nyPerson = personMedNyId().copy(fødselsdato = LocalDate.now().minusYears(18))

            // Act & Assert
            assertThat(person.personopplysningerErLike(nyPerson)).isFalse()
        }

        @Test
        fun `skal returnere false når kjønn er forskjellig`() {
            // Arrange
            val nyPerson = personMedNyId().copy(kjønn = Kjønn.MANN)

            // Act & Assert
            assertThat(person.personopplysningerErLike(nyPerson)).isFalse()
        }

        @Test
        fun `skal returnere false når målform er forskjellig`() {
            // Arrange
            val nyPerson = personMedNyId().copy(målform = Målform.NN)

            // Act & Assert
            assertThat(person.personopplysningerErLike(nyPerson)).isFalse()
        }

        @Test
        fun `skal returnere false når type er forskjellig`() {
            // Arrange
            val nyPerson = personMedNyId().copy(type = PersonType.ANNENPART)

            // Act & Assert
            assertThat(person.personopplysningerErLike(nyPerson)).isFalse()
        }

        @Test
        fun `skal returnere false når harFalskIdentitet er forskjellig`() {
            // Arrange
            val nyPerson = personMedNyId().copy(harFalskIdentitet = true)

            // Act & Assert
            assertThat(person.personopplysningerErLike(nyPerson)).isFalse()
        }

        @Test
        fun `skal returnere false når dødsfall er forskjellig`() {
            // Arrange
            val nyPerson =
                personMedNyId().apply {
                    dødsfall = lagDødsfall(person = this, LocalDate.of(2020, 1, 1))
                }

            // Act & Assert
            assertThat(person.personopplysningerErLike(nyPerson)).isFalse()
        }

        @Test
        fun `skal returnere false når opphold er forskjellige`() {
            // Arrange
            val nyPerson =
                personMedNyId().apply {
                    opphold = mutableListOf(lagGrOpphold(person = this))
                }

            // Act & Assert
            assertThat(person.personopplysningerErLike(nyPerson)).isFalse()
        }

        @Test
        fun `skal returnere false når bostedsadresser er forskjellige`() {
            // Arrange
            val nyPerson =
                personMedNyId().apply {
                    bostedsadresser = mutableListOf(lagGrVegadresseBostedsadresse(person = this))
                }

            // Act & Assert
            assertThat(person.personopplysningerErLike(nyPerson)).isFalse()
        }

        @Test
        fun `skal returnere false når oppholdsadresser er forskjellige`() {
            // Arrange
            val nyPerson =
                personMedNyId().apply {
                    oppholdsadresser = mutableListOf(lagGrVegadresseOppholdsadresse(person = this))
                }

            // Act & Assert
            assertThat(person.personopplysningerErLike(nyPerson)).isFalse()
        }

        @Test
        fun `skal returnere false når deltBosted er forskjellige`() {
            // Arrange
            val nyPerson =
                personMedNyId().apply {
                    deltBosted = mutableListOf(lagGrVegadresseDeltBosted(person = this))
                }

            // Act & Assert
            assertThat(person.personopplysningerErLike(nyPerson)).isFalse()
        }

        @Test
        fun `skal returnere false når sivilstander er forskjellige`() {
            // Arrange
            val nyPerson =
                personMedNyId().apply {
                    sivilstander = mutableListOf(lagGrSivilstand(person = this))
                }

            // Act & Assert
            assertThat(person.personopplysningerErLike(nyPerson)).isFalse()
        }

        @Test
        fun `skal returnere false når statsborgerskap er forskjellige`() {
            // Arrange
            val nyPerson =
                personMedNyId().apply {
                    statsborgerskap = mutableListOf(lagGrStatsborgerskap(person = this))
                }

            // Act & Assert
            assertThat(person.personopplysningerErLike(nyPerson)).isFalse()
        }

        @Test
        fun `skal returnere false når arbeidsforhold er forskjellige`() {
            // Arrange
            val nyPerson =
                personMedNyId().apply {
                    arbeidsforhold = mutableListOf(lagGrArbeidsforhold(person = this))
                }

            // Act & Assert
            assertThat(person.personopplysningerErLike(nyPerson)).isFalse()
        }
    }
}
