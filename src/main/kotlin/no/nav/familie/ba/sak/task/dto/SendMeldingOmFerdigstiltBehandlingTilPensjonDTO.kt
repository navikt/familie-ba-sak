package no.nav.familie.ba.sak.task.dto

class SendMeldingOmFerdigstiltBehandlingTilPensjonDTO(
    val behandlingsId: Long,
    personIdent: String,
) : DefaultTaskDTO(personIdent)
