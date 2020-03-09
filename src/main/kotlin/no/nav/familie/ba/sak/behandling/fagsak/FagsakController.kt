package no.nav.familie.ba.sak.behandling.fagsak

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
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

@RestController
@RequestMapping("/api")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class FagsakController(
        private val fagsakService: FagsakService,
        private val taskRepository: TaskRepository
) {

    @PostMapping(path = ["fagsaker"])
    fun nyFagsak(@RequestBody nyFagsak: NyFagsak): ResponseEntity<Ressurs<RestFagsak>> {
        val saksbehandlerId = hentSaksbehandler()

        logger.info("{} oppretter ny fagsak", saksbehandlerId)

        return Result.runCatching { fagsakService.nyFagsak(nyFagsak) }
                .fold(
                        onSuccess = { ResponseEntity.status(HttpStatus.CREATED).body(it) },
                        onFailure = { e -> ResponseEntity.ok(Ressurs.failure("Opprettelse av fagsak feilet", e)) }
                )
    }

    @GetMapping(path = ["fagsaker/{fagsakId}"])
    fun hentFagsak(@PathVariable @FagsaktilgangConstraint fagsakId: Long): ResponseEntity<Ressurs<RestFagsak>> {
        val saksbehandlerId = hentSaksbehandler()

        logger.info("{} henter fagsak med id {}", saksbehandlerId, fagsakId)

        val ressurs = Result.runCatching { fagsakService.hentRestFagsak(fagsakId) }
                .fold(
                        onSuccess = { it },
                        onFailure = {
                            logger.error("Henting av fagsak med fagsakId $fagsakId feilet", it)
                            Ressurs.failure("Henting av fagsak med fagsakId $fagsakId feilet", it)
                        }
                )

        return ResponseEntity.ok().body(ressurs)
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

    companion object {
        val logger: Logger = LoggerFactory.getLogger(BehandlingService::class.java)
    }
}

data class NyFagsak(
        val personIdent: String
)
