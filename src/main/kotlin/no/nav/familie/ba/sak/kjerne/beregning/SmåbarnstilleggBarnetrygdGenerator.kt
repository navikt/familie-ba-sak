package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.fpsak.tidsserie.StandardCombinators
import java.math.BigDecimal
import java.time.LocalDate

data class SmåbarnstilleggBarnetrygdGenerator(
    val behandlingId: Long,
    val tilkjentYtelse: TilkjentYtelse
) {

    fun lagSmåbarnstilleggAndeler(
        perioderMedFullOvergangsstønad: List<PeriodeOvergangsstønad>,
        andelerSøker: List<AndelTilkjentYtelse>,
        barnasFødselsdatoer: List<LocalDate>
    ): List<AndelTilkjentYtelse> {
        if (perioderMedFullOvergangsstønad.isEmpty() || andelerSøker.isEmpty()) return emptyList()

        val søkersIdent = perioderMedFullOvergangsstønad.firstOrNull()?.personIdent

        val perioderMedFullOvergangsstønadTidslinje =
            LocalDateTimeline(
                perioderMedFullOvergangsstønad.map {
                    LocalDateSegment(
                        it.fomDato.førsteDagIInneværendeMåned(),
                        it.tomDato.sisteDagIMåned(),
                        listOf(SmåbarnstilleggKombinator.OVERGANGSSTØNAD)
                    )
                }
            )

        val søkersTidslinje = LocalDateTimeline(
            andelerSøker.map { andel ->
                LocalDateSegment(
                    andel.stønadFom.førsteDagIInneværendeMåned(),
                    andel.stønadTom.sisteDagIInneværendeMåned(),
                    listOf(SmåbarnstilleggKombinator.UTVIDET)
                )
            }
        )

        val barns3ÅrsTidslinjer = LocalDateTimeline(barnasFødselsdatoer.map {
            LocalDateSegment(
                it.førsteDagIInneværendeMåned(),
                it.plusYears(3).sisteDagIMåned(),
                listOf(SmåbarnstilleggKombinator.UNDER_3_ÅR)
            )
        })

        val sammenslåttTidslinje: LocalDateTimeline<List<SmåbarnstilleggKombinator>> =
            listOf(
                søkersTidslinje,
                barns3ÅrsTidslinjer
            ).fold(perioderMedFullOvergangsstønadTidslinje) { sammenlagt, neste ->
                kombinerTidslinjer(sammenlagt, neste)
            }

        return sammenslåttTidslinje.toSegments()
            .filter { segement ->
                segement.value.containsAll(
                    listOf(
                        SmåbarnstilleggKombinator.UTVIDET,
                        SmåbarnstilleggKombinator.OVERGANGSSTØNAD,
                        SmåbarnstilleggKombinator.UNDER_3_ÅR
                    )
                )
            }
            .map {
                val ordinærSatsForPeriode = SatsService.hentGyldigSatsFor(
                    satstype = SatsType.SMA,
                    stønadFraOgMed = it.fom.toYearMonth(),
                    stønadTilOgMed = it.tom.toYearMonth()
                )
                    .singleOrNull()?.sats
                    ?: error("Skal finnes én ordinær sats for gitt segment oppdelt basert på andeler")

                AndelTilkjentYtelse(
                    behandlingId = behandlingId,
                    tilkjentYtelse = tilkjentYtelse,
                    personIdent = søkersIdent
                        ?: error("Genererer andeler for småbarnstillegg uten noen perioder med full overgangsstønad"),
                    stønadFom = it.fom.toYearMonth(),
                    stønadTom = it.tom.toYearMonth(),
                    kalkulertUtbetalingsbeløp = ordinærSatsForPeriode,
                    type = YtelseType.SMÅBARNSTILLEGG,
                    sats = ordinærSatsForPeriode,
                    prosent = BigDecimal(100)
                )
            }
    }

    private fun kombinerTidslinjer(
        sammenlagtTidslinje: LocalDateTimeline<List<SmåbarnstilleggKombinator>>,
        tidslinje: LocalDateTimeline<List<SmåbarnstilleggKombinator>>
    ): LocalDateTimeline<List<SmåbarnstilleggKombinator>> {
        val sammenlagt =
            sammenlagtTidslinje.combine(
                tidslinje,
                StandardCombinators::bothValues,
                LocalDateTimeline.JoinStyle.CROSS_JOIN
            ) as LocalDateTimeline<List<List<SmåbarnstilleggKombinator>>>

        return LocalDateTimeline(
            sammenlagt.toSegments().map {
                LocalDateSegment(it.fom, it.tom, it.value.flatten())
            }
        )
    }

    enum class SmåbarnstilleggKombinator {
        OVERGANGSSTØNAD,
        UTVIDET,
        UNDER_3_ÅR
    }
}
