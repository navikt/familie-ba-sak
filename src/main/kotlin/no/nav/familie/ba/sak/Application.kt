package no.nav.familie.ba.sak

import no.nav.familie.ba.sak.config.ApplicationConfig
import no.nav.familie.ba.sak.config.DelayedShutdownHook
import no.nav.familie.prosessering.internal.TaskScheduler
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.scheduling.annotation.Scheduled

@SpringBootApplication
class FamilieBaSakApplication(val taskScheduler: TaskScheduler){

    @Scheduled(fixedDelayString = "3000000")
    fun testTaskMaintenance(){
        taskScheduler.settPermanentPlukketTilKlarTilPlukk()
    }
}

fun main(args: Array<String>) {
    val app = SpringApplication(ApplicationConfig::class.java)
    app.setRegisterShutdownHook(false)
    val applicationContext: ConfigurableApplicationContext = app.run(*args)
    Runtime.getRuntime().addShutdownHook(DelayedShutdownHook(applicationContext))
}