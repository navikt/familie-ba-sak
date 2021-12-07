package no.nav.familie.ba.sak.integrasjoner.infotrygd.domene

import java.time.YearMonth

data class MigreringResponseDto(
    val fagsakId: Long,
    val behandlingId: Long,
    val infotrygdStønadId: Long? = null,
    val infotrygdSakId: Long? = null,
    val virkningFom: YearMonth? = null,
    //følgende 4 felt brukes som unik key i infotrygd stønad
    val infotrygdTkNr: String? = null,
    val infotrygdVirkningFom: String? = null,
    val infotrygdIverksattFom: String? = null,
    val infotrygdRegion: String? = null,
)
