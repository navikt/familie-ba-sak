package no.nav.familie.ba.sak.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock

class ClockProviderConfigTest {
    @Test
    fun `skal returnere system default zone clock`() {
        // Arrange
        val clockProviderConfig = ClockProviderConfig()

        // Act
        val clockProvider = clockProviderConfig.clockProvider()

        // Assert
        assertThat(clockProvider.get()).isEqualTo(Clock.systemDefaultZone())
    }
}
