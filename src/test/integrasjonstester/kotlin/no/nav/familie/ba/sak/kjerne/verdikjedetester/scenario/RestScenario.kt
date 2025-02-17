package no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario

data class RestScenario(
    val søker: RestScenarioPerson,
    val barna: List<RestScenarioPerson>,
)
