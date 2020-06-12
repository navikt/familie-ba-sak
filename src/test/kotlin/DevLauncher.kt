import no.nav.familie.ba.sak.config.ApplicationConfig
import org.springframework.boot.builder.SpringApplicationBuilder

object DevLauncher {
    @JvmStatic
    fun main(args: Array<String>) {
        val app = SpringApplicationBuilder(ApplicationConfig::class.java)
                .profiles("postgres", "mock-totrinnkontroll", "mock-dokgen-java", "mock-iverksett", "mock-infotrygd-feed")
        app.run(*args)
    }
}