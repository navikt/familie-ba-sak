package no.nav.familie.ba.sak.kjerne.porteføljejustering

data class PorteføljejusteringFlyttOppgaveDto(
    val oppgaveId: Long,
    val originalEnhet: String,
    val originalMappeId: Long? = null,
)
