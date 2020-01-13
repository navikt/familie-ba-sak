package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.task.DefaultTaskDTO

class IverksettingTaskDTO (
        val behandlingVedtakId: Long,
        val saksbehandlerId: String,
        personIdent: String
): DefaultTaskDTO(personIdent)