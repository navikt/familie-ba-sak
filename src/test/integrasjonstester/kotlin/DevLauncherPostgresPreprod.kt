import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.ApplicationConfig
import no.nav.familie.ba.sak.config.featureToggle.miljø.Profil
import org.springframework.boot.builder.SpringApplicationBuilder
import java.io.BufferedReader
import java.io.InputStreamReader

fun main(args: Array<String>) {
    System.setProperty("spring.profiles.active", Profil.DevPostgresPreprod.navn)

    val springBuilder =
        SpringApplicationBuilder(ApplicationConfig::class.java).profiles(
            "fake-økonomi-klient",
            "mock-infotrygd-feed",
            "fake-tilbakekreving-klient",
            "task-scheduling",
            "mock-infotrygd-barnetrygd",
            "mock-leader-client",
        )

    if (args.contains("--dbcontainer")) {
        System.setProperty("spring.datasource.url", "jdbc:tc:postgresql:17://localhost/familie-ba-sak")
    }

    if (!args.contains("--manuellMiljø") && System.getProperty("AZURE_APP_CLIENT_ID") == null) {
        settClientIdOgSecret()
    }

    springBuilder.run(*args)
}

private fun settClientIdOgSecret() {
    val cmd = "src/test/resources/hentMiljøvariabler.sh"

    val process = ProcessBuilder(cmd).start()

    if (process.waitFor() == 1) {
        val inputStream = BufferedReader(InputStreamReader(process.inputStream))
        inputStream.lines().forEach { println(it) }
        inputStream.close()
        throw Feil("Klarte ikke hente variabler fra Nais. Er du logget på Naisdevice og gcloud?")
    }

    val inputStream = BufferedReader(InputStreamReader(process.inputStream))
    inputStream.readLine() // "Switched to context dev-gcp"
    inputStream
        .readLine()
        .split(";")
        .map { it.split("=") }
        .map { System.setProperty(it[0], it[1]) }
    inputStream.close()
}
