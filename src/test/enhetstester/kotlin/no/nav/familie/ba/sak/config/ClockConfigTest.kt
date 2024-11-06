package no.nav.familie.ba.sak.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock

class ClockConfigTest {
    @Test
    fun `skal returnere system default zone clock`() {
        // Arrange
        val clockConfig = ClockConfig()

        // Act
        val clock = clockConfig.clock()

        // Assert
        assertThat(clock).isEqualTo(Clock.systemDefaultZone())
    }
}
