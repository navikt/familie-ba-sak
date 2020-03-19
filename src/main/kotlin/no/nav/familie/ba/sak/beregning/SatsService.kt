package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.beregning.domene.Sats
import no.nav.familie.ba.sak.beregning.domene.SatsRepository
import no.nav.familie.ba.sak.beregning.domene.SatsType
import org.springframework.stereotype.Service
import java.time.LocalDate
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
                          stønadFraOgMed : YearMonth,
                          stønadTilOgMed : YearMonth,
                          maxSatsGyldigFraOgMed: YearMonth = YearMonth.now()): List<BeløpPeriode> {

        return satsRepository.finnAlleSatserFor(satstype)
                .map { BeløpPeriode.nyFraNullable(it.beløp,it.gyldigFom,it.gyldigTom) }
                .filter { it.fraOgMed <= maxSatsGyldigFraOgMed }
                .map { BeløpPeriode(it.beløp, maxOf(it.fraOgMed, stønadFraOgMed), minOf(it.tilOgMed, stønadTilOgMed)) }
                .filter({ it.fraOgMed <= it.tilOgMed })
    }

    data class BeløpPeriode(
            val beløp: Int,
            val fraOgMed: YearMonth,
            val tilOgMed: YearMonth
    ) {
        companion object {
            fun nyFraNullable(beløp: Int, fraOgMed: LocalDate?, tilOgMed: LocalDate?) : BeløpPeriode {
                val fraYearMonth = YearMonth.from(fraOgMed ?: LocalDate.MIN)
                val tilYearMonth = YearMonth.from(tilOgMed?:LocalDate.MAX)

                return BeløpPeriode(beløp,fraYearMonth,tilYearMonth)
            }
        }
    }
}