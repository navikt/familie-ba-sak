package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.datagenerator.lagPersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.Personident
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.LocalDate
import java.time.LocalDateTime

class PersonDtoTest {
    @Test
    fun `historiskeIdenter skal kun inkludere identer med gjelderTil lik eller etter eldste barn sin fødselsdato`() {
        // Arrange
        val aktør = Aktør(aktørId = "123")
        val eldsteBarnFødselsdato = LocalDate.of(2020, 1, 1)

        val personopplysningGrunnlag =
            lagPersonopplysningGrunnlag(
                id = 1L,
                behandlingId = 1L,
                lagPersoner = { grunnlag ->
                    setOf(
                        Person(
                            aktør = aktør,
                            type = PersonType.SØKER,
                            fødselsdato = LocalDate.of(1980, 1, 1),
                            navn = "Test Testesen",
                            kjønn = Kjønn.KVINNE,
                            personopplysningGrunnlag = grunnlag,
                        ),
                    )
                },
            )

        aktør.personidenter.add(Personident(fødselsnummer = "11111111111", aktør = aktør, aktiv = false, gjelderTil = LocalDateTime.of(2019, 12, 1, 0, 0)))
        aktør.personidenter.add(Personident(fødselsnummer = "22222222222", aktør = aktør, aktiv = false, gjelderTil = LocalDateTime.of(2020, 1, 1, 0, 0)))
        aktør.personidenter.add(Personident(fødselsnummer = "33333333333", aktør = aktør, aktiv = true, gjelderTil = null))

        // Act
        val restPerson = personopplysningGrunnlag.søker.tilPersonDto(eldsteBarnsFødselsdato = eldsteBarnFødselsdato)

        // Assert
        assertEquals(listOf("22222222222", "33333333333"), restPerson.historiskeIdenter)
    }
}
