package no.nav.familie.ba.sak.kjerne.fagsak

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.ekstern.restDomene.FagsakDeltagerDto
import no.nav.familie.ba.sak.ekstern.restDomene.SøkParamDto
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/fagsaker/sok")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class FagsakDeltagerController(
    private val fagsakDeltagerService: FagsakDeltagerService,
    private val personidentService: PersonidentService,
) {
    @PostMapping
    fun søkFagsak(
        @RequestBody søkParam: SøkParamDto,
    ): ResponseEntity<Ressurs<List<FagsakDeltagerDto>>> {
        søkParam.valider()
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} søker fagsak")

        val fagsakDeltagere = fagsakDeltagerService.hentFagsakDeltagere(søkParam.personIdent)
        return ResponseEntity.ok().body(Ressurs.success(fagsakDeltagere))
    }

    @PostMapping(path = ["/fagsakdeltagere"])
    fun oppgiFagsakdeltagere(
        @RequestBody søkParamDto: SøkParamDto,
    ): ResponseEntity<Ressurs<List<FagsakDeltagerDto>>> {
        søkParamDto.valider()
        return Result
            .runCatching {
                val aktør = personidentService.hentAktør(søkParamDto.personIdent)
                val barnsAktørId = personidentService.hentAktørIder(søkParamDto.barnasIdenter)

                fagsakDeltagerService.oppgiFagsakDeltagere(aktør, barnsAktørId)
            }.fold(
                onSuccess = { ResponseEntity.ok(Ressurs.success(it)) },
                onFailure = {
                    logger.info("Henting av fagsakdeltagere feilet.")
                    secureLogger.info("Henting av fagsakdeltagere feilet: ${it.message}", it)
                    ResponseEntity
                        .status(if (it is Feil) it.httpStatus else HttpStatus.OK)
                        .body(
                            Ressurs.failure(
                                error = it,
                                errorMessage = "Henting av fagsakdeltagere feilet: ${it.message}",
                            ),
                        )
                },
            )
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(FagsakDeltagerController::class.java)
    }
}
