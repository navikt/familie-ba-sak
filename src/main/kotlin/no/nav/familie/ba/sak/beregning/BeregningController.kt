package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatService
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatType
import no.nav.familie.ba.sak.behandling.fagsak.FagsakController
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.restDomene.RestBeregningDetalj
import no.nav.familie.ba.sak.behandling.restDomene.RestBeregningOversikt
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.restDomene.RestPerson
import no.nav.familie.ba.sak.behandling.vedtak.AndelTilkjentYtelse
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vedtak.Ytelsetype
import no.nav.familie.ba.sak.common.RessursResponse.badRequest
import no.nav.familie.ba.sak.common.RessursResponse.notFound
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.validering.VedtaktilgangConstraint
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.fpsak.tidsserie.LocalDateInterval
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/vedtak")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BeregningController(
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        private val beregningService: BeregningService,
        private val behandlingResultatService: BehandlingResultatService,
        private val vedtakService: VedtakService
) {

    @PutMapping(path = ["/{vedtakId}/beregning"])
    fun oppdaterVedtakMedBeregning(@PathVariable @VedtaktilgangConstraint vedtakId: Long,
                                   @RequestBody nyBeregning: NyBeregning): ResponseEntity<Ressurs<RestFagsak>> {
        val saksbehandlerId = SikkerhetContext.hentSaksbehandler()

        FagsakController.logger.info("{} oppdaterer vedtak med beregning for vedtak med id {}", saksbehandlerId, vedtakId)

        if (nyBeregning.personBeregninger.isEmpty()) {
            return badRequest("Barnas beregning er tom", null)
        }

        val vedtak = vedtakService.hent(vedtakId)

        val behandling = vedtak.behandling
        val behandlingResultatType =
                behandlingResultatService.hentBehandlingResultatTypeFraBehandling(behandlingId = vedtak.behandling.id)

        if (behandlingResultatType != BehandlingResultatType.INNVILGET) {
            return badRequest("Kan ikke lage beregning på et vedtak som ikke er innvilget", null)
        }

        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)
                                       ?: return notFound("Fant ikke personopplysninggrunnlag på behandling ${behandling.id}")

        return Result.runCatching {
                    beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag, nyBeregning)
                    vedtakService.oppdaterVedtakMedStønadsbrev(vedtak)
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

    @GetMapping(path = ["/oversikt/{behandlingId}"])
    fun oversiktOverBeregnetUtbetaling(@PathVariable behandlingId: Long): ResponseEntity<Ressurs<List<RestBeregningOversikt>>> {
        val tilkjentYtelseForBehandling = beregningService.hentTilkjentYtelseForBehandling(behandlingId)
        val utbetalingsPerioder = beregnUtbetalingsperioderUtenKlassifisering(tilkjentYtelseForBehandling.andelerTilkjentYtelse)
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandling(behandlingId)
                ?: throw IllegalStateException("Fant ikke personopplysninggrunnlag for behandling $behandlingId")

        return Result.runCatching {
            utbetalingsPerioder.toSegments()
                    .sortedWith(compareBy<LocalDateSegment<Int>>({ it.fom }, { it.value }, { it.tom }))
                    .map { segment ->
                        val andelerForSegment = tilkjentYtelseForBehandling.andelerTilkjentYtelse.filter { segment.localDateInterval.overlaps(LocalDateInterval(it.stønadFom, it.stønadTom)) }
                        mapTilRestBeregningOversikt(segment, andelerForSegment, personopplysningGrunnlag)
                    }
        }.fold(
                onSuccess = { ResponseEntity.ok(Ressurs.success(data = it)) },
                onFailure = { e ->
                    ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Ressurs.failure(e.cause?.message ?: e.message,
                                    e))
                }
        )
    }

    private fun mapTilRestBeregningOversikt(segment: LocalDateSegment<Int>, andelerForSegment: List<AndelTilkjentYtelse>, personopplysningGrunnlag: PersonopplysningGrunnlag): RestBeregningOversikt {
        return RestBeregningOversikt(
                periodeFom = segment.fom,
                periodeTom = segment.tom,
                stønadstype = andelerForSegment.map(AndelTilkjentYtelse::type),
                utbetaltPerMnd = segment.value,
                antallBarn = andelerForSegment.count { andel -> personopplysningGrunnlag.barna.any { barn -> barn.id == andel.personId } },
                sakstype = "", //TODO: Denne vet jeg ikke hvor skal komme fra?
                detaljvisning = andelerForSegment.map { andel ->
                    val personForAndel = personopplysningGrunnlag.personer.find { person -> andel.personId == person.id }
                            ?: throw java.lang.IllegalStateException("Fant ikke personopplysningsgrunnlag for andel med personId ${andel.personId}")
                    RestBeregningDetalj(
                            person = RestPerson(
                                    type = personForAndel.type,
                                    kjønn = personForAndel.kjønn,
                                    navn = personForAndel.navn,
                                    fødselsdato = personForAndel.fødselsdato,
                                    personIdent = personForAndel.personIdent.ident
                            ),
                            stønadstype = andel.type,
                            utbetaltPerMnd = andel.beløp
                    )
                }
        )
    }
}

data class NyBeregning(
        val personBeregninger: List<PersonBeregning>
)

data class PersonBeregning(
        val ident: String,
        val beløp: Int,
        val stønadFom: LocalDate,
        val ytelsetype: Ytelsetype = Ytelsetype.ORDINÆR_BARNETRYGD
)
