package no.nav.familie.ba.sak.kjerne.automatiskvurdering

import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/internal]", "/fagsystem"])
class AutomatiskVurderingController(
        private val envService: EnvService
) {


    @GetMapping(path = ["/sendtilmanuellbehandling"])
    @Unprotected
    fun inkrementerTellerForBehandlingSendtTilManuellOppgave(): ResponseEntity<Ressurs<String>> {
        val antallBehandlingSendtTilManuellOppgave = Metrics.counter("behandling.autovalg", "valg", "sendttilmanuelloppgave")

        return if (envService.erPreprod() || envService.erDev() || envService.erE2E()) {
            antallBehandlingSendtTilManuellOppgave.increment()
            ResponseEntity.ok(Ressurs.success("Inkrementerte teller for behandling sendt til manuell oppgave"))
        } else {
            ResponseEntity.ok(Ressurs.success("Oppdaget prod miljø, endepunktet gjør ingenting"))
        }
    }

    @GetMapping(path = ["/sendtilinfotrygd"])
    @Unprotected
    fun inkrementerTellerForBehandlingSendtTilInfotrygd(): ResponseEntity<Ressurs<String>> {
        val antallBehandlingSendtTilInfotrygd = Metrics.counter("behandling.autovalg", "valg", "sendtTilInfotrygd")

        return if (envService.erPreprod() || envService.erDev() || envService.erE2E()) {
            antallBehandlingSendtTilInfotrygd.increment()
            ResponseEntity.ok(Ressurs.success("Inkrementerte teller for behandling sendt til infotrygd"))
        } else {
            ResponseEntity.ok(Ressurs.success("Oppdaget prod miljø, endepunktet gjør ingenting"))
        }
    }

    @GetMapping(path = ["/sendtilbasak"])
    @Unprotected
    fun inkrementerTellerForBehandlingSendtTilBASak(): ResponseEntity<Ressurs<String>> {
        val antallBehandlingSendtTilBASak = Metrics.counter("behandling.autovalg", "valg", "sendtTilManuellOppgave")

        return if (envService.erPreprod() || envService.erDev() || envService.erE2E()) {
            antallBehandlingSendtTilBASak.increment()
            ResponseEntity.ok(Ressurs.success("Inkrementerte teller for behandling sendt til BA-sak"))
        } else {
            ResponseEntity.ok(Ressurs.success("Oppdaget prod miljø, endepunktet gjør ingenting"))
        }
    }


}