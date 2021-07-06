package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
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
class VedtaksperioderMedBegrunnelserMigrering(
        private val behandlingRepository: BehandlingRepository,
        private val envService: EnvService,
        private val vedtakService: VedtakService,
        private val featureToggleService: FeatureToggleService,
) {

    @Transactional
    @Scheduled(initialDelay = 120000, fixedDelay = Long.MAX_VALUE)
    fun migrer() {
        val erLeader = if (envService.erDev()) true else {
            val client = HttpClient.newHttpClient()
            val request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:4040"))
                    .GET()
                    .build()

            val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
            val hostname: String = InetAddress.getLocalHost().hostName
            logger.info("Respons=${response.body()}, hostname=$hostname")

            response.body().contains(hostname)
        }

        logger.info("Er leader: $erLeader")

        if (erLeader && featureToggleService.isEnabled(FeatureToggleConfig.MIGRER_VEDTAK_BEGRUNNELSES_MODEL_UTREDNING)) {
            logger.info("Migrerer behandlinger for ny begrunnelsesmodell - finn alle behandlinger under utredning.")
            val behandlinger = behandlingRepository.finnBehandlingerForMigreringAvVedtaksbegrunnelser()

            var vellykkedeMigreringer = 0
            var mislykkedeMigreringer = 0
            behandlinger.forEach { behandling ->
                Result.runCatching {
                            logger.info("Sett behandling ${behandling.id} til steg vilkårsvurdering.")
                            vedtakService.settStegSlettVedtakBegrunnelserOgTilbakekreving(behandlingId = behandling.id)
                }.onSuccess {
                    logger.info("Vellykket migrering for behandling ${behandling.id}")
                    vellykkedeMigreringer++
                }.onFailure {
                    logger.info("Migrering feilet for behandling ${behandling.id}, fagsak ${behandling.fagsak.id}")
                    secureLogger.info("Migrering feilet for behandling ${behandling.id}, fagsak ${behandling.fagsak.id}", it)
                    mislykkedeMigreringer++
                }
            }

            logger.info("Migrering av vedtaksbegrunnelser ferdig.\n" +
                        "Antall behandlinger=${behandlinger.size}\n" +
                        "Vellykede migreringer=$vellykkedeMigreringer\n" +
                        "Mislykkede migreringer=$mislykkedeMigreringer\n")
        }
    }

    companion object {

        val logger = LoggerFactory.getLogger(VedtaksperioderMedBegrunnelserMigrering::class.java)
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
