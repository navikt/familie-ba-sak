package no.nav.familie.ba.sak.kjerne.falskidentitet

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlFalskIdentitet
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FalskIdentitetServiceTest {
    private val personRepository = mockk<PersonRepository>()
    private val pdlRestKlient = mockk<PdlRestKlient>()
    private val falskIdentitetService =
        FalskIdentitetService(
            personRepository = personRepository,
            pdlRestKlient = pdlRestKlient,
        )

    @Nested
    inner class HentFalskIdentitet {
        @Test
        fun `skal hente og returnere FalskIdentitet dersom aktør er falsk identitet med tidligere behandling`() {
            // Arrange
            val fødselsdato = LocalDate.of(1990, 1, 1)
            val person =
                lagPerson(
                    personIdent = PersonIdent(ident = randomFnr(foedselsdato = fødselsdato)),
                    navn = "Falsk Falskesen",
                    kjønn = Kjønn.MANN,
                )

            every { pdlRestKlient.hentFalskIdentitet(person.aktør.aktivFødselsnummer()) } returns PdlFalskIdentitet(erFalsk = true)
            every { personRepository.findByAktør(person.aktør) } returns listOf(person)

            // Act
            val falskIdentitet = falskIdentitetService.hentFalskIdentitet(person.aktør)

            // Assert
            assertThat(falskIdentitet).isNotNull
            assertThat(falskIdentitet!!.navn).isEqualTo(person.navn)
            assertThat(falskIdentitet.fødselsdato).isEqualTo(person.fødselsdato)
            assertThat(falskIdentitet.kjønn).isEqualTo(person.kjønn)
        }

        @Test
        fun `skal hente og returnere FalskIdentitet dersom aktør er falsk identitet uten tidligere behandling`() {
            // Arrange
            val aktør = lagAktør()

            every { pdlRestKlient.hentFalskIdentitet(aktør.aktivFødselsnummer()) } returns PdlFalskIdentitet(erFalsk = true)
            every { personRepository.findByAktør(aktør) } returns emptyList()

            // Act
            val falskIdentitet = falskIdentitetService.hentFalskIdentitet(aktør)

            // Assert
            assertThat(falskIdentitet).isNotNull
            assertThat(falskIdentitet!!.navn).isEqualTo("Ukjent navn")
            assertThat(falskIdentitet.fødselsdato).isNull()
            assertThat(falskIdentitet.kjønn).isEqualTo(Kjønn.UKJENT)
        }

        @Test
        fun `skal returnere null dersom pdl ikke returnerer noen falsk identitet for aktør`() {
            // Arrange
            val aktør = lagAktør()

            every { pdlRestKlient.hentFalskIdentitet(aktør.aktivFødselsnummer()) } returns null

            // Act
            val falskIdentitet = falskIdentitetService.hentFalskIdentitet(aktør)

            // Assert
            assertThat(falskIdentitet).isNull()
        }

        @Test
        fun `skal returnere null dersom pdl returnerer falsk identitet for aktør som ikke er falsk`() {
            // Arrange
            val aktør = lagAktør()

            every { pdlRestKlient.hentFalskIdentitet(aktør.aktivFødselsnummer()) } returns PdlFalskIdentitet(erFalsk = false)

            // Act
            val falskIdentitet = falskIdentitetService.hentFalskIdentitet(aktør)

            // Assert
            assertThat(falskIdentitet).isNull()
        }
    }
}
