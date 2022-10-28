package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.task.KonsistensavstemMotOppdragStartTask
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingStartTaskDTO
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.transaction.Transactional

@RestController
@RequestMapping("/api/konsistensavstemming")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class KonsistensavstemmingController(
    private val taskRepository: TaskRepository,
    private val batchRepository: BatchRepository
) {

    @PostMapping(path = ["/dryrun"], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Transactional
    fun kjørKonsistensavstemmingUtenSendingTilØkonomi(): ResponseEntity<Ressurs<String>> {
        val transaksjonsId = UUID.randomUUID()
        val batch = batchRepository.saveAndFlush(Batch(kjøreDato = LocalDate.now(), status = KjøreStatus.MANUELL))
        val task = taskRepository.save(
            Task(
                type = KonsistensavstemMotOppdragStartTask.TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(
                    KonsistensavstemmingStartTaskDTO(
                        batchId = batch.id,
                        avstemmingdato = LocalDateTime.now(),
                        transaksjonsId = transaksjonsId,
                        sendTilØkonomi = false
                    )
                )
            )
        )

        return ResponseEntity.ok(Ressurs.success("Testkjører konsistensavstemming uten å sende til økonomi. transaksjonsId=$transaksjonsId callId=${task.callId}"))
    }
}
