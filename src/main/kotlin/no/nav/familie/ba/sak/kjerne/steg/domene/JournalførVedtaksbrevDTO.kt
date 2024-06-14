package no.nav.familie.ba.sak.kjerne.steg.domene

import no.nav.familie.prosessering.domene.Task

data class JournalførVedtaksbrevDTO(
    val vedtakId: Long,
    val task: Task,
)
