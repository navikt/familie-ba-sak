package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.domene.vedtak.NyttVedtak
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.task.AvstemMotOppdrag
import no.nav.familie.ba.sak.task.IverksettMotOppdrag
import no.nav.familie.ba.sak.task.OpphørBehandlingOgVedtak.Companion.opprettTaskOpphørBehandlingOgVedtak
import no.nav.familie.ba.sak.økonomi.AvstemmingTaskDTO
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.sikkerhet.OIDCUtil
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api")
@ProtectedWithClaims(issuer = "azuread")
class FagsakController(
        private val oidcUtil: OIDCUtil,
        private val fagsakService: FagsakService,
        private val behandlingService: BehandlingService,
        private val taskRepository: TaskRepository
) {

    @GetMapping(path = ["/fagsak/{fagsakId}"])
    fun hentFagsak(@PathVariable fagsakId: Long): ResponseEntity<Ressurs<RestFagsak>> {
        val saksbehandlerId = oidcUtil.getClaim("preferred_username")

        logger.info("{} henter fagsak med id {}", saksbehandlerId ?: "Ukjent", fagsakId)

        val ressurs = Result.runCatching { fagsakService.hentRestFagsak(fagsakId) }
                .fold(
                        onSuccess = { it },
                        onFailure = { e -> Ressurs.failure("Henting av fagsak med fagsakId $fagsakId feilet", e) }
                )

        return ResponseEntity.ok(ressurs)
    }

    @PostMapping(path = ["/fagsak/{fagsakId}/nytt-vedtak"])
    fun nyttVedtak(@PathVariable fagsakId: Long, @RequestBody nyttVedtak: NyttVedtak): ResponseEntity<Ressurs<RestFagsak>> {
        val saksbehandlerId = oidcUtil.getClaim("preferred_username")

        logger.info("{} lager nytt vedtak for fagsak med id {}", saksbehandlerId ?: "Ukjent", fagsakId)

        val fagsak: Ressurs<RestFagsak> = Result.runCatching {
            behandlingService.nyttVedtakForAktivBehandling(fagsakId,
                                                           nyttVedtak,
                                                           ansvarligSaksbehandler = saksbehandlerId)
        }
                .fold(
                        onSuccess = { it },
                        onFailure = { e ->
                            Ressurs.failure("Klarte ikke å opprette nytt vedtak", e)
                        }
                )

        return ResponseEntity.ok(fagsak)
    }

    @PostMapping(path = ["/fagsak/{fagsakId}/iverksett-vedtak"])
    fun iverksettVedtak(@PathVariable fagsakId: Long): ResponseEntity<Ressurs<String>> {
        val saksbehandlerId = oidcUtil.getClaim("preferred_username")

        logger.info("{} oppretter task for iverksetting av vedtak for fagsak med id {}", saksbehandlerId ?: "Ukjent", fagsakId)

        val behandling = behandlingService.hentBehandlingHvisEksisterer(fagsakId)
                         ?: throw Error("Fant ikke behandling på fagsak $fagsakId")

        val vedtak = behandlingService.hentVedtakHvisEksisterer(behandlingId = behandling.id)
                     ?: throw Error("Fant ikke aktivt vedtak på behandling ${behandling.id}")

        if (behandling.status == BehandlingStatus.LAGT_PA_KO_FOR_SENDING_MOT_OPPDRAG
            || behandling.status == BehandlingStatus.SENDT_TIL_IVERKSETTING) {
            return ResponseEntity.ok(Ressurs.failure("Behandlingen er allerede sendt til oppdrag og venter på kvittering"))
        } else if (behandling.status == BehandlingStatus.IVERKSATT) {
            return ResponseEntity.ok(Ressurs.failure("Behandlingen er allerede iverksatt/avsluttet"))
        }

        opprettTaskIverksettMotOppdrag(behandling, vedtak, saksbehandlerId)

        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.LAGT_PA_KO_FOR_SENDING_MOT_OPPDRAG)

        return ResponseEntity
                .ok(Ressurs.success("Task for iverksetting ble opprettet på fagsak $fagsakId på vedtak ${vedtak.id}"))
    }

    private fun opprettTaskIverksettMotOppdrag(behandling: Behandling, vedtak: Vedtak, saksbehandlerId: String) {
        val task = IverksettMotOppdrag.opprettTask(behandling, vedtak, saksbehandlerId)
        taskRepository.save(task)
    }


    @GetMapping("/avstemming")
    fun settIGangAvstemming(): ResponseEntity<Ressurs<String>> {

        val iDag = LocalDateTime.now().toLocalDate().atStartOfDay()
        val taskDTO = AvstemmingTaskDTO(iDag.minusDays(1), iDag)

        logger.info("Lager task for avstemming")
        val initiellAvstemmingTask = Task.nyTaskMedTriggerTid(AvstemMotOppdrag.TASK_STEP_TYPE,
                                                              objectMapper.writeValueAsString(taskDTO),
                                                              LocalDateTime.now())
        taskRepository.save(initiellAvstemmingTask)
        return ResponseEntity.ok(Ressurs.success("Laget task for avstemming"))
    }

    @GetMapping(path = ["/behandling/{behandlingId}/vedtak-html"])
    fun hentHtmlVedtak(@PathVariable behandlingId: Long): Ressurs<String> {
        val saksbehandlerId = oidcUtil.getClaim("preferred_username")
        logger.info("{} henter vedtaksbrev", saksbehandlerId ?: "VL")
        val html = behandlingService.hentHtmlVedtakForBehandling(behandlingId)
        logger.debug(html.data)

        return html
    }

    @PostMapping(path = ["/fagsak/{fagsakId}/opphoer-migrert-vedtak"])
    fun opphørMigrertVedtak(@PathVariable fagsakId: Long): ResponseEntity<Ressurs<String>> {
        val saksbehandlerId = oidcUtil.getClaim("preferred_username")

        logger.info("{} oppretter task for opphør av migrert vedtak for fagsak med id {}", saksbehandlerId ?: "Ukjent", fagsakId)

        val behandling = behandlingService.hentBehandlingHvisEksisterer(fagsakId)
                         ?: return notFound("Fant ikke behandling på fagsak $fagsakId")

        val vedtak = behandlingService.hentVedtakHvisEksisterer(behandlingId = behandling.id)
                     ?: return notFound("Fant ikke aktivt vedtak på behandling ${behandling.id}")

        if (behandling.type != BehandlingType.MIGRERING_FRA_INFOTRYGD) {
            return forbidden("Prøver å opphøre et vedtak for behandling ${behandling.id}, som ikke er migrering")
        }

        if (behandling.status != BehandlingStatus.IVERKSATT) {
            return forbidden("Prøver å opphøre et vedtak for behandling ${behandling.id}, som ikke er iverksatt")
        }

        val task = opprettTaskOpphørBehandlingOgVedtak(behandling,
                                                        vedtak,
                                                        saksbehandlerId,
                                                        BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT)
        taskRepository.save(task)

        return ResponseEntity.ok(Ressurs.success("Task for opphør av migrert behandling og vedtak på fagsak $fagsakId opprettet"))
    }

    private fun notFound(errorMessage: String): ResponseEntity<Ressurs<String>> =
            errorResponse(HttpStatus.NOT_FOUND, errorMessage)

    private fun forbidden(errorMessage: String): ResponseEntity<Ressurs<String>> =
            errorResponse(HttpStatus.FORBIDDEN, errorMessage)

    private fun errorResponse(notFound: HttpStatus, errorMessage: String): ResponseEntity<Ressurs<String>> {
        return ResponseEntity.status(notFound).body(Ressurs.failure(errorMessage))
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(BehandlingService::class.java)
    }
}
