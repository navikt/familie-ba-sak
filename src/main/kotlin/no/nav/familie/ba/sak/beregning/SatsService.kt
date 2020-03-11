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


    private fun hentGyldigSatsFor(satsPeriode: SatsPeriode, ignorerFra: YearMonth): List<BeløpPeriode> =
            satsRepository.finnAlleSatserFor(satsPeriode.satstype)
                    // Gir alle fra- og til-datoer verdi
                    .map { it.copy(gyldigFom = it.gyldigFom ?: LocalDate.MIN, gyldigTom = it.gyldigTom ?: LocalDate.MAX) }
                    // Utelukk perioder som starter i måneden etter cut-off
                    .filter { YearMonth.from(it.gyldigFom) < ignorerFra.plusMonths(1) }
                    .map { finnBeløpsperiode(it, satsPeriode) }
                    // Utelukk "negative" (ugyldige) perioder
                    .filter { it.fraOgMed<=it.tilOgMed }


    private fun finnBeløpsperiode(sats: Sats, satsPeriode: SatsPeriode): BeløpPeriode {

        val fraOgMed = when {
            sats.gyldigFom == null -> satsPeriode.fraOgMed
            YearMonth.from(sats.gyldigFom) < satsPeriode.fraOgMed -> satsPeriode.fraOgMed
            else -> YearMonth.from(sats.gyldigFom)
        }

        val tilOgMed = when {
            sats.gyldigTom == null -> satsPeriode.tilOgMed
            YearMonth.from(sats.gyldigTom) > satsPeriode.tilOgMed -> satsPeriode.tilOgMed
            else -> YearMonth.from(sats.gyldigTom)
        }

        return BeløpPeriode(sats.beløp, fraOgMed, tilOgMed)
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