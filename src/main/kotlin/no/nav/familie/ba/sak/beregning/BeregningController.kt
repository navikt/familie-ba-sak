package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.restDomene.RestBeregningOversikt
import no.nav.familie.ba.sak.common.RessursUtils.badRequest
import no.nav.familie.ba.sak.common.RessursUtils.notFound
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.validering.BehandlingstilgangConstraint
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/vedtak")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BeregningController(
        private val persongrunnlagService: PersongrunnlagService,
        private val beregningService: BeregningService
) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping(path = ["/oversikt/{behandlingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Deprecated("Lagt til den beregnede oversikten på restfagsak")
    fun oversiktOverBeregnetUtbetaling(@PathVariable @BehandlingstilgangConstraint behandlingId: Long)
            : ResponseEntity<Ressurs<List<RestBeregningOversikt>>> {
        val saksbehandlerId = SikkerhetContext.hentSaksbehandler()
        logger.info("$saksbehandlerId henter oversikt over beregnet utbetaling for behandlingId=$behandlingId")

        return Result.runCatching {
            val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId)
            val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId)
                                           ?: return notFound("Fant ikke personopplysninggrunnlag for behandling $behandlingId")

            TilkjentYtelseUtils.hentBeregningOversikt(tilkjentYtelse, personopplysningGrunnlag)
        }.fold(
                onSuccess = { ResponseEntity.ok(Ressurs.success(data = it)) },
                onFailure = { e ->
                    badRequest("Uthenting av beregnet utbetaling feilet ${e.cause?.message ?: e.message}", e)
                }
        )
    }
}