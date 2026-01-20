package no.nav.familie.ba.sak.ekstern.restDomene

import java.time.LocalDate

data class FeilutbetaltValutaDto(
    val id: Long?,
    val fom: LocalDate,
    val tom: LocalDate,
    val feilutbetaltBeløp: Int,
)
