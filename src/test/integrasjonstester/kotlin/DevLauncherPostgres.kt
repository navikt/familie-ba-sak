import no.nav.familie.ba.sak.config.ApplicationConfig
import org.springframework.boot.builder.SpringApplicationBuilder

fun main(args: Array<String>) {
    System.setProperty("spring.profiles.active", "postgres")

    val springBuilder =
        SpringApplicationBuilder(ApplicationConfig::class.java).profiles(
            "dev",
            "postgres",
            "mock-brev-klient",
            "mock-økonomi",
            "mock-infotrygd-feed",
            "mock-infotrygd-barnetrygd",
            "mock-pdl",
            "mock-ident-client",
            "mock-tilbakekreving-klient",
            "task-scheduling",
        )

    if (args.contains("--dbcontainer")) {
        System.setProperty("spring.datasource.url", "jdbc:tc:postgresql:15://localhost/familie-ba-sak")
    }

    springBuilder.run(*args)
}
