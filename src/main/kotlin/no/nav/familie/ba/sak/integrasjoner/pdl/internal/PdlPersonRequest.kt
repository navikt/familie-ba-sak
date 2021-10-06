package no.nav.familie.ba.sak.integrasjoner.pdl.internal

data class PdlPersonRequest(
    val variables: PdlPersonRequestVariables,
    val query: String
)
