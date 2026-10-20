package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.datagenerator.lagGrVegadresseBostedsadresse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagPersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.bostedsadresse.GrBostedsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.bostedsadresse.GrVegadresseBostedsadresse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PersonopplysningGrunnlagTest {
    @Nested
    inner class HarRelevantEndring {
        private val søkerPerson = lagPerson(id = 0, type = PersonType.SØKER)
        private val barnPerson = lagPerson(id = 1, type = PersonType.BARN)

        @Test
        fun `skal returnere false når det ikke er noen endringer`() {
            // Arrange
            val personopplysningGrunnlag = lagPersonopplysningGrunnlag(id = 0) { setOf(søkerPerson) }
            val nyttPersonopplysningGrunnlag = personopplysningGrunnlag.copy(id = 1, personer = mutableSetOf(søkerPerson))

            // Act & Assert
            assertThat(personopplysningGrunnlag.harRelevantEndring(nyttPersonopplysningGrunnlag)).isFalse()
        }

        @Test
        fun `skal returnere true når en person i personopplysninggrunnlaget er endret`() {
            // Arrange
            val personopplysningGrunnlag = lagPersonopplysningGrunnlag(id = 0) { setOf(søkerPerson) }
            val nyttPersonopplysningGrunnlag = personopplysningGrunnlag.copy(id = 1, personer = mutableSetOf(søkerPerson.copy(navn = "Endret Navn")))

            // Act & Assert
            assertThat(personopplysningGrunnlag.harRelevantEndring(nyttPersonopplysningGrunnlag)).isTrue()
        }

        @Test
        fun `skal returnere true når en person er lagt til i personopplysninggrunnlaget`() {
            // Arrange
            val personopplysningGrunnlag = lagPersonopplysningGrunnlag(id = 0) { setOf(søkerPerson) }
            val nyttPersonopplysningGrunnlag = personopplysningGrunnlag.copy(id = 1, personer = mutableSetOf(søkerPerson, barnPerson))

            // Act & Assert
            assertThat(personopplysningGrunnlag.harRelevantEndring(nyttPersonopplysningGrunnlag)).isTrue()
        }

        @Test
        fun `skal returnere true når en person er fjernet fra personopplysninggrunnlaget`() {
            // Arrange
            val personopplysningGrunnlag = lagPersonopplysningGrunnlag(id = 0) { setOf(søkerPerson, barnPerson) }
            val nyttPersonopplysningGrunnlag = personopplysningGrunnlag.copy(id = 1, personer = mutableSetOf(søkerPerson))

            // Act & Assert
            assertThat(personopplysningGrunnlag.harRelevantEndring(nyttPersonopplysningGrunnlag)).isTrue()
        }

        @Nested
        inner class Bostedsadresser {
            private val gammelLeilighet =
                lagGrVegadresseBostedsadresse(
                    matrikkelId = 123L,
                    adressenavn = "Testgate",
                    husnummer = "1",
                    husbokstav = "A",
                    bruksenhetsnummer = "H0101",
                    postnummer = "0001",
                    periode = DatoIntervallEntitet(fom = LocalDate.of(2020, 1, 1)),
                )

            private fun lagGrunnlag(
                id: Long,
                vararg adresser: GrBostedsadresse,
            ) = lagPersonopplysningGrunnlag(id = id) { grunnlag ->
                setOf(
                    søkerPerson.copy(personopplysningGrunnlag = grunnlag, bostedsadresser = adresser.toMutableList()),
                )
            }

            @Test
            fun `skal returnere false når bostedsadressene er like`() {
                // Arrange
                val personopplysningGrunnlag = lagGrunnlag(0, gammelLeilighet)
                val nyttPersonopplysningGrunnlag = lagGrunnlag(1, gammelLeilighet.kopi())

                // Act & Assert
                assertThat(nyttPersonopplysningGrunnlag.harRelevantEndring(personopplysningGrunnlag)).isFalse()
            }

            @Test
            fun `skal returnere true når søker har flyttet til ny leilighet på samme adresse`() {
                // Arrange
                val personopplysningGrunnlag = lagGrunnlag(0, gammelLeilighet)
                val nyttPersonopplysningGrunnlag =
                    lagGrunnlag(
                        1,
                        gammelLeilighet.kopi(periode = DatoIntervallEntitet(fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2026, 8, 23))),
                        gammelLeilighet.kopi(bruksenhetsnummer = "H0202", periode = DatoIntervallEntitet(fom = LocalDate.of(2026, 8, 24))),
                    )

                // Act & Assert
                assertThat(nyttPersonopplysningGrunnlag.harRelevantEndring(personopplysningGrunnlag)).isTrue()
            }

            @Test
            fun `skal returnere true når bare perioden på en bostedsadresse er endret`() {
                // Arrange
                val personopplysningGrunnlag = lagGrunnlag(0, gammelLeilighet)
                val nyttPersonopplysningGrunnlag =
                    lagGrunnlag(1, gammelLeilighet.kopi(periode = DatoIntervallEntitet(fom = LocalDate.of(2020, 1, 1), tom = LocalDate.of(2026, 8, 23))))

                // Act & Assert
                assertThat(nyttPersonopplysningGrunnlag.harRelevantEndring(personopplysningGrunnlag)).isTrue()
            }

            @Test
            fun `skal returnere true når søker har flyttet tilbake til nøyaktig samme adresse`() {
                // Arrange
                val annenLeilighet = gammelLeilighet.kopi(bruksenhetsnummer = "H0202", periode = DatoIntervallEntitet(fom = LocalDate.of(2023, 1, 1)))
                val personopplysningGrunnlag = lagGrunnlag(0, gammelLeilighet, annenLeilighet)
                val nyttPersonopplysningGrunnlag =
                    lagGrunnlag(
                        1,
                        gammelLeilighet.kopi(),
                        annenLeilighet.kopi(),
                        gammelLeilighet.kopi(periode = DatoIntervallEntitet(fom = LocalDate.of(2026, 8, 24))),
                    )

                // Act & Assert
                assertThat(nyttPersonopplysningGrunnlag.harRelevantEndring(personopplysningGrunnlag)).isTrue()
            }

            @Test
            fun `skal returnere false når bostedsadressene er like men i ulik rekkefølge`() {
                // Arrange
                val annenLeilighet = gammelLeilighet.kopi(bruksenhetsnummer = "H0202", periode = DatoIntervallEntitet(fom = LocalDate.of(2023, 1, 1)))
                val personopplysningGrunnlag = lagGrunnlag(0, gammelLeilighet, annenLeilighet)
                val nyttPersonopplysningGrunnlag = lagGrunnlag(1, annenLeilighet.kopi(), gammelLeilighet.kopi())

                // Act & Assert
                assertThat(nyttPersonopplysningGrunnlag.harRelevantEndring(personopplysningGrunnlag)).isFalse()
            }

            @Test
            fun `skal returnere true når kommunenummeret på en bostedsadresse er endret`() {
                // Arrange
                val personopplysningGrunnlag = lagGrunnlag(0, gammelLeilighet)
                val nyttPersonopplysningGrunnlag = lagGrunnlag(1, gammelLeilighet.kopi(kommunenummer = "5601"))

                // Act & Assert
                assertThat(nyttPersonopplysningGrunnlag.harRelevantEndring(personopplysningGrunnlag)).isTrue()
            }

            private fun GrVegadresseBostedsadresse.kopi(
                bruksenhetsnummer: String? = this.bruksenhetsnummer,
                kommunenummer: String? = this.kommunenummer,
                periode: DatoIntervallEntitet? = this.periode,
            ) = lagGrVegadresseBostedsadresse(
                matrikkelId = this.matrikkelId,
                husnummer = husnummer,
                husbokstav = husbokstav,
                bruksenhetsnummer = bruksenhetsnummer,
                adressenavn = this.adressenavn,
                kommunenummer = kommunenummer,
                tilleggsnavn = this.tilleggsnavn,
                postnummer = postnummer,
                periode = periode,
                poststed = poststed,
            )
        }
    }
}
