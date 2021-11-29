package no.nav.familie.ba.sak.internal

import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.kjerne.autovedtak.omregning.Autobrev6og18ÅrScheduler
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.SatsendringService
import no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg.VedtakOmOvergangsstønadService
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/internal", "/testverktoy"])
class TestVerktøyController(
    private val scheduler: Autobrev6og18ÅrScheduler,
    private val satsendringService: SatsendringService,
    private val envService: EnvService,
    private val vedtakOmOvergangsstønadService: VedtakOmOvergangsstønadService
) {

    @GetMapping(path = ["/autobrev"])
    @Unprotected
    fun kjørSchedulerForAutobrev(): ResponseEntity<Ressurs<String>> {
        return if (envService.erPreprod() || envService.erDev()) {
            scheduler.opprettTask()
            ResponseEntity.ok(Ressurs.success("Laget task."))
        } else {
            ResponseEntity.ok(Ressurs.success("Endepunktet gjør ingenting i prod."))
        }
    }

    @GetMapping(path = ["/test-satsendring/{behandlingId}"])
    @Unprotected
    fun utførSatsendringPåBehandling(@PathVariable behandlingId: Long): ResponseEntity<Ressurs<String>> {
        return if (envService.erPreprod() || envService.erDev()) {
            satsendringService.utførSatsendring(behandlingId)
            ResponseEntity.ok(Ressurs.success("Trigget satsendring"))
        } else {
            ResponseEntity.ok(Ressurs.success("Endepunktet gjør ingenting i prod."))
        }
    }

    @PostMapping(path = ["/vedtak-om-overgangsstønad"])
    @Unprotected
    fun mottaHendelseOmVedtakOmOvergangsstønad(@RequestBody personIdent: PersonIdent): ResponseEntity<Ressurs<String>> {
        return if (envService.erPreprod() || envService.erDev()) {
            val melding = vedtakOmOvergangsstønadService.håndterVedtakOmOvergangsstønad(personIdent = personIdent.ident)
            ResponseEntity.ok(Ressurs.success(melding))
        } else {
            ResponseEntity.ok(Ressurs.success("Endepunktet gjør ingenting i prod."))
        }
    }
}
