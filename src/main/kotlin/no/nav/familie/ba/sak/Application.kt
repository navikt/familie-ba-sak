package no.nav.familie.ba.sak

import no.nav.familie.ba.sak.config.ApplicationConfig
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class FamilieBaSakApplication

fun main(args: Array<String>) {
    SpringApplication(ApplicationConfig::class.java)
}
