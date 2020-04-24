package no.nav.familie.ba.sak.beregning

import net.bytebuddy.asm.Advice
import no.nav.familie.ba.sak.beregning.domene.Sats
import no.nav.familie.ba.sak.beregning.domene.SatsRegister
import no.nav.familie.ba.sak.beregning.domene.SatsRepository
import no.nav.familie.ba.sak.beregning.domene.SatsType
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth

@Service
class SatsService(private val satsRepository: SatsRepository) {

    private val satser = listOf(
            Sats(SatsType.ORBA, 1054, LocalDate.of(2019,3,1), LocalDate.MAX),
            Sats(SatsType.ORBA, 970, LocalDate.MIN, LocalDate.of(2019,2,28)),
            Sats(SatsType.SMA, 660,LocalDate.MIN,LocalDate.MAX),
            Sats(SatsType.TILLEGG_ORBA, 1354, LocalDate.of(2020,9,1),LocalDate.MAX),
            Sats(SatsType.FINN_SVAL, 1054, LocalDate.MIN, LocalDate.of(2014,3,31))
    )

    fun hentGyldigSatsFor(type: SatsType, dato: LocalDate): Sats {
        return finnAlleSatserFor(type)
                .first { sats -> sats.gyldigFom <= dato && sats.gyldigTom >= dato }
    }

    fun hentGyldigSatsFor(satstype: SatsType,
                          stønadFraOgMed : YearMonth,
                          stønadTilOgMed : YearMonth,
                          maxSatsGyldigFraOgMed: YearMonth = YearMonth.now()): List<BeløpPeriode> {

        return finnAlleSatserFor(satstype)
                .map { BeløpPeriode(it.beløp,it.gyldigFom.toYearMonth() ,it.gyldigTom.toYearMonth()) }
                .filter { it.fraOgMed <= maxSatsGyldigFraOgMed }
                .map { BeløpPeriode(it.beløp, maxOf(it.fraOgMed, stønadFraOgMed), minOf(it.tilOgMed, stønadTilOgMed)) }
                .filter({ it.fraOgMed <= it.tilOgMed })
    }

    private fun finnAlleSatserFor(type: SatsType) : List<Sats> = satser.filter { it.type==type }

    data class BeløpPeriode(
            val beløp: Int,
            val fraOgMed: YearMonth,
            val tilOgMed: YearMonth
    )

    fun LocalDate.toYearMonth() = YearMonth.from(this)
}