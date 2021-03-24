package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.steg.StegService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.net.InetAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

// TODO: Fjern etter migrering
@Component
class MigrerNyeBehandlinger(
        private val stegService: StegService,
        private val behandlingRepository: BehandlingRepository,
) {

    @Scheduled(initialDelay = 60000, fixedDelay = Long.MAX_VALUE)
    @Transactional
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
            logger.info("Migrerer nye behandlinger med manglende persongrunnlag ferdig")
            val behandlinger = behandlingRepository.finnBehandlingerForMigrering()

            var vellykkedeMigreringer = 0
            var mislykkedeMigreringer = 0
            behandlinger.forEach { behandling ->
                val søker = behandling.fagsak.søkerIdenter.singleOrNull() ?: error("Søker har flere identer")
                kotlin.runCatching {
                    stegService.håndterNyBehandlingMigrering(behandling = behandling, søkersIdent = søker.personIdent.ident)
                }.fold(
                        onSuccess = {
                            logger.info("Vellykket migrering for behandling ${behandling.id}")
                            vellykkedeMigreringer++
                        },
                        onFailure = {
                            logger.warn("Mislykket migrering for behandling ${behandling.id}")
                            secureLogger.warn("Mislykket migrering for behandling ${behandling.id}", it)
                            mislykkedeMigreringer++
                        }
                )
            }

            logger.info("Migrering av nye behandlinger med manglende persongrunnlag ferdig.\n" +
                        "Antall behandlinger=${behandlinger.size}\n" +
                        "Vellykede migreringer=$vellykkedeMigreringer\n" +
                        "Mislykkede migreringer=$mislykkedeMigreringer\n")
        }
    }

    companion object {

        val logger = LoggerFactory.getLogger(MigrerNyeBehandlinger::class.java)
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
