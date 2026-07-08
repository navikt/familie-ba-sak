package no.nav.familie.ba.sak.config

import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpServletRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.http.HttpStatus
import java.io.EOFException
import java.io.IOException
import java.net.SocketException
import java.nio.channels.ClosedChannelException

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
}
