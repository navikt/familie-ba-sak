package no.nav.familie.ba.sak.mottak

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.FagsakService
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.sikkerhet.OIDCUtil
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.exceptions.JwtTokenValidatorException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
@ProtectedWithClaims(issuer = "azuread")
class MottakController(private val oidcUtil: OIDCUtil,
                       private val behandlingService: BehandlingService,
                       private val fagsakService: FagsakService,
                       private val featureToggleService: FeatureToggleService) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping(path = ["/behandling/opprett"])
    fun opprettBehandling(@RequestBody nyBehandling: NyBehandling): ResponseEntity<Ressurs<RestFagsak>> {
        val saksbehandlerId = try {
            oidcUtil.getClaim("preferred_username") ?: "VL"
        } catch (e: JwtTokenValidatorException) {
            "VL"
        }

        if (featureToggleService.isEnabled("familie-ba-sak.lag-oppgave")){
            logger.info("FeatureToggle for lag-oppgave er skrudd på")
        } else {
            logger.info("FeatureToggle for lag-oppgave er skrudd av")
        }

        if (featureToggleService.isEnabled("familie-ba-sak.distribuer-vedtaksbrev")){
            logger.info("FeatureToggle for distribuer-vedtaksbrev er skrudd på")
        } else {
            logger.info("FeatureToggle for distribuer-vedtaksbrev er skrudd av")
        }

        logger.info("{} oppretter ny behandling", saksbehandlerId)

        return Result.runCatching { behandlingService.opprettBehandling(nyBehandling) }
                .fold(
                        onFailure = {
                            logger.info("Opprettelse av behandling feilet", it)
                            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body(Ressurs.failure(it.message, it))
                        },
                        onSuccess = { ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = it.id)) }
                )
    }

    @PostMapping(path = ["/behandling/opprettfrahendelse"])
    fun opprettEllerOppdaterBehandlingFraHendelse(@RequestBody nyBehandling: NyBehandling): ResponseEntity<Ressurs<RestFagsak>> {
        val saksbehandlerId = "VL"

        if (featureToggleService.isEnabled("familie-ba-sak.lag-oppgave")){
            logger.info("FeatureToggle for lag-oppgave er skrudd på")
        } else {
            logger.info("FeatureToggle for lag-oppgave er skrudd av")
        }

        logger.info("{} oppretter ny behandling fra hendelse", saksbehandlerId)

        return Result.runCatching { behandlingService.opprettEllerOppdaterBehandlingFraHendelse(nyBehandling) }
                .fold(
                        onFailure = {
                            logger.info("Opprettelse av behandling fra hendelse feilet", it)
                            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .body(Ressurs.failure(it.message, it))
                        },
                        onSuccess = { ResponseEntity.ok(fagsakService.hentRestFagsak(fagsakId = it.id)) }
                )
    }

}

class NyBehandling(val fødselsnummer: String,
                   val barnasFødselsnummer: Array<String>,
                   val behandlingType: BehandlingType,
                   val journalpostID: String?)
