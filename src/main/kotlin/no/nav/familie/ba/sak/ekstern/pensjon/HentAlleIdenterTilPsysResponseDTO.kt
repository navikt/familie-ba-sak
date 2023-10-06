package no.nav.familie.ba.sak.ekstern.pensjon

import java.util.UUID
data class HentAlleIdenterTilPsysResponseDTO(
    val meldingstype: Meldingstype,
    val requestId: UUID,
    val personident: String?,
    val antallIdenterTotalt: Int
)

enum class Meldingstype {
    START, DATA, SLUTT
}
