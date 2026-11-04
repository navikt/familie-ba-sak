package no.nav.familie.ba.sak.config

import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpServletRequest
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpServerErrorException
import java.io.EOFException
import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.http.HttpTimeoutException
import java.nio.channels.ClosedChannelException
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeoutException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiExceptionHandlerTest {
    private val handler = ApiExceptionHandler()
    private val request = mockk<HttpServletRequest>()

    @BeforeEach
    fun setUp() {
        every { request.requestURI } returns "/api/behandling/123"
        every { request.method } returns "GET"
    }

    @Test
    fun `handleNettverksfeil returnerer 503 ved IOException broken pipe`() {
        // Arrange
        val feil = IOException("Broken pipe")

        // Act
        val respons = handler.handleNettverksfeil(feil, request)

        // Assert
        assertThat(respons.statusCode).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
        assertThat(respons.body?.frontendFeilmelding).isEqualTo("Tilkoblingen ble brutt")
    }

    @Test
    fun `handleNettverksfeil returnerer 503 ved ClosedChannelException`() {
        // Arrange
        val feil = ClosedChannelException()

        // Act
        val respons = handler.handleNettverksfeil(feil, request)

        // Assert
        assertThat(respons.statusCode).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
    }

    @Test
    fun `handleNettverksfeil returnerer 503 ved EOFException`() {
        // Arrange
        val feil = EOFException("Unexpected end of stream")

        // Act
        val respons = handler.handleNettverksfeil(feil, request)

        // Assert
        assertThat(respons.statusCode).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
    }

    @Test
    fun `NettverksfeilType klassifiserer broken pipe korrekt`() {
        assertThat(NettverksfeilType.fraException(IOException("Broken pipe"))).isEqualTo(NettverksfeilType.BROKEN_PIPE)
        assertThat(NettverksfeilType.fraException(IOException("broken pipe"))).isEqualTo(NettverksfeilType.BROKEN_PIPE)
    }

    @Test
    fun `NettverksfeilType klassifiserer ClosedChannelException korrekt`() {
        assertThat(NettverksfeilType.fraException(ClosedChannelException())).isEqualTo(NettverksfeilType.CLOSED_CHANNEL)
    }

    @Test
    fun `NettverksfeilType klassifiserer EOFException korrekt`() {
        assertThat(NettverksfeilType.fraException(EOFException())).isEqualTo(NettverksfeilType.EOF)
    }

    @Test
    fun `NettverksfeilType klassifiserer ukjent IOException som ukjent`() {
        assertThat(NettverksfeilType.fraException(IOException("Connection reset"))).isEqualTo(NettverksfeilType.UKJENT)
    }

    @Test
    fun `NettverksfeilType klassifiserer SocketException connection reset korrekt`() {
        assertThat(NettverksfeilType.fraException(SocketException("Connection reset"))).isEqualTo(NettverksfeilType.CONNECTION_RESET)
        assertThat(NettverksfeilType.fraException(SocketException("connection reset by peer"))).isEqualTo(NettverksfeilType.CONNECTION_RESET)
    }

    @Test
    fun `NettverksfeilType klassifiserer ukjent SocketException som ukjent`() {
        assertThat(NettverksfeilType.fraException(SocketException("Network unreachable"))).isEqualTo(NettverksfeilType.UKJENT)
    }

    @Test
    fun `skal returnere 503 for integrasjonsfeil med nettverksårsaker`() {
        // Arrange
        val feil =
            listOf(
                IOException("Broken pipe"),
                SocketException("Connection reset"),
                EOFException("Unexpected end of stream"),
                ClosedChannelException(),
            ).map { IntegrasjonException("Integrasjonsfeil", it) }

        // Act
        val responser = feil.map { handler.handleIntegrasjonException(it, request) }

        // Assert
        assertThat(responser).allSatisfy { respons ->
            assertThat(respons.statusCode).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
        }
    }

    @Test
    fun `skal klassifisere SocketTimeoutException HttpTimeoutException og TimeoutException som timeout`() {
        // Arrange
        val årsaker =
            listOf(
                SocketTimeoutException("socket timed out"),
                HttpTimeoutException("HTTP request timed out"),
                TimeoutException("request timed out"),
            )

        // Act
        val typer = årsaker.map(NettverksfeilType::fraException)
        val responser = årsaker.map { handler.handleIntegrasjonException(IntegrasjonException("Integrasjonsfeil", it), request) }

        // Assert
        assertThat(typer).containsOnly(NettverksfeilType.TIMEOUT)
        assertThat(responser).allSatisfy { respons ->
            assertThat(respons.statusCode).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
        }
    }

    @Test
    fun `skal finne nettverksfeil gjennom flere wrappernivåer og hele årsakskjeden`() {
        // Arrange
        val årsak = IllegalStateException("innerst", IOException("Broken pipe"))
        val feil = IntegrasjonException("Integrasjonsfeil", RuntimeException("wrapper", årsak))

        // Act
        val respons = handler.handleIntegrasjonException(feil, request)

        // Assert
        assertThat(respons.statusCode).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
    }

    @Test
    fun `skal returnere 500 når IOException har IllegalStateException som innerste årsak`() {
        // Arrange
        val nettverksfeil = IOException("Broken pipe", IllegalStateException("innerste årsak"))
        val feil = IntegrasjonException("Integrasjonsfeil", nettverksfeil)

        // Act
        val respons = handler.handleIntegrasjonException(feil, request)

        // Assert
        assertThat(respons.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @Test
    fun `skal returnere 500 for integrasjonsfeil med ukjent IOException som årsak`() {
        // Arrange
        val feil = IntegrasjonException("Integrasjonsfeil", IOException("Ukjent I/O-feil"))

        // Act
        val respons = handler.handleIntegrasjonException(feil, request)

        // Assert
        assertThat(respons.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @Test
    fun `skal returnere 500 for integrasjonsfeil uten nettverksårsak`() {
        // Arrange
        val feil = IntegrasjonException("Integrasjonsfeil", IllegalStateException("annen feil"))

        // Act
        val respons = handler.handleIntegrasjonException(feil, request)

        // Assert
        assertThat(respons.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @Test
    fun `skal returnere 503 når HttpServerErrorException har IOException som årsak`() {
        // Arrange
        val httpFeil =
            HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR).apply {
                initCause(IOException("Broken pipe"))
            }
        val feil = IntegrasjonException("Integrasjonsfeil", httpFeil)

        // Act
        val respons = handler.handleIntegrasjonException(feil, request)

        // Assert
        assertThat(respons.statusCode).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
        assertThat(respons.body?.frontendFeilmelding).isEqualTo("Tilkoblingen ble brutt")
    }

    @Test
    fun `skal ikke tolke timeouttekst i HTTP 500 response body som nettverksfeil`() {
        // Arrange
        val httpFeil =
            HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                HttpHeaders.EMPTY,
                "request timed out".toByteArray(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8,
            )
        val feil = IntegrasjonException("Integrasjonsfeil", httpFeil)

        // Act
        val respons = handler.handleIntegrasjonException(feil, request)

        // Assert
        assertThat(respons.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
