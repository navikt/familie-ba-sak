package no.nav.familie.ba.sak.config

import no.nav.familie.felles.utbetalingsgenerator.Utbetalingsgenerator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class UtbetalingsgeneratorConfig {
    @Bean
    fun utbetalingsgenerator(): Utbetalingsgenerator = Utbetalingsgenerator()
}
