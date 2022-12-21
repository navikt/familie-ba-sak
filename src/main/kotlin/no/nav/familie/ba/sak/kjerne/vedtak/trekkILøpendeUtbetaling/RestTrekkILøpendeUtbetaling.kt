package no.nav.familie.ba.sak.kjerne.vedtak.trekkILøpendeUtbetaling

import java.time.LocalDate

data class RestTrekkILøpendeUtbetaling(
    val id: Long?,
    val fom: LocalDate,
    val tom: LocalDate,
    val feilutbetaltBeløp: Int
)
