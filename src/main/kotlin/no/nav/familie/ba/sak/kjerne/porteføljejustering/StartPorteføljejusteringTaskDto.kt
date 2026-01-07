package no.nav.familie.ba.sak.kjerne.porteføljejustering

import no.nav.familie.ba.sak.internal.BehandlesAvApplikasjon

data class StartPorteføljejusteringTaskDto(
    val antallTasks: Int? = null,
    val behandlesAvApplikasjon: String? = null,
    val dryRun: Boolean = true,
)
