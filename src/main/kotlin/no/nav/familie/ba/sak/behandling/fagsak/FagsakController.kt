package no.nav.familie.ba.sak.behandling.fagsak

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsakDeltager
import no.nav.familie.ba.sak.common.RessursUtils.badRequest
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.hentSaksbehandler
import no.nav.familie.ba.sak.task.GrensesnittavstemMotOppdrag
import no.nav.familie.ba.sak.task.dto.GrensesnittavstemmingTaskDTO
import no.nav.familie.ba.sak.validering.FagsaktilgangConstraint
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import no.nav.familie.ba.sak.behandling.restDomene.RestSøkParam
import no.nav.familie.ba.sak.common.RessursUtils.illegalState

@RestController
@RequestMapping("/api")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class FagsakController(
        private val fagsakService: FagsakService,
        private val taskRepository: TaskRepository
) {

    @PostMapping(path = ["fagsaker"])
    fun hentEllerOpprettFagsak(@RequestBody fagsakRequest: FagsakRequest): ResponseEntity<Ressurs<RestFagsak>> {
        val saksbehandlerId = hentSaksbehandler()

        logger.info("{} henter eller oppretter ny fagsak", saksbehandlerId)

        return Result.runCatching { fagsakService.hentEllerOpprettFagsak(fagsakRequest) }
                .fold(
                        onSuccess = { ResponseEntity.status(HttpStatus.CREATED).body(it) },
                        onFailure = { ResponseEntity.ok(Ressurs.failure("Opprettelse eller henting av fagsak feilet", it)) }
                )
    }

    @GetMapping(path = ["fagsaker/{fagsakId}"])
    fun hentFagsak(@PathVariable @FagsaktilgangConstraint fagsakId: Long): ResponseEntity<Ressurs<RestFagsak>> {
        val saksbehandlerId = hentSaksbehandler()

        logger.info("{} henter fagsak med id {}", saksbehandlerId, fagsakId)

        return Result.runCatching { fagsakService.hentRestFagsak(fagsakId) }
                .fold(
                        onSuccess = { ResponseEntity.ok().body(it) },
                        onFailure = {
                            badRequest("Henting av fagsak med fagsakId $fagsakId feilet", it)
                        }
                )
    }

    @GetMapping("fagsaker/avstemming")
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

    @PostMapping(path = ["fagsaker/sok"])
    fun søkFagsak(@RequestBody søkParam: RestSøkParam): ResponseEntity<Ressurs<List<RestFagsakDeltager>>> {
        val saksbehandlerId = hentSaksbehandler()

        logger.info("{} søker fagsak", saksbehandlerId)

        return Result.runCatching { fagsakService.hentFagsakDeltager(søkParam.personIdent) }
                .fold(
                        onSuccess = { ResponseEntity.ok().body(Ressurs.success(it)) },
                        onFailure = {
                            logger.info("Søker fagsak feilet: ${it.message}")
                            secureLogger.info("Søker fagsak feilet: ${it.message}", it)
                            ResponseEntity.status(HttpStatus.NOT_FOUND).body(Ressurs.failure("Søker fagsak feilet: ${it.message}"))
                        }
                )
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(BehandlingService::class.java)
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}

data class FagsakRequest(
        val personIdent: String
)
