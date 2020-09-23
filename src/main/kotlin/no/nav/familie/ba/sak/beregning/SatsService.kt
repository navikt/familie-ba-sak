package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.beregning.domene.Sats
import no.nav.familie.ba.sak.beregning.domene.SatsType
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.isSameOrBefore
import java.time.LocalDate
import java.time.YearMonth

object SatsService {

    private val satser = listOf(
            Sats(SatsType.ORBA, 1054, LocalDate.of(2019, 3, 1), LocalDate.MAX),
            Sats(SatsType.ORBA, 970, LocalDate.MIN, LocalDate.of(2019, 2, 28)),
            Sats(SatsType.SMA, 660, LocalDate.MIN, LocalDate.MAX),
            Sats(SatsType.TILLEGG_ORBA, 970, LocalDate.MIN, LocalDate.of(2019, 2, 28)),
            Sats(SatsType.TILLEGG_ORBA, 1054, LocalDate.of(2019, 3, 1), LocalDate.of(2020, 8, 31)),
            Sats(SatsType.TILLEGG_ORBA, 1354, LocalDate.of(2020, 9, 1), LocalDate.MAX),
            Sats(SatsType.FINN_SVAL, 1054, LocalDate.MIN, LocalDate.of(2014, 3, 31))
    )

    fun hentGyldigSatsFor(satstype: SatsType,
                          stønadFraOgMed: YearMonth,
                          stønadTilOgMed: YearMonth,
                          maxSatsGyldigFraOgMed: YearMonth = YearMonth.now()): List<BeløpPeriode> {

        return finnAlleSatserFor(satstype)
                .map { BeløpPeriode(it.beløp, it.gyldigFom.toYearMonth(), it.gyldigTom.toYearMonth()) }
                .filter { it.fraOgMed <= maxSatsGyldigFraOgMed }
                .map { BeløpPeriode(it.beløp, maxOf(it.fraOgMed, stønadFraOgMed), minOf(it.tilOgMed, stønadTilOgMed)) }
                .filter { it.fraOgMed <= it.tilOgMed }
    }

    private fun finnAlleSatserFor(type: SatsType): List<Sats> = satser.filter { it.type == type }

    data class BeløpPeriode(
            val beløp: Int,
            val fraOgMed: YearMonth,
            val tilOgMed: YearMonth
    )

    private fun LocalDate.toYearMonth() = YearMonth.from(this)

    fun hentPeriodeUnder6år(seksårsdag: LocalDate, oppfyltFom: LocalDate, oppfyltTom: LocalDate): Periode? =
            when {
                oppfyltFom.isSameOrAfter(seksårsdag) -> {
                    null
                }
                else -> {
                    Periode(oppfyltFom, minOf(oppfyltTom, seksårsdag))
                }
            }

    fun hentPeriodeOver6år(seksårsdag: LocalDate,
                           oppfyltFom: LocalDate,
                           oppfyltTom: LocalDate): Periode? =
            when {
                oppfyltTom.isSameOrBefore(seksårsdag) -> {
                    null
                }
                else -> {
                    Periode(maxOf(oppfyltFom, seksårsdag.plusDays(1)), oppfyltTom)
                }
            }

    fun splittPeriodePå6Årsdag(seksårsdag: LocalDate, fom: LocalDate, tom: LocalDate): Pair<Periode?, Periode?> =
            Pair(hentPeriodeUnder6år(seksårsdag, fom, tom), hentPeriodeOver6år(seksårsdag, fom, tom))

    fun hentDatoForSatsendring(satstype: SatsType,
                               oppdatertBeløp: Int): LocalDate? = satser.find { it.type == satstype && it.beløp == oppdatertBeløp }?.gyldigFom
}