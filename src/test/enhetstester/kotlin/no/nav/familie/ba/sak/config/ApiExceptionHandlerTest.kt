package no.nav.familie.ba.sak.config

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpServletRequest
import no.nav.familie.ba.sak.common.RessursUtils
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonException
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpServerErrorException
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

    @Nested
    inner class HandleIntegrasjonException {
        private val loggere =
            listOf(ApiExceptionHandler::class.java, RessursUtils::class.java)
                .map { LoggerFactory.getLogger(it) as Logger }
        private val listAppender = ListAppender<ILoggingEvent>()

        @BeforeEach
        fun leggTilAppender() {
            listAppender.list.clear()
            listAppender.start()
            loggere.forEach { it.addAppender(listAppender) }
        }

        @AfterEach
        fun fjernAppender() {
            loggere.forEach { it.detachAppender(listAppender) }
        }

        @Test
        fun `skal logge warn og ikke error når ekstern tjeneste svarer med 5xx`() {
            // Arrange
            val feil =
                IntegrasjonException(
                    msg = "Kall mot ef-sak feilet",
                    throwable = HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR),
                )

            // Act
            val respons = handler.handleIntegrasjonException(feil)

            // Assert
            assertThat(respons.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
            assertThat(respons.body?.status).isEqualTo(Ressurs.Status.FEILET)
            assertThat(respons.body?.melding).isEqualTo("Kall mot ef-sak feilet")
            assertThat(listAppender.list).noneMatch { it.level == Level.ERROR }
            assertThat(listAppender.list).anySatisfy {
                assertThat(it.level).isEqualTo(Level.WARN)
                assertThat(it.formattedMessage).isEqualTo("Ekstern tjeneste svarte med 500. Se securelogs for detaljer.")
            }
        }

        @Test
        fun `skal logge error når integrasjonsfeilen ikke skyldes 5xx fra ekstern tjeneste`() {
            // Arrange
            val feil =
                IntegrasjonException(
                    msg = "Kall mot ef-sak feilet",
                    throwable = IllegalStateException("Kunne ikke deserialisere respons"),
                )

            // Act
            val respons = handler.handleIntegrasjonException(feil)

            // Assert
            assertThat(respons.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
            assertThat(respons.body?.status).isEqualTo(Ressurs.Status.FEILET)
            assertThat(respons.body?.melding).isEqualTo("Kall mot ef-sak feilet")
            assertThat(listAppender.list).anyMatch { it.level == Level.ERROR }
        }
    }
}
