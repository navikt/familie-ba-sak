package no.nav.familie.ba.sak.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiKey
import springfox.documentation.service.AuthorizationScope
import springfox.documentation.service.SecurityReference
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spi.service.contexts.SecurityContext
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2
import java.time.LocalDateTime
import java.time.YearMonth

@Configuration
@EnableSwagger2
class SwaggerConfig {

    private val bearer = "Bearer"

    @Bean
    fun bisysApi(): Docket {
        return Docket(DocumentationType.SWAGGER_2)
            .directModelSubstitute(YearMonth::class.java, String::class.java)
            .directModelSubstitute(LocalDateTime::class.java, String::class.java)
            .groupName("BISYS")
            .select()
            .apis(RequestHandlerSelectors.basePackage("no.nav.familie.ba.sak.bisys"))
            .paths(PathSelectors.any())
            .build()
            .pathMapping("/")
            .securitySchemes(securitySchemes())
            .securityContexts(securityContext())
            .apiInfo(
                ApiInfoBuilder().description("Tjenester for BISYS. For å ta i bruk tjenesten så trenger man å få lagt inn client-id inn i liste som godkjente klienter. Ta kontakt med barnetrygdteamet i slackkanalen #team-familie")
                    .build()
            )
    }

    private fun securitySchemes(): List<ApiKey> {
        return listOf(ApiKey(bearer, "Authorization", "header"))
    }

    private fun securityContext(): List<SecurityContext> {
        return listOf(
            SecurityContext.builder()
                .securityReferences(defaultAuth())
                .forPaths(PathSelectors.regex("/api.*"))
                .build()
        )
    }

    private fun defaultAuth(): List<SecurityReference> {
        val authorizationScope = AuthorizationScope("global", "accessEverything")
        val authorizationScopes = arrayOfNulls<AuthorizationScope>(1)
        authorizationScopes[0] = authorizationScope
        return listOf(SecurityReference(bearer, authorizationScopes))
    }


}
