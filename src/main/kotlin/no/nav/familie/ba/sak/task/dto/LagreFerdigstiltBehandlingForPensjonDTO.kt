package no.nav.familie.ba.sak.task.dto

class LagreFerdigstiltBehandlingForPensjonDTO(
    val behandlingId: Long,
    personIdent: String,
) : DefaultTaskDTO(personIdent)
