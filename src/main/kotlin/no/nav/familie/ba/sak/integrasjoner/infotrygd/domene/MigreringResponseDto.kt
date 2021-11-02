package no.nav.familie.ba.sak.integrasjoner.infotrygd.domene

data class MigreringResponseDto(
    val fagsakId: Long,
    val behandlingId: Long,
    val infotrygdStønadId: Long? = null,
    val infotrygdSakId: Long? = null,
)
