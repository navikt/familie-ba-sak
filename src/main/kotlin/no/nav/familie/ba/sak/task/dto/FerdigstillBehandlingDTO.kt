package no.nav.familie.ba.sak.task.dto

class FerdigstillBehandlingDTO(
    val behandlingsId: Long,
    aktørId: String
) : DefaultTaskDTO(aktørId)
