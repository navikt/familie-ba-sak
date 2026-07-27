package no.nav.familie.ba.sak.integrasjoner

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.core.retry.RetryException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException

class RetryTemplateTest {
    @Test
    fun `skal ikke retrye ved 403 Forbidden`() {
        // Arrange
        val forbidden =
            HttpClientErrorException.create(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                HttpHeaders(),
                ByteArray(0),
                null,
            )
        val kall = mockk<() -> String>()
        every { kall.invoke() } throws forbidden

        // Act & Assert
        assertThatThrownBy {
            retryVedException(delayInMs = 1).execute { kall() }
        }.isInstanceOf(RetryException::class.java)
            .hasCauseInstanceOf(HttpClientErrorException.Forbidden::class.java)

        // Ingen retry ved 403 - kun det opprinnelige forsoeket
        verify(exactly = 1) { kall.invoke() }
    }

    @Test
    fun `skal retrye ved andre exceptions og til slutt kaste feilen`() {
        // Arrange
        val kall = mockk<() -> String>()
        every { kall.invoke() } throws RuntimeException("feil")

        // Act & Assert
        assertThatThrownBy {
            retryVedException(delayInMs = 1).execute { kall() }
        }.isInstanceOf(RetryException::class.java)
            .hasCauseInstanceOf(RuntimeException::class.java)

        // 1 opprinnelig forsoek + 3 retries
        verify(exactly = 4) { kall.invoke() }
    }

    @Test
    fun `skal returnere resultat uten retry naar kallet lykkes`() {
        // Arrange
        val kall = mockk<() -> String>()
        every { kall.invoke() } returns "ok"

        // Act
        val resultat = retryVedException(delayInMs = 1).execute { kall() }

        // Assert
        assertThat(resultat).isEqualTo("ok")
        verify(exactly = 1) { kall.invoke() }
    }
}
