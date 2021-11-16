package no.nav.familie.ba.sak.integrasjoner.infotrygd.domene

import java.time.YearMonth

data class MigreringResponseDto(
    val fagsakId: Long,
    val behandlingId: Long,
    val infotrygdStønadId: Long? = null,
    val infotrygdSakId: Long? = null,
    val virkningFom: YearMonth? = null,
)
