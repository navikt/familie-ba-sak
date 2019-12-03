package no.nav.familie.ba.sak.config

import org.springframework.context.ConfigurableApplicationContext

class DelayedShutdownHook(private val applicationContext: ConfigurableApplicationContext) : Thread() {
    override fun run() {
        try {
            // https://github.com/kubernetes/kubernetes/issues/64510
            // https://nav-it.slack.com/archives/C5KUST8N6/p1543497847341300
            sleep(5000L)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        applicationContext.close()
        super.run()
    }

}