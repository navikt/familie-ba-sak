package no.nav.familie.ba.sak.integrasjoner.infotrygd

import no.nav.familie.kontrakter.ba.infotrygd.Stønad
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private const val UTEN_DATO = "000000"

// Infotrygd lagrer virkningFom invertert, som 999999 - yyyyMM
private const val INVERTERT_DATO_GRUNNLAG = 999999

private val ÅR_MÅNED_FORMAT = DateTimeFormatter.ofPattern("yyyyMM")
private val MÅNED_ÅR_FORMAT = DateTimeFormatter.ofPattern("MMyyyy")

val Stønad.virkningFomSomYearMonth: YearMonth?
    get() {
        val dato = virkningFom.takeUnless { it == UTEN_DATO } ?: return null
        return YearMonth.parse("${INVERTERT_DATO_GRUNNLAG - dato.toInt()}", ÅR_MÅNED_FORMAT)
    }

val Stønad.opphørtFomSomYearMonth: YearMonth?
    get() {
        val dato = opphørtFom.takeUnless { it == UTEN_DATO } ?: return null
        return YearMonth.parse(dato, MÅNED_ÅR_FORMAT)
    }
