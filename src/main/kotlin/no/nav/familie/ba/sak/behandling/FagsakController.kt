package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.auditlogger.AuditLogger
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.domene.vedtak.*
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.task.GrensesnittavstemMotOppdrag
import no.nav.familie.ba.sak.task.IverksettMotOppdrag
import no.nav.familie.ba.sak.task.OpphørVedtakTask.Companion.opprettOpphørVedtakTask
import no.nav.familie.ba.sak.task.dto.GrensesnittavstemmingTaskDTO
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
import java.time.LocalDate
import java.time.LocalDateTime

val SAKSBEHANDLER_IKKE_TILGJGENGELIG = "Saksbehandler er ikke tilgjengelig på forespørsel."

@RestController
@RequestMapping("/api/fagsak")
@ProtectedWithClaims(issuer = "azuread")
class FagsakController(
        private val oidcUtil: OIDCUtil,
        private val fagsakService: FagsakService,
        private val behandlingService: BehandlingService,
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        private val taskRepository: TaskRepository
) {

    @GetMapping(path = ["/{fagsakId}"])
    fun hentFagsak(@PathVariable fagsakId: Long): ResponseEntity<Ressurs<RestFagsak>> {
        val saksbehandlerId = hentSaksbehandler()
                              ?: return forbidden(SAKSBEHANDLER_IKKE_TILGJGENGELIG)

        logger.info("{} henter fagsak med id {}", saksbehandlerId, fagsakId)

        AuditLogger.logLesFagsak(this.javaClass, fagsakId, saksbehandlerId)

        val ressurs = Result.runCatching { fagsakService.hentRestFagsak(fagsakId) }
                .fold(
                        onSuccess = { it },
                        onFailure = { e -> Ressurs.failure("Henting av fagsak med fagsakId $fagsakId feilet", e) }
                )

        return ResponseEntity.ok(ressurs)
    }

    @PostMapping(path = ["/{fagsakId}/nytt-vedtak"])
    fun nyttVedtak(@PathVariable fagsakId: Long, @RequestBody nyttVedtak: NyttVedtak): ResponseEntity<Ressurs<RestFagsak>> {
        val saksbehandlerId = hentSaksbehandler()
                              ?: return forbidden(SAKSBEHANDLER_IKKE_TILGJGENGELIG)

        logger.info("{} lager nytt vedtak for fagsak med id {}", saksbehandlerId, fagsakId)

        val behandling = behandlingService.hentBehandlingHvisEksisterer(fagsakId)
                         ?: return notFound("Fant ikke behandling på fagsak $fagsakId")

        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)
                                       ?: return notFound("Fant ikke personopplysninggrunnlag på behandling ${behandling.id}")

        return Result.runCatching {
            behandlingService.nyttVedtakForAktivBehandling(behandling,
                                                           personopplysningGrunnlag,
                                                           nyttVedtak,
                                                           ansvarligSaksbehandler = saksbehandlerId)
        }
                .fold(
                        onSuccess = { ResponseEntity.ok(it) },
                        onFailure = { e ->
                            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Ressurs.failure(e.cause?.message ?: e.message, e))
                        }
                )
    }

    @PostMapping(path = ["/{fagsakId}/oppdater-vedtak-beregning"])
    fun oppdaterVedtakMedBeregning(@PathVariable fagsakId: Long, @RequestBody
    nyBeregning: NyBeregning): ResponseEntity<Ressurs<RestFagsak>> {
        val saksbehandlerId = hentSaksbehandler()
                              ?: return forbidden(SAKSBEHANDLER_IKKE_TILGJGENGELIG)

        logger.info("{} oppdaterer vedtak med beregning for fagsak med id {}", saksbehandlerId, fagsakId)

        if (nyBeregning.barnasBeregning.isEmpty()) {
            return badRequest("Barnas beregning er tom")
        }

        val behandling =
                behandlingService.hentBehandlingHvisEksisterer(fagsakId)
                ?: return notFound("Fant ikke behandling på fagsak $fagsakId")

        val vedtak = behandlingService.hentAktivVedtakForBehandling(behandling.id)
                     ?: return notFound("Fant ikke aktiv vedtak på fagsak $fagsakId, behandling ${behandling.id}")

        if (vedtak.resultat == VedtakResultat.AVSLÅTT) {
            return badRequest("Kan ikke lagre beregning på et avslått vedtak")
        }

        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)
                                       ?: return notFound("Fant ikke personopplysninggrunnlag på behandling ${behandling.id}")

        return Result.runCatching {
            behandlingService.oppdaterAktivVedtakMedBeregning(vedtak,
                                                              personopplysningGrunnlag,
                                                              nyBeregning)
        }
                .fold(
                        onSuccess = { ResponseEntity.ok(it) },
                        onFailure = { e ->
                            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body(Ressurs.failure(e.cause?.message ?: e.message,
                                                          e))
                        }
                )
    }

    @PostMapping(path = ["/{fagsakId}/iverksett-vedtak"])
    fun iverksettVedtak(@PathVariable fagsakId: Long): ResponseEntity<Ressurs<String>> {
        val saksbehandlerId = hentSaksbehandler()
                              ?: return forbidden(SAKSBEHANDLER_IKKE_TILGJGENGELIG)

        logger.info("{} oppretter task for iverksetting av vedtak for fagsak med id {}", saksbehandlerId, fagsakId)

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
        val taskDTO = GrensesnittavstemmingTaskDTO(iDag.minusDays(1), iDag)

        logger.info("Lager task for avstemming")
        val initiellAvstemmingTask = Task.nyTaskMedTriggerTid(GrensesnittavstemMotOppdrag.TASK_STEP_TYPE,
                                                              objectMapper.writeValueAsString(taskDTO),
                                                              LocalDateTime.now())
        taskRepository.save(initiellAvstemmingTask)
        return ResponseEntity.ok(Ressurs.success("Laget task for avstemming"))
    }

    @PostMapping(path = ["/{fagsakId}/opphoer-migrert-vedtak"])
    fun opphørMigrertVedtak(@PathVariable fagsakId: Long): ResponseEntity<Ressurs<String>> {
        val førsteNesteMåned = LocalDate.now().førsteDagINesteMåned()
        return opphørMigrertVedtak(fagsakId, Opphørsvedtak(førsteNesteMåned))
    }

    @PostMapping(path = ["/{fagsakId}/opphoer-migrert-vedtak/v2"])
    fun opphørMigrertVedtak(@PathVariable fagsakId: Long, @RequestBody
    opphørsvedtak: Opphørsvedtak): ResponseEntity<Ressurs<String>> {
        val saksbehandlerId = hentSaksbehandler()
                              ?: return forbidden(SAKSBEHANDLER_IKKE_TILGJGENGELIG)

        logger.info("{} oppretter task for opphør av migrert vedtak for fagsak med id {}", saksbehandlerId, fagsakId)

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

        val task = opprettOpphørVedtakTask(behandling,
                                           vedtak,
                                           saksbehandlerId,
                                           BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT,
                                           opphørsvedtak.opphørsdato)
        taskRepository.save(task)

        return ResponseEntity.ok(Ressurs.success("Task for opphør av migrert behandling og vedtak på fagsak $fagsakId opprettet"))
    }

    private fun hentSaksbehandler() = Result.runCatching { oidcUtil.getClaim("preferred_username") }.fold(
            onSuccess = { it },
            onFailure = { null }
    )

    private fun <T> notFound(errorMessage: String): ResponseEntity<Ressurs<T>> =
            errorResponse(HttpStatus.NOT_FOUND, errorMessage)

    private fun <T> badRequest(errorMessage: String): ResponseEntity<Ressurs<T>> =
            errorResponse(HttpStatus.BAD_REQUEST, errorMessage)

    private fun <T> forbidden(errorMessage: String): ResponseEntity<Ressurs<T>> =
            errorResponse(HttpStatus.FORBIDDEN, errorMessage)

    private fun <T> errorResponse(notFound: HttpStatus, errorMessage: String): ResponseEntity<Ressurs<T>> {
        return ResponseEntity.status(notFound).body(Ressurs.failure(errorMessage))
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(BehandlingService::class.java)
    }
}
