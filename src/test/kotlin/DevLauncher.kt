import no.nav.familie.ba.sak.config.ApplicationConfig
import org.springframework.boot.builder.SpringApplicationBuilder

object DevLauncher {

    @JvmStatic
    fun main(args: Array<String>) {
        val app = SpringApplicationBuilder(ApplicationConfig::class.java)
                .profiles(
                        "dev",
                        "mock-brev-klient",
                        "mock-økonomi",
                        "mock-infotrygd-feed",
                        "mock-infotrygd-barnetrygd",
                        "mock-sts",
                        "mock-pdl",
                        "mock-simulering",
                        "mock-tilbake-klient",
                )
        app.run(*args)
    }
}