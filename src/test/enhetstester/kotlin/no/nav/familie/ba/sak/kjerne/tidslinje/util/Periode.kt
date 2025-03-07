package no.nav.familie.ba.sak.kjerne.tidslinje.util

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.tidslinje.Periode
import java.time.LocalDate
import java.time.YearMonth

fun <V> periode(
    verdi: V,
    fom: LocalDate?,
    tom: LocalDate?,
) = Periode(verdi, fom, tom)

fun <V> periode(
    verdi: V,
    fom: YearMonth?,
    tom: YearMonth?,
) = Periode(verdi, fom?.førsteDagIInneværendeMåned(), tom?.sisteDagIInneværendeMåned())
