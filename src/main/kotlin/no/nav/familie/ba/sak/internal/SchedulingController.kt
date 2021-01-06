package no.nav.familie.ba.sak.internal

import no.nav.familie.ba.sak.behandling.autobrev.Autobrev6og18ÅrScheduler
import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal")
@Profile(value = ["dev"])
class SchedulingController(
        private val scheduler: Autobrev6og18ÅrScheduler,
private val envService: EnvService) {

    @GetMapping(path = ["/autobrev"])
    @Unprotected
    fun kjørSchedulerForAutobrev(): ResponseEntity<Ressurs<String>> {
        return if (!envService.erProd()) {
            scheduler.opprettTask()
            ResponseEntity.ok(Ressurs.success("Laget task."))
        } else {
            ResponseEntity.ok(Ressurs.success("Endepunktet gjør ingenting i prod."))
        }
    }
}