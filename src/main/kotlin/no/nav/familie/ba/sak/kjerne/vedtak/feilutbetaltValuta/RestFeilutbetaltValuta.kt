package no.nav.familie.ba.sak.kjerne.vedtak.feilutbetaltValuta

import java.time.LocalDate

data class RestFeilutbetaltValuta(
    val id: Long?,
    val fom: LocalDate,
    val tom: LocalDate,
    val feilutbetaltBeløp: Int
)
