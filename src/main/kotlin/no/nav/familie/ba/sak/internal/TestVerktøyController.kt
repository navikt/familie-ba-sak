package no.nav.familie.ba.sak.internal

import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.omregning.Autobrev6og18ÅrScheduler
import no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg.VedtakOmOvergangsstønadService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.task.SatsendringTask
import no.nav.familie.ba.sak.task.StartSatsendringForAlleBehandlingerTask
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
    private val personidentService: PersonidentService,
    private val envService: EnvService,
    private val vedtakOmOvergangsstønadService: VedtakOmOvergangsstønadService,
    private val taskRepository: TaskRepositoryWrapper
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
            SatsendringTask.opprettTask(behandlingId)
            ResponseEntity.ok(Ressurs.success("Trigget satsendring for behandling $behandlingId"))
        } else {
            ResponseEntity.ok(Ressurs.success("Endepunktet gjør ingenting i prod."))
        }
    }

    @GetMapping(path = ["/test-satsendring/alle"])
    @Unprotected
    fun utførSatsendringPåAlleBehandlinger(): ResponseEntity<Ressurs<String>> {
        return if (envService.erPreprod() || envService.erDev()) {
            taskRepository.save(StartSatsendringForAlleBehandlingerTask.opprettTask(1654))
            ResponseEntity.ok(Ressurs.success("Trigget satsendring for alle behandlinger"))
        } else {
            ResponseEntity.ok(Ressurs.success("Endepunktet gjør ingenting i prod."))
        }
    }

    @PostMapping(path = ["/vedtak-om-overgangsstønad"])
    @Unprotected
    fun mottaHendelseOmVedtakOmOvergangsstønad(@RequestBody personIdent: PersonIdent): ResponseEntity<Ressurs<String>> {
        return if (envService.erPreprod() || envService.erDev()) {
            val aktør = personidentService.hentOgLagreAktør(personIdent.ident)
            val melding = vedtakOmOvergangsstønadService.håndterVedtakOmOvergangsstønad(aktør = aktør)
            ResponseEntity.ok(Ressurs.success(melding))
        } else {
            ResponseEntity.ok(Ressurs.success("Endepunktet gjør ingenting i prod."))
        }
    }
}
