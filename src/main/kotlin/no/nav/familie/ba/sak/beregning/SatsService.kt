package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.beregning.domene.Sats
import no.nav.familie.ba.sak.beregning.domene.SatsType
import no.nav.familie.ba.sak.common.*
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

    fun hentAlleSatser() = satser

    fun finnSatsendring(startDato: LocalDate, beløp: Int): List<Sats> =
            finnSatsendring(startDato)
                    .filter { it.beløp == beløp }

    fun finnSatsendring(startDato: LocalDate): List<Sats> = satser
            .filter { it.gyldigFom == startDato }
            .filter { it.gyldigFom != LocalDate.MIN }

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

    fun hentPeriodeTil6år(seksårsdag: LocalDate, oppfyltFom: LocalDate, oppfyltTom: LocalDate): Periode? =
            when {
                oppfyltFom.toYearMonth().isSameOrAfter(seksårsdag.toYearMonth()) -> {
                    null
                }
                else -> {
                    Periode(oppfyltFom,
                            minOf(oppfyltTom, seksårsdag.sisteDagIForrigeMåned()))
                }
            }

    fun hentPeriodeFraOgMed6år(seksårsdag: LocalDate,
                               oppfyltFom: LocalDate,
                               oppfyltTom: LocalDate): Periode? =
            when {
                oppfyltTom.toYearMonth().isBefore(seksårsdag.toYearMonth()) -> {
                    null
                }
                else -> {
                    Periode(maxOf(oppfyltFom, seksårsdag.førsteDagIInneværendeMåned()), oppfyltTom)
                }
            }

    /**
     * Denne splitter perioden basert på seksårsalderen til barnet.
     * F.eks. hvis barnet fyller 6 år 10.10.2020 og perioden er 01.01.2020 - 31.13.2020
     * får vi 2 nye perioder:
     * 01.01.2020 - 30.09.2020
     * 01.10.2020 - 31.12.2020
     */
    fun splittPeriodePå6Årsdag(seksårsdag: LocalDate, fom: LocalDate, tom: LocalDate): Pair<Periode?, Periode?> =
            Pair(hentPeriodeTil6år(seksårsdag, fom, tom), hentPeriodeFraOgMed6år(seksårsdag, fom, tom))

    fun hentDatoForSatsendring(satstype: SatsType,
                               oppdatertBeløp: Int): LocalDate? = satser.find { it.type == satstype && it.beløp == oppdatertBeløp }?.gyldigFom
}