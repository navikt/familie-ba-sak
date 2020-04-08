package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.RestFagsak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vedtak.Ytelsetype
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
    private val fagsakService: FagsakService,
    private val vedtakService: VedtakService
) {

    @Deprecated("Erstattes av direkte mapping fra vilkårsvurdering")
    @PutMapping(path = ["/{vedtakId}/beregning"])
    fun oppdaterVedtakMedBeregning(@PathVariable @VedtaktilgangConstraint vedtakId: Long,
                                   @RequestBody nyBeregning: NyBeregning): ResponseEntity<Ressurs<RestFagsak>> {

        val vedtak = vedtakService.hent(vedtakId)
        return Result.runCatching {
            fagsakService.hentRestFagsak(vedtak.behandling.fagsak.id)
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
