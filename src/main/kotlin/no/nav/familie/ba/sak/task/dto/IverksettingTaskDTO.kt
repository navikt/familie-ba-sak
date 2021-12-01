package no.nav.familie.ba.sak.task.dto

class IverksettingTaskDTO(
    val behandlingsId: Long,
    val vedtaksId: Long,
    val saksbehandlerId: String,
    aktørId: String
) : DefaultTaskDTO(aktørId)

const val FAGSYSTEM = "BA"
