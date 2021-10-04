package no.nav.familie.ba.sak.internal

import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.kjerne.autobrev.Autobrev6og18ÅrScheduler
import no.nav.familie.ba.sak.kjerne.autorevurdering.SatsendringService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/internal", "/testverktoy"])
class SchedulingController(
    private val scheduler: Autobrev6og18ÅrScheduler,
    private val satsendringService: SatsendringService,
    private val envService: EnvService
) {

    @GetMapping(path = ["/autobrev"])
    @Unprotected
    fun kjørSchedulerForAutobrev(): ResponseEntity<Ressurs<String>> {
        return if (envService.erPreprod() || envService.erDev() || envService.erE2E()) {
            scheduler.opprettTask()
            ResponseEntity.ok(Ressurs.success("Laget task."))
        } else {
            ResponseEntity.ok(Ressurs.success("Endepunktet gjør ingenting i prod."))
        }
    }

    @GetMapping(path = ["/test-satsendring/{behandlingId}"])
    @Unprotected
    fun utførSatsendringPåBehandling(@PathVariable behandlingId: Long): ResponseEntity<Ressurs<String>> {
        return if (envService.erPreprod() || envService.erDev() || envService.erE2E()) {
            satsendringService.utførSatsendring(behandlingId)
            ResponseEntity.ok(Ressurs.success("Trigget satsendring"))
        } else {
            ResponseEntity.ok(Ressurs.success("Endepunktet gjør ingenting i prod."))
        }
    }
}
