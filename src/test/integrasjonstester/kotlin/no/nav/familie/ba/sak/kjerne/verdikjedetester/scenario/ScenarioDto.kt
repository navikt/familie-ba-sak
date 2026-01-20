package no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario

data class ScenarioDto(
    val søker: ScenarioPersonDto,
    val barna: List<ScenarioPersonDto>,
)
