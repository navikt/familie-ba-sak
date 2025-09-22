package no.nav.familie.ba.sak.integrasjoner.pdl.domene

private const val KOMMUNENUMMER_SVALBARD = "2100"

data class PdlGeografiskTilknytningResponse(
    val geografiskTilknytning: GeografiskTilknytning?,
)

data class GeografiskTilknytning(
    val gtType: String? = null,
    val gtKommune: String? = null,
)

fun GeografiskTilknytning?.erSvalbard() = this != null && gtType == "KOMMUNE" && gtKommune == KOMMUNENUMMER_SVALBARD
