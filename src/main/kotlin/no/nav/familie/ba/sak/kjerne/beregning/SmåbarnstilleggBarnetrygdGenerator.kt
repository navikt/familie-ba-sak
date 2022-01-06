package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.slåSammenOverlappendePerioder
import no.nav.familie.ba.sak.common.slåSammenSammenhengendePerioder
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.fpsak.tidsserie.StandardCombinators
import java.math.BigDecimal
import java.time.LocalDate

data class SmåbarnstilleggBarnetrygdGenerator(
    val behandlingId: Long,
    val tilkjentYtelse: TilkjentYtelse,
) {

    fun lagSmåbarnstilleggAndeler(
        perioderMedFullOvergangsstønad: List<InternPeriodeOvergangsstønad>,
        andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
        barnasIdenterOgFødselsdatoer: List<Pair<String, LocalDate>>,
    ): List<AndelTilkjentYtelse> {
        val (søkersAndeler, barnasAndeler) = andelerTilkjentYtelse.partition { it.erSøkersAndel() }

        if (perioderMedFullOvergangsstønad.isEmpty() || søkersAndeler.isEmpty()) return emptyList()

        val søkerAktør = søkersAndeler.firstOrNull()?.aktør

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
            søkersAndeler.map { andel ->
                LocalDateSegment(
                    andel.stønadFom.førsteDagIInneværendeMåned(),
                    andel.stønadTom.sisteDagIInneværendeMåned(),
                    listOf(SmåbarnstilleggKombinator.UTVIDET)
                )
            }
        )

        val perioderMedBarnSomGirRettTilSmåbarnstillegg = LocalDateTimeline(
            lagPerioderMedBarnSomGirRettTilSmåbarnstillegg(
                barnasIdenterOgFødselsdatoer = barnasIdenterOgFødselsdatoer,
                barnasAndeler = barnasAndeler
            ).map {
                LocalDateSegment(
                    it.fom.førsteDagIInneværendeMåned(),
                    it.tom.sisteDagIInneværendeMåned(),
                    listOf(SmåbarnstilleggKombinator.UNDER_3_ÅR)
                )
            }
        )

        val sammenslåttTidslinje: LocalDateTimeline<List<SmåbarnstilleggKombinator>> =
            listOf(
                søkersTidslinje,
                perioderMedBarnSomGirRettTilSmåbarnstillegg
            ).fold(perioderMedFullOvergangsstønadTidslinje) { sammenlagt, neste ->
                kombinerTidslinjerForÅLageSmåbarnstilleggAndeler(sammenlagt, neste)
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
                    aktør = søkerAktør
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

    fun lagPerioderMedBarnSomGirRettTilSmåbarnstillegg(
        barnasIdenterOgFødselsdatoer: List<Pair<String, LocalDate>>,
        barnasAndeler: List<AndelTilkjentYtelse>
    ): List<MånedPeriode> {
        return slåSammenOverlappendePerioder(
            barnasIdenterOgFødselsdatoer.map { (ident, fødselsdato) ->
                val barnetsMånedPeriodeAndeler = LocalDateTimeline(
                    barnasAndeler
                        .filter { andel -> andel.aktør.aktivFødselsnummer() == ident }
                        .map { andel ->
                            LocalDateSegment(
                                andel.stønadFom.førsteDagIInneværendeMåned(),
                                andel.stønadTom.sisteDagIInneværendeMåned(),
                                listOf(BarnSinRettTilSmåbarnstilleggKombinator.UTBETALING)
                            )
                        }
                )

                val barnetsUnder3ÅrTidslinje = LocalDateTimeline(
                    listOf(
                        LocalDateSegment(
                            fødselsdato.førsteDagIInneværendeMåned(),
                            fødselsdato.plusYears(3).sisteDagIMåned(),
                            listOf(BarnSinRettTilSmåbarnstilleggKombinator.UNDER_3_ÅR)
                        )
                    )
                )

                listOf(barnetsMånedPeriodeAndeler)
                    .fold(barnetsUnder3ÅrTidslinje) { sammenlagt, neste ->
                        kombinerTidslinjerForÅLageBarnasPerioderMedRettPåSmåbarnstillegg(sammenlagt, neste)
                    }.toSegments()
                    .filter { segement ->
                        segement.value.containsAll(
                            listOf(
                                BarnSinRettTilSmåbarnstilleggKombinator.UTBETALING,
                                BarnSinRettTilSmåbarnstilleggKombinator.UNDER_3_ÅR,
                            )
                        )
                    }.map { segement -> DatoIntervallEntitet(fom = segement.fom, tom = segement.tom) }
            }.flatten().slåSammenSammenhengendePerioder()
        ).map { MånedPeriode(fom = it.fom!!.toYearMonth(), tom = it.tom!!.toYearMonth()) }
    }

    private fun kombinerTidslinjerForÅLageBarnasPerioderMedRettPåSmåbarnstillegg(
        sammenlagtTidslinje: LocalDateTimeline<List<BarnSinRettTilSmåbarnstilleggKombinator>>,
        tidslinje: LocalDateTimeline<List<BarnSinRettTilSmåbarnstilleggKombinator>>
    ): LocalDateTimeline<List<BarnSinRettTilSmåbarnstilleggKombinator>> {
        val sammenlagt =
            sammenlagtTidslinje.combine(
                tidslinje,
                StandardCombinators::bothValues,
                LocalDateTimeline.JoinStyle.CROSS_JOIN
            ) as LocalDateTimeline<List<List<BarnSinRettTilSmåbarnstilleggKombinator>>>

        return LocalDateTimeline(
            sammenlagt.toSegments().map {
                LocalDateSegment(it.fom, it.tom, it.value.flatten())
            }
        )
    }

    private fun kombinerTidslinjerForÅLageSmåbarnstilleggAndeler(
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

    enum class BarnSinRettTilSmåbarnstilleggKombinator {
        UTBETALING,
        UNDER_3_ÅR
    }
}
