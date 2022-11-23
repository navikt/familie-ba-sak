package no.nav.familie.ba.sak.kjerne.vedtak.trekkILøpendeUtbetaling

import java.time.YearMonth

data class RestTrekkILøpendeUtbetaling(
    val id: Long,
    val behandlingId: Long,
    val fom: YearMonth?,
    val tom: YearMonth?,
    val feilutbetaltBeløp: Int
)
