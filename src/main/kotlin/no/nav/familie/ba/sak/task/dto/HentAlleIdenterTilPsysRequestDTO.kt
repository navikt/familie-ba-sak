package no.nav.familie.ba.sak.task.dto

import com.fasterxml.jackson.annotation.JsonAutoDetect
import java.util.UUID

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class HentAlleIdenterTilPsysRequestDTO(
    val requestId: UUID,
    val år: Int,
)
