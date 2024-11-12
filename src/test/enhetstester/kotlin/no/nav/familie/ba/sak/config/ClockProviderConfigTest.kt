package no.nav.familie.ba.sak.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock

class ClockProviderConfigTest {
    @Test
    fun `skal returnere system default zone clock fra clock provider`() {
        // Arrange
        val clockProvider = ClockProviderConfig().clockProvider()

        // Act
        val clock = clockProvider.get()

        // Assert
        assertThat(clock).isEqualTo(Clock.systemDefaultZone())
    }
}
