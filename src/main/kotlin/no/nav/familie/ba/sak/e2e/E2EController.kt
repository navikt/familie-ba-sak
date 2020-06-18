package no.nav.familie.ba.sak.e2e

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/e2e")
@ProtectedWithClaims(issuer = "azuread")
@Profile("e2e")
class E2EController(
        private val databaseCleanupService: DatabaseCleanupService,
        private val taskRepository: TaskRepository
) {

    @GetMapping(path = ["/truncate"])
    fun truncate(): ResponseEntity<Ressurs<String>> {
        databaseCleanupService.truncate()

        return ResponseEntity.ok(Ressurs.success("Truncate fullført"))
    }

    @GetMapping(path = ["/task/{key}/{value}"])
    fun hentTaskMedProperty(@PathVariable(name = "key", required = true) key: String,
                            @PathVariable(name = "value", required = true) value: String): List<Task> {
        return taskRepository.findAll().filter { it.metadata[key] == value }
    }
}