package no.nav.familie.ba.sak.kjerne.beregning.domene

import java.time.LocalDateTime

interface SisteAndelTilkjentYtelse {
    fun getType(): YtelseType
    fun getIdent(): String
    fun getFom(): LocalDateTime
    fun getTom(): LocalDateTime
    fun getPeriodeOffset(): Long
    fun getForrigePeriodeOffset(): Long?
    fun getKildeBehandlingId(): Long
}
