package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.beregning.domene.Sats
import no.nav.familie.ba.sak.beregning.domene.SatsRepository
import no.nav.familie.ba.sak.beregning.domene.SatsType
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth

@Service
class SatsService(private val satsRepository: SatsRepository) {

    fun hentGyldigSatsFor(type: SatsType, dato: LocalDate): Sats {
        return satsRepository.finnAlleSatserFor(type)
                .first { sats ->
                    when {
                        sats.gyldigFom !== null -> sats.gyldigFom <= dato
                        sats.gyldigTom !== null -> sats.gyldigTom >= dato
                        else -> true
                    }
                }
    }

    fun hentGyldigSatsFor(satstype: SatsType,
                          fom: LocalDate,
                          tom: LocalDate,
                          ignorerFra: YearMonth = YearMonth.now()): List<BeløpPeriode> {

        return hentGyldigSatsFor(satstype, YearMonth.from(fom), YearMonth.from(tom), ignorerFra)

    }

    fun hentGyldigSatsFor(satstype: SatsType,
                          fom: YearMonth,
                          tom: YearMonth,
                          ignorerFra: YearMonth = YearMonth.now()): List<BeløpPeriode> {

        return hentGyldigSatsFor(SatsPeriode(satstype, fom, tom), ignorerFra)
    }

    private fun hentGyldigSatsFor(satsPeriode: SatsPeriode, ignorerFra: YearMonth): List<BeløpPeriode> {
        val finnAlleSatserFor = satsRepository.finnAlleSatserFor(satsPeriode.satstype)

        return finnAlleSatserFor
                .map(lagSatsDerDatoerHarVerdi())
                .filter(barePerioderMedStartFørCutOff(ignorerFra))
                .map(finnBeløpsperiode(satsPeriode))
                .filter(bareGyldigePerioder())
    }

    private fun lagSatsDerDatoerHarVerdi(): (Sats) -> Sats =
            { it.copy(gyldigFom = it.gyldigFom ?: LocalDate.MIN, gyldigTom = it.gyldigTom ?: LocalDate.MAX) }

    private fun barePerioderMedStartFørCutOff(ignorerFra: YearMonth): (Sats) -> Boolean =
            { YearMonth.from(it.gyldigFom) < ignorerFra.plusMonths(1) }

    private fun bareGyldigePerioder(): (BeløpPeriode) -> Boolean =
            { it.fraOgMed <= it.tilOgMed }

    private fun finnBeløpsperiode(satsPeriode: SatsPeriode): (Sats)-> BeløpPeriode = {

        val fraOgMed = when {
            it.gyldigFom == null -> satsPeriode.fraOgMed
            YearMonth.from(it.gyldigFom) < satsPeriode.fraOgMed -> satsPeriode.fraOgMed
            else -> YearMonth.from(it.gyldigFom)
        }

        val tilOgMed = when {
            it.gyldigTom == null -> satsPeriode.tilOgMed
            YearMonth.from(it.gyldigTom) > satsPeriode.tilOgMed -> satsPeriode.tilOgMed
            else -> YearMonth.from(it.gyldigTom)
        }

        BeløpPeriode(it.beløp, fraOgMed, tilOgMed)
    }

    private data class SatsPeriode(
            val satstype: SatsType,
            val fraOgMed: YearMonth,
            val tilOgMed: YearMonth
    )

    data class BeløpPeriode(
            val beløp: Int,
            val fraOgMed: YearMonth,
            val tilOgMed: YearMonth
    )
}