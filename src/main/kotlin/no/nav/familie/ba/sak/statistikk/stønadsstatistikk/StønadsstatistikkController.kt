package no.nav.familie.ba.sak.statistikk.stønadsstatistikk

import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.pdl.secureLogger
import no.nav.familie.ba.sak.integrasjoner.statistikk.StatistikkClient
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.task.PubliserVedtakTask
import no.nav.familie.eksterne.kontrakter.VedtakDVH
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/stonadsstatistikk")
@ProtectedWithClaims(issuer = "azuread")
class StønadsstatistikkController(
    private val stønadsstatistikkService: StønadsstatistikkService,
    private val taskRepository: TaskRepositoryWrapper,
    private val behandlingRepository: BehandlingRepository,
    private val statistikkClient: StatistikkClient
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
            if (!statistikkClient.harSendtVedtaksmeldingForBehandling(it)) {
                val vedtakDVH = stønadsstatistikkService.hentVedtak(it)
                val task = PubliserVedtakTask.opprettTask(vedtakDVH.person.personIdent, it)
                taskRepository.save(task)
            }
        }
    }

    @PostMapping(path = ["/ettersend-manuell-migrering/{dryRun}"])
    fun ettersendManuellMigrereringer(@PathVariable dryRun: Boolean = true) {
        val manuelleMigreringer = behandlingRepository.finnBehandlingIdMedOpprettetÅrsak(
            listOf(
                BehandlingÅrsak.ENDRE_MIGRERINGSDATO,
                BehandlingÅrsak.HELMANUELL_MIGRERING
            )
        )

        manuelleMigreringer.forEach {
            if (!statistikkClient.harSendtVedtaksmeldingForBehandling(it)) {
                logger.info("Ettersender stønadstatistikk for $it")
                val vedtakDVH = stønadsstatistikkService.hentVedtak(it)
                if (!dryRun) {
                    secureLogger.info("Oppretter task for å ettersende vedtak $vedtakDVH.person.personIdent")
                    val task = PubliserVedtakTask.opprettTask(vedtakDVH.person.personIdent, it)
                    taskRepository.save(task)
                }
            }
        }
    }
}
