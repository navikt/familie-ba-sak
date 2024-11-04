package no.nav.familie.ba.sak.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class UtbetalingsgeneratorConfigTest : AbstractSpringIntegrationTest() {
    @Autowired
    private lateinit var utbetalingsgeneratorConfig: UtbetalingsgeneratorConfig

    @Test
    fun `skal instansiere utbetalingsgenerator`() {
        // Act
        val utbetalingsgenerator = utbetalingsgeneratorConfig.utbetalingsgenerator()

        // Assert
        assertThat(utbetalingsgenerator).isNotNull()
    }
}
