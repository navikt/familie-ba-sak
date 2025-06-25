package no.nav.familie.ba.sak.kjerne.minside

import no.nav.familie.sikkerhet.EksternBrukerUtils
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/minside/barnetrygd")
@RequiredIssuers(
    ProtectedWithClaims(
        issuer = EksternBrukerUtils.ISSUER_TOKENX,
        claimMap = ["acr=Level4"],
    ),
)
@Validated
class InnvilgetBarnetrygdController(
    private val innvilgetBarnetrygdService: InnvilgetBarnetrygdService,
) {
    @GetMapping
    fun hentInnvilgetBarnetrygd(): ResponseEntity<InnvilgetBarnetrygd> {
        val fnr = EksternBrukerUtils.hentFnrFraToken()
        val innvilgetBarnetrygd = innvilgetBarnetrygdService.hentInnvilgetBarnetrygd(fnr)
        return ResponseEntity.ok(innvilgetBarnetrygd)
    }
}
