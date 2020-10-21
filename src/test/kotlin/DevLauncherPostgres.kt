import no.nav.familie.ba.sak.config.ApplicationConfig
import org.springframework.boot.builder.SpringApplicationBuilder

class DevLauncherPostgres

fun main(args: Array<String>) {
    SpringApplicationBuilder(ApplicationConfig::class.java).profiles("postgres",
                                                                     "mock-dokgen-java",
                                                                     "mock-økonomi",
                                                                     "mock-infotrygd-feed",
                                                                     "mock-infotrygd-barnetrygd",
                                                                     "mock-sts",
                                                                     "mock-pdl",
    ).run(*args)
}
