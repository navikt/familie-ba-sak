package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakController
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakPerson
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vedtak.Ytelsetype
import no.nav.familie.ba.sak.common.RessursResponse.badRequest
import no.nav.familie.ba.sak.common.RessursResponse.notFound
import no.nav.familie.ba.sak.common.sisteDagIForrigeMåned
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.validering.VedtaktilgangConstraint
import no.nav.familie.kontrakter.felles.Ressurs
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

        if (behandling.resultat != BehandlingResultat.INNVILGET) {
            return badRequest("Kan ikke lage beregning på et vedtak som ikke er innvilget", null)
        }

        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)
                                       ?: return notFound("Fant ikke personopplysninggrunnlag på behandling ${behandling.id}")

        return Result.runCatching {
                    val vedtakPersoner = mapNyBeregningTilVedtakPerson(vedtak.id,nyBeregning,personopplysningGrunnlag)
                    vedtakService.oppdaterAktivtVedtakMedBeregning(vedtak, vedtakPersoner)
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
        val personBeregninger: List<PersonBeregning>
)

data class PersonBeregning(
        val ident: String,
        val beløp: Int,
        val stønadFom: LocalDate,
        val ytelsetype: Ytelsetype = Ytelsetype.ORDINÆR_BARNETRYGD
)

fun mapNyBeregningTilVedtakPerson(vedtakId: Long, nyBeregning: NyBeregning, personopplysningGrunnlag: PersonopplysningGrunnlag)
 : List<VedtakPerson>{

    val identBarnMap = personopplysningGrunnlag.barna
            .associateBy { it.personIdent.ident }

    return nyBeregning.personBeregninger
            .map {

                val person= identBarnMap[it.ident]
                if(person==null) {
                    error("Finner ikke person med ident ${it.ident} i personopplysningsgrunnlaget knyttet til behandlingen")
                }

                if (it.stønadFom.isBefore(person.fødselsdato)) {
                    error("Ugyldig fra og med dato for barn med fødselsdato ${person.fødselsdato}")
                }

                val sikkerStønadFom = it.stønadFom.withDayOfMonth(1)
                val sikkerStønadTom = person.fødselsdato.plusYears(18).sisteDagIForrigeMåned()

                if (sikkerStønadTom.isBefore(sikkerStønadFom)) {
                    error("Stønadens fra-og-med-dato (${sikkerStønadFom}) er etter til-og-med-dato (${sikkerStønadTom}). ")
                }

                VedtakPerson(personId = person.id,
                             vedtakId = vedtakId,
                             beløp = it.beløp,
                             stønadFom = sikkerStønadFom,
                             stønadTom = sikkerStønadTom,
                             type = it.ytelsetype)
            }

}