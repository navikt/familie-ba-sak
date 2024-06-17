package no.nav.familie.ba.sak.statistikk.stønadsstatistikk

import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.task.OpprettTaskerForVedtakEtterVedtaksdatoTask
import no.nav.familie.ba.sak.task.PubliserVedtakV2Task
import no.nav.familie.eksterne.kontrakter.VedtakDVHV2
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/stonadsstatistikk")
@ProtectedWithClaims(issuer = "azuread")
class StønadsstatistikkController(
    private val stønadsstatistikkService: StønadsstatistikkService,
    private val taskRepository: TaskRepositoryWrapper,
    private val vedtakRepository: VedtakRepository,
) {
    private val logger = LoggerFactory.getLogger(StønadsstatistikkController::class.java)

    @PostMapping(path = ["/vedtakV2"])
    fun hentVedtakDvhV2(
        @RequestBody(required = true) behandlinger: List<Long>,
    ): List<VedtakDVHV2> {
        try {
            return behandlinger.map { stønadsstatistikkService.hentVedtakV2(it) }
        } catch (e: Exception) {
            logger.warn("Feil ved henting av stønadsstatistikk V2 for $behandlinger", e)
            throw e
        }
    }

    @PostMapping(path = ["/send-til-dvh-manuell"])
    fun sendTilStønadsstatistikkManuell(
        @RequestBody(required = true) behandlinger: List<Long>,
    ) {
        behandlinger.forEach {
            val vedtakV2DVH = stønadsstatistikkService.hentVedtakV2(it)
            val vedtakV2Task = PubliserVedtakV2Task.opprettTask(vedtakV2DVH.personV2.personIdent, it)
            taskRepository.save(vedtakV2Task)
        }
    }

    @PostMapping(path = ["/send-til-dvh-vedtak-etter-dato"])
    @Transactional
    fun sendTilStønadsstatistikkAlleVedtakEtterDato(
        @RequestBody(required = true) dato: LocalDateTime,
    ) {
        logger.info("Starter opprettelse av FinnBehandlingerMedVedtakEtterDatoTask")
        OpprettTaskerForVedtakEtterVedtaksdatoTask.opprettTask(dato)
        logger.info("Fullført opprettelse av FinnBehandlingerMedVedtakEtterDatoTask")
    }
}
