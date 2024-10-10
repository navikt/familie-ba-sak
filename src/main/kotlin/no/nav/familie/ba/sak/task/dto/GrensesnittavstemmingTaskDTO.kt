package no.nav.familie.ba.sak.task.dto

import java.time.LocalDateTime
import java.util.UUID

data class GrensesnittavstemmingTaskDTO(
    val fomDato: LocalDateTime,
    val tomDato: LocalDateTime,
    val avstemmingId: UUID?,
)
