package no.nav.familie.ba.sak.ekstern.bisys

import io.swagger.annotations.ApiOperation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import no.nav.familie.ba.sak.common.EksternTjenesteFeil
import no.nav.familie.ba.sak.common.EksternTjenesteFeilException
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.YearMonth

@RestController
@RequestMapping("/api/bisys")
@ProtectedWithClaims(issuer = "azuread")
class BisysController(private val bisysService: BisysService) {


    @ApiOperation("Tjeneste for BISYS for å hente utvidet barnetrygd og småbarnstillegg for en gitt person. ")

    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "Liste over perioder som brukeren har hatt innvilget ytelse", content = [
                    (Content(
                        mediaType = "application/json", array = (
                                ArraySchema(schema = Schema(implementation = UtvidetBarnetrygdPeriode::class)))
                    ))]
            ),
            ApiResponse(responseCode = "400", description = "Ugyldig input. fraDato maks tilbake 5 år", content = [Content()]),
            ApiResponse(responseCode = "500", description = "Uventet feil", content = [Content()])]
    )
    @PostMapping(
        path = ["/hent-utvidet-barnetrygd"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun hentUtvidetBarnetrygd(
        @RequestBody
        request: BisysUtvidetBarnetrygdRequest
    ): ResponseEntity<BisysUtvidetBarnetrygdResponse> {
        if (LocalDate.now().minusYears(5).isAfter(request.fraDato)) {
            throw EksternTjenesteFeilException(
                EksternTjenesteFeil(
                    "/api/bisys/hent-utvidet-barnetrygd",
                    HttpStatus.BAD_REQUEST
                ),
                "fraDato kan ikke være lenger enn 5 år tilbake i tid",
                request,
            )
        }

        return try {
            return ResponseEntity.ok(bisysService.hentUtvidetBarnetrygd(request.personIdent, request.fraDato))
        } catch (e: RuntimeException) {
            throw EksternTjenesteFeilException(
                EksternTjenesteFeil("/api/bisys/hent-utvidet-barnetrygd"),
                e.message ?: "Ukjent feil ved hent utvidet barnetrygd",
                request,
                e
            )
        }
    }
}

data class BisysUtvidetBarnetrygdRequest(
    val personIdent: String,
    val fraDato: LocalDate
)

class BisysUtvidetBarnetrygdResponse(val perioder: List<UtvidetBarnetrygdPeriode>)
data class UtvidetBarnetrygdPeriode(val stønadstype: BisysStønadstype, val fomMåned: YearMonth, val tomMåned: YearMonth?, val beløp: Double)


enum class BisysStønadstype {
    UTVIDET,
    SMÅBARNSTILLEGG
}