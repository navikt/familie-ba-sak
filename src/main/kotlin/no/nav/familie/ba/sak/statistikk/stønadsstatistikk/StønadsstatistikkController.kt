package no.nav.familie.ba.sak.statistikk.stønadsstatistikk

import no.nav.familie.ba.sak.task.PubliserVedtakTask
import no.nav.familie.eksterne.kontrakter.VedtakDVH
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/stonadsstatistikk")
@ProtectedWithClaims(issuer = "azuread")
class StønadsstatistikkController(
    private val stønadsstatistikkService: StønadsstatistikkService,
    private val taskRepository: TaskRepository
) {

    private val logger = LoggerFactory.getLogger(StønadsstatistikkController::class.java)

    @PostMapping(path = ["/vedtak"])
    fun hentBehandlingDvh(@RequestBody(required = true) behandlinger: List<Long>): List<VedtakDVH> {
        try {
            return behandlinger.map { stønadsstatistikkService.hentVedtak(it) }
        } catch (e: Exception) {
            logger.warn("Feil ved henting av stønadsstatistikk for $behandlinger", e)
            throw e
        }
    }

    @PostMapping(path = ["/send-til-dvh"])
    fun sendTilStønadsstatistikk(@RequestBody(required = true) behandlinger: List<Long>) {
        behandlinger.forEach {
            val vedtakDVH = stønadsstatistikkService.hentVedtak(it)
            val task = PubliserVedtakTask.opprettTask(vedtakDVH.person.personIdent, it)
            taskRepository.save(task)
        }
    }
}