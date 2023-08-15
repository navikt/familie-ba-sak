package no.nav.familie.ba.sak.ekstern.pensjon

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import no.nav.familie.ba.sak.common.EksternTjenesteFeil
import no.nav.familie.ba.sak.common.EksternTjenesteFeilException
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/ekstern/pensjon")
@ProtectedWithClaims(issuer = "azuread")
class PensjonController(private val pensjonService: PensjonService) {

    @Operation(
        description = "Tjeneste for Pensjon for å hente barnetrygd og relaterte saker for en gitt person.",

    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description =
                """Liste over fagsaker og relaterte fagsaker(hvis barna finnes i flere fagsaker) fra ba-sak 
                                        
                   fagsakId:                unik id for fagsaken
                   fagsakEiersIdent:        Fnr for eier av fagsaken
                   barnetrygdPerioder:      Liste over perioder med barnetrygd
                   
                """,
                content = [
                    Content(
                        mediaType = "application/json",
                        array = (
                            ArraySchema(schema = Schema(implementation = BarnetrygdTilPensjon::class))
                            ),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Ugyldig input. fraDato maks tilbake 2 år",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "500",
                description = "Uventet feil",
                content = [
                    Content(
                        mediaType = "application/json",
                        array = (
                            ArraySchema(schema = Schema(implementation = Ressurs::class))
                            ),
                    ),
                ],
            ),
        ],
    )
    @PostMapping(
        path = ["/hent-barnetrygd"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentBarnetrygd(
        @RequestBody
        request: BarnetrygdTilPensjonRequest,
    ): ResponseEntity<BarnetrygdTilPensjonResponse> {
        if (LocalDate.now().minusYears(2).isAfter(request.fraDato)) {
            throw EksternTjenesteFeilException(
                EksternTjenesteFeil(
                    "/api/ekstern/pensjon/hent-barnetrygd",
                    HttpStatus.BAD_REQUEST,
                ),
                "fraDato kan ikke være lenger enn 2 år tilbake i tid",
                request,
            )
        }

        return ResponseEntity.ok(
            BarnetrygdTilPensjonResponse(
                pensjonService.hentBarnetrygd(
                    request.ident,
                    request.fraDato,
                ),
            ),
        )
    }

    @Operation(
        description = "Tjeneste for Pensjon for å hente barnetrygd og relaterte saker for en gitt person.",

    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description =
                """Liste over fagsaker og relaterte fagsaker(hvis barna finnes i flere fagsaker) fra ba-sak 
                                        
                   fagsakId:                unik id for fagsaken
                   fagsakEiersIdent:        Fnr for eier av fagsaken
                   barnetrygdPerioder:      Liste over perioder med barnetrygd
                   
                """,
                content = [
                    Content(
                        mediaType = "application/json",
                        array = (
                            ArraySchema(schema = Schema(implementation = BarnetrygdTilPensjon::class))
                            ),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Ugyldig input. fraDato maks tilbake 2 år",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "500",
                description = "Uventet feil",
                content = [
                    Content(
                        mediaType = "application/json",
                        array = (
                            ArraySchema(schema = Schema(implementation = Ressurs::class))
                            ),
                    ),
                ],
            ),
        ],
    )
    @PostMapping(
        path = ["/bestill-personer-med-barnetrygd"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun bestillPersonerMedBarnetrygdForGittÅrPåKafka(
        @RequestBody
        år: String,
    ): ResponseEntity<String> {
        // lage task som henter identer

        return ResponseEntity.accepted().body("id-fra-task")
    }
}
