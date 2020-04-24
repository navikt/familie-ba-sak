package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.restDomene.RestBeregningDetalj
import no.nav.familie.ba.sak.behandling.restDomene.RestBeregningOversikt
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.RestPerson
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.common.RessursUtils.badRequest
import no.nav.familie.ba.sak.common.RessursUtils.notFound
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.validering.BehandlingstilgangConstraint
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.fpsak.tidsserie.LocalDateInterval
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
        private val fagsakService: FagsakService,
        private val vedtakService: VedtakService
) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping(path = ["/oversikt/{behandlingId}"])
    fun oversiktOverBeregnetUtbetaling(@PathVariable @BehandlingstilgangConstraint behandlingId: Long)
            : ResponseEntity<Ressurs<List<RestBeregningOversikt>>> {
        val saksbehandlerId = SikkerhetContext.hentSaksbehandler()
        logger.info("{} henter oversikt over beregnet utbetaling for behandlingId={}", saksbehandlerId, behandlingId)

        val tilkjentYtelseForBehandling = beregningService.hentTilkjentYtelseForBehandling(behandlingId)
        val utbetalingsPerioder = beregnUtbetalingsperioderUtenKlassifisering(tilkjentYtelseForBehandling.andelerTilkjentYtelse)
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandling(behandlingId)
                ?: return notFound("Fant ikke personopplysninggrunnlag for behandling $behandlingId")

        return Result.runCatching {
            utbetalingsPerioder.toSegments()
                    .sortedWith(compareBy<LocalDateSegment<Int>>({ it.fom }, { it.value }, { it.tom }))
                    .map { segment ->
                        val andelerForSegment = tilkjentYtelseForBehandling.andelerTilkjentYtelse.filter {
                            segment.localDateInterval.overlaps(LocalDateInterval(it.stønadFom, it.stønadTom))
                        }
                        mapTilRestBeregningOversikt(segment, andelerForSegment, tilkjentYtelseForBehandling.behandling, personopplysningGrunnlag)
                    }
        }.fold(
                onSuccess = { ResponseEntity.ok(Ressurs.success(data = it)) },
                onFailure = { e ->
                    badRequest("Uthenting av beregnet utbetaling feilet ${e.cause?.message ?: e.message}", e)
                }
        )
    }

    private fun mapTilRestBeregningOversikt(segment: LocalDateSegment<Int>,
                                            andelerForSegment: List<AndelTilkjentYtelse>,
                                            behandling: Behandling,
                                            personopplysningGrunnlag: PersonopplysningGrunnlag): RestBeregningOversikt {
        return RestBeregningOversikt(
                periodeFom = segment.fom,
                periodeTom = segment.tom,
                ytelseTyper = andelerForSegment.map(AndelTilkjentYtelse::type),
                utbetaltPerMnd = segment.value,
                antallBarn = andelerForSegment.count { andel -> personopplysningGrunnlag.barna.any { barn -> barn.id == andel.personId } },
                sakstype = behandling.kategori,
                beregningDetaljer = andelerForSegment.map { andel ->
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
                            ytelseType = andel.type,
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
        val ytelseType: YtelseType = YtelseType.ORDINÆR_BARNETRYGD
)
