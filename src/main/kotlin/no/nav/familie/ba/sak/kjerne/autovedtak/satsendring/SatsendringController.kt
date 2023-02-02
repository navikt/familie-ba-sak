package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/satsendring")
@ProtectedWithClaims(issuer = "azuread")
class SatsendringController(
    private val startSatsendring: StartSatsendring
) {
    @GetMapping(path = ["/kjorsatsendring/{fagsakId}"])
    fun utførSatsendringPåFagsak(@PathVariable fagsakId: Long): ResponseEntity<Ressurs<String>> {
        startSatsendring.opprettSatsendringForFagsak(fagsakId)
        return ResponseEntity.ok(Ressurs.success("Trigget satsendring for fagsak $fagsakId"))
    }

    @PostMapping(path = ["/kjorsatsendringForListeMedIdenter"])
    fun utførSatsendringPåListeIdenter(@RequestBody listeMedIdenter: Set<String>): ResponseEntity<Ressurs<String>> {
        listeMedIdenter.forEach {
            startSatsendring.opprettSatsendringForIdent(it)
        }
        return ResponseEntity.ok(Ressurs.success("Trigget satsendring for liste med identer ${listeMedIdenter.size}"))
    }
}
