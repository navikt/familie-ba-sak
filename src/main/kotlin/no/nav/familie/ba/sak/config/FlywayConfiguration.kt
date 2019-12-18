package no.nav.familie.ba.sak.config

import org.flywaydb.core.api.configuration.FluentConfiguration
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty("spring.flyway.enabled")
class FlywayConfiguration {
    @Bean
    fun flywayConfig(@Value("\${spring.cloud.vault.database.role}") role: String?): FlywayConfigurationCustomizer {
        return FlywayConfigurationCustomizer { c: FluentConfiguration ->
            c.initSql(String.format("SET ROLE \"%s\"", role))
        }
    }
}