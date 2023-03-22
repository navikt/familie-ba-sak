package no.nav.familie.ba.sak.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Bean
    fun openApi(): OpenAPI {
        return OpenAPI()
            .components(
                Components()
                    .addSecuritySchemes("bearer", bearerTokenSecurityScheme())
            )
            .addSecurityItem(SecurityRequirement().addList("bearer", listOf("read", "write")))
    }

    @Bean
    fun eksternOpenApi(): GroupedOpenApi {
        return GroupedOpenApi.builder().group("ekstern").packagesToScan("no.nav.familie.ba.sak.ekstern.bisys")
            .build()
    }

    @Bean
    fun internOpenApi(): GroupedOpenApi {
        return GroupedOpenApi.builder().group("intern").packagesToScan("no.nav.familie.ba.sak")
            .build()
    }

    private fun bearerTokenSecurityScheme(): SecurityScheme {
        return SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .scheme("bearer")
            .bearerFormat("JWT")
            .`in`(SecurityScheme.In.HEADER)
            .name("Authorization")
    }
}
