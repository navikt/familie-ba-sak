package no.nav.familie.ba.sak.statistikk.stønadsstatistikk

import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.pdl.secureLogger
import no.nav.familie.ba.sak.integrasjoner.statistikk.StatistikkClient
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingId
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.task.PubliserVedtakV2Task
import no.nav.familie.eksterne.kontrakter.VedtakDVHV2
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
    private val statistikkClient: StatistikkClient,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository
) {

    private val logger = LoggerFactory.getLogger(StønadsstatistikkController::class.java)

    @PostMapping(path = ["/vedtakV2"])
    fun hentVedtakDvhV2(@RequestBody(required = true) behandlinger: List<Long>): List<VedtakDVHV2> {
        try {
            return behandlinger.map { stønadsstatistikkService.hentVedtakV2(BehandlingId(it)) }
        } catch (e: Exception) {
            logger.warn("Feil ved henting av stønadsstatistikk V2 for $behandlinger", e)
            throw e
        }
    }

    @PostMapping(path = ["/send-til-dvh"])
    fun sendTilStønadsstatistikk(@RequestBody(required = true) behandlinger: List<Long>) {
        behandlinger.forEach {
            val behandlingId = BehandlingId(it)
            if (!statistikkClient.harSendtVedtaksmeldingForBehandling(behandlingId)) {
                val vedtakV2DVH = stønadsstatistikkService.hentVedtakV2(behandlingId)
                val vedtakV2Task = PubliserVedtakV2Task.opprettTask(vedtakV2DVH.personV2.personIdent, behandlingId)
                taskRepository.save(vedtakV2Task)
            }
        }
    }

    @PostMapping(path = ["/send-til-dvh-manuell"])
    fun sendTilStønadsstatistikkManuell(@RequestBody(required = true) behandlinger: List<Long>) {
        behandlinger.forEach {
            val behandlingId = BehandlingId(it)
            val vedtakV2DVH = stønadsstatistikkService.hentVedtakV2(behandlingId)
            val vedtakV2Task = PubliserVedtakV2Task.opprettTask(vedtakV2DVH.personV2.personIdent, behandlingId)
            taskRepository.save(vedtakV2Task)
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
            val behandlingId = BehandlingId(it)
            if (!statistikkClient.harSendtVedtaksmeldingForBehandling(behandlingId) && erIverksattBehandling(
                    behandlingId
                )
            ) {
                logger.info("Ettersender stønadstatistikk for behandlingId=${behandlingId.id} dryRun=$dryRun")
                val vedtakV2DVH = stønadsstatistikkService.hentVedtakV2(behandlingId)
                if (!dryRun) {
                    secureLogger.info("Oppretter task for å ettersende vedtak $vedtakV2DVH.person.personIdent")
                    val vedtakV2Task = PubliserVedtakV2Task.opprettTask(vedtakV2DVH.personV2.personIdent, behandlingId)
                    taskRepository.save(vedtakV2Task)
                }
            }
        }
    }

    private fun erIverksattBehandling(behandlingId: BehandlingId): Boolean {
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandlingOptional(behandlingId.id)

        return if (tilkjentYtelse != null) {
            tilkjentYtelse.utbetalingsoppdrag != null
        } else {
            false
        }
    }
}
