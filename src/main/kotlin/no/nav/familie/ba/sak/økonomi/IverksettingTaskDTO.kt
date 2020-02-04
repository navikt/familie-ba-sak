package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.task.DefaultTaskDTO

class IverksettingTaskDTO(
        val behandlingsId: Long,
        val vedtaksId: Long,
        val saksbehandlerId: String,
        personIdent: String
) : DefaultTaskDTO(personIdent)

val FAGSYSTEM = "BA"
