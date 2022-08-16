package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.Utils.avrundetHeltallAvProsent
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
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.tilSeparateTidslinjerForBarna
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNull
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
        barnasAktørerOgFødselsdatoer: List<Pair<Aktør, LocalDate>>,
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
            søkersAndeler.map {
                LocalDateSegment(
                    it.stønadFom.førsteDagIInneværendeMåned(),
                    it.stønadTom.sisteDagIInneværendeMåned(),
                    if (it.prosent == BigDecimal.ZERO) listOf(SmåbarnstilleggKombinator.UTVIDET_UTEN_UTBETALING) else listOf(SmåbarnstilleggKombinator.UTVIDET_MED_UTBETALING)
                )
            }
        )

        val perioderMedBarnSomGirRettTilSmåbarnstillegg = LocalDateTimeline(
            lagPerioderMedBarnSomGirRettTilSmåbarnstilleggGammel(
                barnasAktørOgFødselsdatoer = barnasAktørerOgFødselsdatoer,
                barnasAndeler = barnasAndeler
            ).map {
                LocalDateSegment(
                    it.fom.førsteDagIInneværendeMåned(),
                    it.tom.sisteDagIInneværendeMåned(),
                    listOf(SmåbarnstilleggKombinator.UNDER_3_ÅR_UTBETALING)
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
                        SmåbarnstilleggKombinator.OVERGANGSSTØNAD,
                        SmåbarnstilleggKombinator.UNDER_3_ÅR_UTBETALING
                    )
                ) && (segement.value.contains(SmåbarnstilleggKombinator.UTVIDET_MED_UTBETALING) || segement.value.contains(SmåbarnstilleggKombinator.UTVIDET_UTEN_UTBETALING))
            }
            .map {
                val ordinærSatsForPeriode = SatsService.hentGyldigSatsFor(
                    satstype = SatsType.SMA,
                    stønadFraOgMed = it.fom.toYearMonth(),
                    stønadTilOgMed = it.tom.toYearMonth()
                )
                    .singleOrNull()?.sats
                    ?: error("Skal finnes én ordinær sats for gitt segment oppdelt basert på andeler")

                val prosentForPeriode = if (it.value.contains(SmåbarnstilleggKombinator.UTVIDET_MED_UTBETALING)) BigDecimal(100) else BigDecimal.ZERO
                val nasjonaltPeriodebeløp = ordinærSatsForPeriode.avrundetHeltallAvProsent(prosentForPeriode)
                AndelTilkjentYtelse(
                    behandlingId = behandlingId,
                    tilkjentYtelse = tilkjentYtelse,
                    aktør = søkerAktør
                        ?: error("Genererer andeler for småbarnstillegg uten noen perioder med full overgangsstønad"),
                    stønadFom = it.fom.toYearMonth(),
                    stønadTom = it.tom.toYearMonth(),
                    kalkulertUtbetalingsbeløp = nasjonaltPeriodebeløp,
                    nasjonaltPeriodebeløp = nasjonaltPeriodebeløp,
                    type = YtelseType.SMÅBARNSTILLEGG,
                    sats = ordinærSatsForPeriode,
                    prosent = prosentForPeriode
                )
            }
    }

    fun lagPerioderMedBarnSomGirRettTilSmåbarnstilleggGammel(
        barnasAktørOgFødselsdatoer: List<Pair<Aktør, LocalDate>>,
        barnasAndeler: List<AndelTilkjentYtelse>
    ): List<MånedPeriode> {
        return slåSammenOverlappendePerioder(
            barnasAktørOgFødselsdatoer.map { (aktør, fødselsdato) ->
                val barnetsMånedPeriodeAndeler = LocalDateTimeline(
                    barnasAndeler
                        .filter { andel -> andel.aktør == aktør }
                        .map { andel ->
                            LocalDateSegment(
                                andel.stønadFom.førsteDagIInneværendeMåned(),
                                andel.stønadTom.sisteDagIInneværendeMåned(),
                                if (andel.prosent == BigDecimal.ZERO) listOf(BarnSinRettTilSmåbarnstilleggKombinator.UTBETALING_OVERSTYRT_TIL_NULL) else listOf(BarnSinRettTilSmåbarnstilleggKombinator.UTBETALING)
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

    private fun kombinerBarnasTidslinjerTilUnder3ÅrResultat(
        alleResultater: Iterable<BarnSinRettTilSmåbarnstilleggKombinator>
    ): SmåbarnstilleggKombinator? {
        val barnMedUtbetaling = alleResultater.filter { it == BarnSinRettTilSmåbarnstilleggKombinator.UTBETALING }
        val barnMedNullutbetaling = alleResultater.filter { it == BarnSinRettTilSmåbarnstilleggKombinator.UTBETALING_OVERSTYRT_TIL_NULL }

        return when {
            barnMedUtbetaling.isNotEmpty() -> SmåbarnstilleggKombinator.UNDER_3_ÅR_UTBETALING
            barnMedNullutbetaling.isNotEmpty() -> SmåbarnstilleggKombinator.UNDER_3_ÅR_NULLUTBETALING
            else -> null
        }
    }

    private fun lagPerioderMedBarnSomGirRettTilSmåbarnstillegg(barnasAndeler: List<AndelTilkjentYtelse>, barnasAktørOgFødselsdatoer: List<Pair<Aktør, LocalDate>>,) {
        val barnasTidslinjer = barnasAndeler.tilSeparateTidslinjerForBarna() // Må kopiere denne funksjonen, ligger i eøs-mappen

        val barnasKombinerteTidslinjer = barnasTidslinjer.map { (aktør, tidslinje) ->
            val fødselsdato = barnasAktørOgFødselsdatoer.find { it.first == aktør }?.second

            val under3ÅrTidslinje = Under3ÅrTidslinje(under3ÅrPerioder = listOf(Under3ÅrPeriode(fom = fødselsdato!!, tom = fødselsdato.plusYears(18))))

            val kombinertTidslinje = tidslinje.kombinerMed(under3ÅrTidslinje) {
                barnetsAndelerTidslinje, barnetsUnder3ÅrTidslinje ->
                if (barnetsUnder3ÅrTidslinje == null || barnetsAndelerTidslinje == null) null
                else if (barnetsAndelerTidslinje.prosent > BigDecimal.ZERO) BarnSinRettTilSmåbarnstilleggKombinator.UTBETALING
                else BarnSinRettTilSmåbarnstilleggKombinator.UTBETALING_OVERSTYRT_TIL_NULL
            }

            kombinertTidslinje
        }

        barnasKombinerteTidslinjer.kombinerUtenNull { kombinerBarnasTidslinjerTilUnder3ÅrResultat(it) }
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
        UTVIDET_MED_UTBETALING,
        UTVIDET_UTEN_UTBETALING,
        UNDER_3_ÅR_UTBETALING,
        UNDER_3_ÅR_NULLUTBETALING
    }

    enum class BarnSinRettTilSmåbarnstilleggKombinator {
        UTBETALING,
        UTBETALING_OVERSTYRT_TIL_NULL,
        UNDER_3_ÅR
    }
}
