package no.nav.familie.ba.sak.integrasjoner.pdl.domene

data class PdlPersonRequestVariables(
    var ident: String,
    var historikk: Boolean = false,
)

data class PdlPersonBolkRequestVariables(
    var identer: List<String>,
)
