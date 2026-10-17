package no.nav.familie.ba.sak.task.dto

class SendMeldingOmFerdigstiltBehandlingTilPensjonDTO(
    val behandlingId: Long,
    personIdent: String,
) : DefaultTaskDTO(personIdent)
