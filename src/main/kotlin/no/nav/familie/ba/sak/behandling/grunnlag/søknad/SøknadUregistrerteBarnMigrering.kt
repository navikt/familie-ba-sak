package no.nav.familie.ba.sak.behandling.grunnlag.søknad

import no.nav.familie.ba.sak.behandling.restDomene.writeValueAsString
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.net.InetAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

// TODO: Fjern etter migrering
@Component
class SøknadUregistrerteBarnMigrering(
        private val søknadGrunnlagRepository: SøknadGrunnlagRepository,
) {

    @Scheduled(initialDelay = 240000, fixedDelay = Long.MAX_VALUE)
    fun migrer() {

        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:4040"))
                .GET()
                .build()

        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        val hostname: String = InetAddress.getLocalHost().hostName
        val erLeader = response.body().contains(hostname)

        logger.info("Respons=${response.body()}, hostname=$hostname")
        logger.info("Er leader : $erLeader")

        if (erLeader) {
            logger.info("Migrering av søknader til å støtte barn som ikke finnes i folkeregisteret starter")
            val søknader = søknadGrunnlagRepository.findAll()

            var vellykkedeMigreringer = 0
            var mislykkedeMigreringer = 0

            søknader.forEach { søknad ->

                Result.runCatching {
                    val søknadDTO = søknad.hentSøknadDto()

                    if (søknadDTO.barnaMedOpplysninger.isNotEmpty() && søknadDTO.barnaMedOpplysninger.any { it.erFolkeregistrert == null }) {
                        søknadGrunnlagRepository.save(søknad.copy(
                                søknad = søknadDTO.copy(
                                        barnaMedOpplysninger = søknadDTO.barnaMedOpplysninger.map { it.copy(erFolkeregistrert = true) }
                                ).writeValueAsString()
                        ))
                    }
                }.fold(
                        onSuccess = {
                            logger.info("Vellykket migrering for søknad ${søknad.id} på behandling ${søknad.behandlingId}")
                            vellykkedeMigreringer++
                        },
                        onFailure = { throwable ->
                            logger.info("Mislykket migrering for søknad ${søknad.id} på behandling ${søknad.behandlingId}")
                            secureLogger.info("Mislykket migrering for søknad ${søknad.id} på behandling ${søknad.behandlingId}",
                                              throwable)
                            mislykkedeMigreringer++
                        }
                )
            }

            logger.info("Migrering av søknader til å støtte barn som ikke finnes i folkeregisteret er ferdig.\n" +
                        "Antall søknader=${søknader.size}\n" +
                        "Vellykede migreringer=$vellykkedeMigreringer\n" +
                        "Mislykkede migreringer=$mislykkedeMigreringer\n")
        }
    }

    companion object {

        val logger = LoggerFactory.getLogger(SøknadUregistrerteBarnMigrering::class.java)
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
