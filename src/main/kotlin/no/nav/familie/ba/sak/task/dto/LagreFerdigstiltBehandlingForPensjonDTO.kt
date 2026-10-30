package no.nav.familie.ba.sak.task.dto

import java.time.LocalDate

class LagreFerdigstiltBehandlingForPensjonDTO(
    val behandlingId: Long,
    personIdent: String,
    val opprettetDato: LocalDate = LocalDate.now(),
) : DefaultTaskDTO(personIdent)
