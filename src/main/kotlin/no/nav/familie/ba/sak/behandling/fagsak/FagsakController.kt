package no.nav.familie.ba.sak.behandling.fagsak

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.restDomene.*
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.RessursUtils.illegalState
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.GrensesnittavstemMotOppdrag
import no.nav.familie.ba.sak.task.dto.GrensesnittavstemmingTaskDTO
import no.nav.familie.ba.sak.validering.FagsaktilgangConstraint
import no.nav.familie.ba.sak.validering.PersontilgangConstraint
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.HttpClientErrorException
import java.time.LocalDateTime

@RestController
@RequestMapping("/api")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class FagsakController(
        private val fagsakService: FagsakService,
        private val taskRepository: TaskRepository
) {

    @PostMapping(path = ["fagsaker"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentEllerOpprettFagsak(@RequestBody fagsakRequest: FagsakRequest): ResponseEntity<Ressurs<RestFagsak>> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} henter eller oppretter ny fagsak")

        return Result.runCatching { fagsakService.hentEllerOpprettFagsak(fagsakRequest) }
                .fold(
                        onSuccess = { ResponseEntity.status(HttpStatus.CREATED).body(it) },
                        onFailure = { throw it }
                )
    }

    @GetMapping(path = ["fagsaker/{fagsakId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentFagsak(@PathVariable @FagsaktilgangConstraint fagsakId: Long): ResponseEntity<Ressurs<RestFagsak>> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} henter fagsak med id $fagsakId")

        val fagsak = fagsakService.hentRestFagsak(fagsakId)
        return ResponseEntity.ok().body(fagsak)
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
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} søker fagsak")

        return Result.runCatching { fagsakService.hentFagsakDeltager(søkParam.personIdent) }
                .fold(
                        onSuccess = { ResponseEntity.ok().body(Ressurs.success(it)) },
                        onFailure = {
                            val clientError = it as? HttpClientErrorException?
                            if (clientError != null && clientError.statusCode == HttpStatus.NOT_FOUND) {
                                logger.info("Søker fagsak feilet: ${it.message}")
                                secureLogger.info("Søker fagsak feilet: ${it.message}", it)
                                ResponseEntity.ok().body(Ressurs.failure("Søk på fagsak feilet: ${it.message}"))
                            } else {
                                illegalState("Søker fagsak feilet: ${it.message}", it)
                            }
                        }
                )
    }

    @PostMapping(path = ["fagsaker/sok/ba-sak-og-infotrygd"])
    fun søkEtterPågåendeSak(@RequestBody restSøkParam: RestPågåendeSakRequest): ResponseEntity<Ressurs<RestPågåendeSakResponse>> {
        return Result.runCatching {
            fagsakService.hentPågåendeSakStatus(restSøkParam.personIdent,
                                                restSøkParam.barnasIdenter ?: emptyList())
        }
                .fold(
                        onSuccess = { ResponseEntity.ok(Ressurs.success(it)) },
                        onFailure = {
                            logger.info("Søk etter pågående sak feilet.")
                            secureLogger.info("Søk etter pågående sak feilet: ${it.message}", it)
                            ResponseEntity
                                    .status(if (it is Feil) it.httpStatus else HttpStatus.OK)
                                    .body(Ressurs.failure(error = it,
                                                          errorMessage = "Søk etter pågående sak feilet: ${it.message}"))
                        }
                )
    }

    @PostMapping(path = ["fagsaker/hent-fagsak-paa-person"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentRestFagsak(@RequestBody request: RestHentFagsakForPerson)
            : ResponseEntity<Ressurs<RestFagsak?>> {

        return Result.runCatching {
            fagsakService.hentRestFagsakForPerson(PersonIdent(request.personIdent))
        }.fold(
                onSuccess = { return ResponseEntity.ok().body(it) },
                onFailure = { illegalState("Ukjent feil ved henting data for manuell journalføring.", it) }
        )
    }

    companion object {

        val logger: Logger = LoggerFactory.getLogger(BehandlingService::class.java)
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}

data class FagsakRequest(
        val personIdent: String?,
        val aktørId: String? = null
)
