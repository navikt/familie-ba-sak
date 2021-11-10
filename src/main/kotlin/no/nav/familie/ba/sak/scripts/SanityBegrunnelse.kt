package no.nav.familie.ba.sak.scripts

data class SanityBegrunnelse(
    val apiNavn: String? = "",
    val navnISystem: String? = "",
    val begrunnelsetype: String? = "",
)

data class SanityRespons(
    val ms: Int? = 0,
    val query: String? = "",
    val result: List<SanityBegrunnelse> = listOf(SanityBegrunnelse())
)
