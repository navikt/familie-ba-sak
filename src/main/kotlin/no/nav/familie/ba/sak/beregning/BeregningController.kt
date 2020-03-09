package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakController
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vedtak.Ytelsetype
import no.nav.familie.ba.sak.common.RessursResponse.badRequest
import no.nav.familie.ba.sak.common.RessursResponse.notFound
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.validering.FagsaktilgangConstraint
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

// TODO: endre til beregning
@RestController
@RequestMapping("/api/vedtak")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BeregningController(
        private val behandlingService: BehandlingService,
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        private val vedtakService: VedtakService
) {

    @PutMapping(path = ["/{vedtakId}/beregning"])
    fun oppdaterVedtakMedBeregning(@PathVariable @FagsaktilgangConstraint fagsakId: Long,
                                   @RequestBody nyBeregning: NyBeregning): ResponseEntity<Ressurs<RestFagsak>> {
        val saksbehandlerId = SikkerhetContext.hentSaksbehandler()

        FagsakController.logger.info("{} oppdaterer vedtak med beregning for fagsak med id {}", saksbehandlerId, fagsakId)

        if (nyBeregning.barnasBeregning.isEmpty()) {
            return badRequest("Barnas beregning er tom")
        }

        val behandling =
                behandlingService.hentAktivForFagsak(fagsakId)
                ?: return notFound("Fant ikke behandling på fagsak $fagsakId")

        val vedtak = vedtakService.hentAktivForBehandling(behandling.id)
                     ?: return notFound("Fant ikke aktiv vedtak på fagsak $fagsakId, behandling ${behandling.id}")

        if (behandling.resultat != BehandlingResultat.INNVILGET) {
            return badRequest("Kan ikke lage beregning på et vedtak som ikke er innvilget")
        }

        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)
                                       ?: return notFound("Fant ikke personopplysninggrunnlag på behandling ${behandling.id}")

        return Result.runCatching {
                    vedtakService.oppdaterAktivVedtakMedBeregning(vedtak,
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
}

data class NyBeregning(
        val barnasBeregning: List<BarnBeregning>
)

data class BarnBeregning(
        val ident: String,
        val beløp: Int,
        val stønadFom: LocalDate,
        val ytelsetype: Ytelsetype = Ytelsetype.ORDINÆR_BARNETRYGD
)