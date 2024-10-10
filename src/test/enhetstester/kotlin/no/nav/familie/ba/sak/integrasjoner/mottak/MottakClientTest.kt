package no.nav.familie.ba.sak.integrasjoner.mottak

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestOperations
import org.springframework.web.client.exchange
import java.net.URI

class MottakClientTest {
    private val mockedRestOperations: RestOperations = mockk()
    private val baseUri = URI("http://localhost:8080")
    private val mottakClient: MottakClient =
        MottakClient(
            baseUri,
            mockedRestOperations,
        )

    @Test
    fun `skal returnere den strengeste adressebeskyttelsegraderingen i digital søknad`() {
        // Arrange
        val journalpostId = "1"
        val adressebeskyttelsegradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT

        every {
            mockedRestOperations.exchange<ADRESSEBESKYTTELSEGRADERING?>(
                URI("$baseUri/soknad/adressebeskyttelse/${Tema.BAR.name}/$journalpostId"),
                HttpMethod.GET,
                any(),
            )
        } returns
            ResponseEntity<ADRESSEBESKYTTELSEGRADERING?>(
                adressebeskyttelsegradering,
                HttpStatus.OK,
            )

        // Act
        val strengesteAdressebeskyttelsegradering = mottakClient.hentStrengesteAdressebeskyttelsegraderingIDigitalSøknad(journalpostId = journalpostId)

        // Assert
        assertThat(strengesteAdressebeskyttelsegradering).isEqualTo(ADRESSEBESKYTTELSEGRADERING.UGRADERT)
    }

    @Test
    fun `skal returnere null dersom ingen av personene i digital søknad har adressebeskyttelsegradering`() {
        // Arrange
        val journalpostId = "1"

        every {
            mockedRestOperations.exchange<ADRESSEBESKYTTELSEGRADERING?>(
                URI("$baseUri/soknad/adressebeskyttelse/${Tema.BAR.name}/$journalpostId"),
                HttpMethod.GET,
                any(),
            )
        } returns
            ResponseEntity<ADRESSEBESKYTTELSEGRADERING?>(
                null,
                HttpStatus.OK,
            )

        // Act
        val strengesteAdressebeskyttelsegradering = mottakClient.hentStrengesteAdressebeskyttelsegraderingIDigitalSøknad(journalpostId = journalpostId)

        // Assert
        assertThat(strengesteAdressebeskyttelsegradering).isNull()
    }
}
