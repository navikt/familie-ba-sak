package no.nav.familie.ba.sak.behandling.fagsak

import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsakDeltager
import no.nav.familie.ba.sak.behandling.restDomene.RestHentFagsakForPerson
import no.nav.familie.ba.sak.behandling.restDomene.RestPågåendeSakRequest
import no.nav.familie.ba.sak.behandling.restDomene.RestPågåendeSakResponse
import no.nav.familie.ba.sak.behandling.restDomene.RestSøkParam
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.RessursUtils.illegalState
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.validering.FagsaktilgangConstraint
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.HttpClientErrorException

@RestController
@RequestMapping("/api")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class FagsakController(
        private val fagsakService: FagsakService
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

    @PostMapping(path = ["fagsaker/sok"])
    fun søkFagsak(@RequestBody søkParam: RestSøkParam): ResponseEntity<Ressurs<List<RestFagsakDeltager>>> {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} søker fagsak")

        val fagsakDeltagere = fagsakService.hentFagsakDeltager(søkParam.personIdent)
        return ResponseEntity.ok().body(Ressurs.success(fagsakDeltagere))
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

        val logger: Logger = LoggerFactory.getLogger(this::class.java)
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}

data class FagsakRequest(
        val personIdent: String?,
        val aktørId: String? = null
)
