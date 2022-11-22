package no.nav.familie.ba.sak.kjerne.vedtak.trekkILøpendeUtbetaling

import java.time.LocalDate

data class RestTrekkILøpendeUtbetaling(
    val behandlingId: Long,
    val fom: LocalDate,
    val tom: LocalDate,
    val sum: Int
)