package no.nav.familie.ba.sak.kjerne

import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBrevkode
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/testautobrev")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class TestAutobrevController(
    private val opprettTaskService: OpprettTaskService,
    private val infotrygdService: InfotrygdService
) {

    @GetMapping("/6/{fagsakId}")
    @Unprotected
    fun opprettAutobrev6(@PathVariable fagsakId: Long): ResponseEntity<Ressurs<String>> {
        opprettTaskService.opprettAutovedtakFor6Og18ÅrBarn(fagsakId, 6)
        return ResponseEntity.ok(Ressurs.success("Håndtert ny ident"))
    }

    @GetMapping("/18/{fagsakId}")
    @Unprotected
    fun opprettAutobrev18(@PathVariable fagsakId: Long): ResponseEntity<Ressurs<String>> {
        opprettTaskService.opprettAutovedtakFor6Og18ÅrBarn(fagsakId, 18)
        return ResponseEntity.ok(Ressurs.success("Håndtert ny ident"))
    }

    @GetMapping("/smaabarntillegg/{fagsakId}")
    @Unprotected
    fun opprettAutobrevSmåbarnstilleg(@PathVariable fagsakId: Long): ResponseEntity<Ressurs<String>> {
        opprettTaskService.opprettAutovedtakForOpphørSmåbarnstilleggTask(fagsakId)
        return ResponseEntity.ok(Ressurs.success("Håndtert ny ident"))
    }

    @PostMapping("/brev/{brevkode}")
    @Unprotected
    fun harsendtBrev(@RequestBody ident: PersonIdent, @PathVariable brevkode: String): ResponseEntity<Ressurs<String>> {
        infotrygdService.harSendtbrev(listOf(ident.ident), listOf(InfotrygdBrevkode.valueOf(brevkode)))
        return ResponseEntity.ok(Ressurs.success("Håndtert ny ident"))
    }
}
