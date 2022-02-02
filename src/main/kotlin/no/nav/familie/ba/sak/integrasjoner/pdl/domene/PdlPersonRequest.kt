package no.nav.familie.ba.sak.integrasjoner.pdl.domene

data class PdlPersonRequest(
    val variables: PdlPersonRequestVariables,
    val query: String
)
