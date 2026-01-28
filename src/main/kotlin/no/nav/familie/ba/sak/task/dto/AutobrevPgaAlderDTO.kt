package no.nav.familie.ba.sak.task.dto

import com.fasterxml.jackson.annotation.JsonAutoDetect
import java.time.YearMonth

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class AutobrevPgaAlderDTO(
    val fagsakId: Long,
    val alder: Int,
    val årMåned: YearMonth,
)
