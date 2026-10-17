package no.nav.familie.ba.sak.ekstern.pensjon.kafka

import java.time.LocalDate
import java.time.LocalDateTime

data class FerdigstiltBehandlingHendelse(
    val ident: String,
    val vedtaktidspunkt: LocalDateTime,
    val endringstidspunkt: LocalDate,
)
