import no.nav.familie.ba.sak.config.ApplicationConfig
import org.springframework.boot.builder.SpringApplicationBuilder

class DevLauncherPostgres

fun main(args: Array<String>) {
    SpringApplicationBuilder(ApplicationConfig::class.java).profiles(
            "postgres",
            "mock-brev-klient",
            "mock-økonomi",
            "mock-infotrygd-feed",
            "mock-infotrygd-barnetrygd",
            "mock-pdl",
            "mock-tilbakekreving-klient"
    ).run(*args)
}
