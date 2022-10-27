package no.nav.familie.ba.sak.task.dto

import no.nav.familie.kontrakter.felles.oppdrag.PerioderForBehandling
import java.time.LocalDateTime
import java.util.UUID

data class KonsistensavstemmingStartTaskDTO(
    val batchId: Long,
    val avstemmingdato: LocalDateTime,
    val transaksjonsId: UUID = UUID.randomUUID(),
    val sendTilØkonomi: Boolean = true
)

data class KonsistensavstemmingDataTaskDTO(
    val transaksjonsId: UUID,
    val chunkNr: Int,
    val avstemmingdato: LocalDateTime,
    val perioderForBehandling: List<PerioderForBehandling>
)

data class KonsistensavstemmingAvsluttTaskDTO(
    val batchId: Long,
    val transaksjonsId: UUID,
    val avstemmingsdato: LocalDateTime
)

data class KonsistensavstemmingPerioderGeneratorTaskDTO(
    val batchId: Long,
    val transaksjonsId: UUID,
    val avstemmingsdato: LocalDateTime,
    val chunkNr: Int,
    val relevanteBehandlinger: List<Long>
)
