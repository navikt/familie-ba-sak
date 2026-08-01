package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagPersonopplysningGrunnlag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

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
    }
}
