package no.nav.familie.ba.sak.økonomi

import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import java.time.LocalDateTime

data class KonsistensavstemmingTaskDTO(val avstemmingdato: LocalDateTime, val utbetalingsoppdrag: List<Utbetalingsoppdrag>)