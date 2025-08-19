package no.nav.familie.ba.sak.integrasjoner.pdl.domene

data class PdlGeografiskTilknytningResponse(
    val geografiskTilknytning: GeografiskTilknytning,
)

data class GeografiskTilknytning(
    val gtType: String? = null,
    val gtKommune: String? = null,
)

fun GeografiskTilknytning?.erSvalbard() = this != null && gtType == "KOMMUNE" && gtKommune == "2100"
