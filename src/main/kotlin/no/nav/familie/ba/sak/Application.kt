package no.nav.familie.ba.sak

import no.nav.familie.ba.sak.config.DelayedShutdownHook
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ConfigurableApplicationContext

@SpringBootApplication
class FamilieBaSakApplication

fun main(args: Array<String>) {
    val app = SpringApplication(FamilieBaSakApplication::class.java)
    app.setRegisterShutdownHook(false)
    val applicationContext: ConfigurableApplicationContext = app.run(*args)
    Runtime.getRuntime().addShutdownHook(DelayedShutdownHook(applicationContext))
}
