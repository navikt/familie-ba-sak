import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.config.ApplicationConfig
import org.springframework.boot.builder.SpringApplicationBuilder
import java.io.BufferedReader
import java.io.InputStreamReader

fun main(args: Array<String>) {
    System.setProperty("spring.profiles.active", "dev-postgres-preprod")
    val springBuilder = SpringApplicationBuilder(ApplicationConfig::class.java).profiles(
        "mock-brev-klient",
        "mock-økonomi",
        "mock-infotrygd-feed",
        "mock-tilbakekreving-klient",
        "task-scheduling",
        "mock-infotrygd-barnetrygd"
    )

    if (args.contains("--dbcontainer")) {
        springBuilder.initializers(DbContainerInitializer())
    }

    if (!args.contains("--manuellMiljø")) {
        settClientIdOgSecret()
    }

    springBuilder.run(* args)
}

private fun settClientIdOgSecret() {
    val cmd = "src/test/resources/hentMiljøvariabler.sh"

    val process = ProcessBuilder(cmd).start()

    if (process.waitFor() == 1) {
        error("Klarte ikke hente variabler fra Nais. Er du logget på Naisdevice og gcloud?")
    }

    val inputStream = BufferedReader(InputStreamReader(process.inputStream))
    inputStream.readLine() // "Switched to context dev-gcp"
    val clientIdOgSecret = inputStream.readLine().split(";")
        .map { it.split("=") }
        .map { it[1] }
    inputStream.close()

    System.setProperty("BA_SAK_CLIENT_ID", clientIdOgSecret[0])
    System.setProperty("CLIENT_SECRET", clientIdOgSecret[1])
}
