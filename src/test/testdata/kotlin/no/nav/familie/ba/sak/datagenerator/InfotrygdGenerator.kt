package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.kontrakter.ba.infotrygd.Stønad
import java.time.YearMonth
import java.time.format.DateTimeFormatter

fun lagInfotrygdstønad(
    virkningFom: YearMonth,
    opphørtFom: YearMonth?,
): Stønad =
    Stønad(
        virkningFom = (999999 - virkningFom.format(DateTimeFormatter.ofPattern("yyyyMM")).toInt()).toString(),
        opphørtFom = opphørtFom?.format(DateTimeFormatter.ofPattern("MMyyyy")) ?: "000000",
    )
