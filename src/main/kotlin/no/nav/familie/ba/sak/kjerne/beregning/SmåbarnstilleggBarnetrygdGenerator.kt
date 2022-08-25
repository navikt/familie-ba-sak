package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.Utils.avrundetHeltallAvProsent
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.slåSammenOverlappendePerioder
import no.nav.familie.ba.sak.common.slåSammenSammenhengendePerioder
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønadTidslinje
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
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
        perioderMedFullOvergangsstønad: List<InternPeriodeOvergangsstønad>,
        utvidetAndeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        barnasAndeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        barnasAktørerOgFødselsdatoer: List<Pair<Aktør, LocalDate>>
    ): List<AndelTilkjentYtelseMedEndreteUtbetalinger> {
        if (perioderMedFullOvergangsstønad.isEmpty() || utvidetAndeler.isEmpty() || barnasAndeler.isEmpty()) return emptyList()

        validerUtvidetOgBarnasAndeler(utvidetAndeler = utvidetAndeler, barnasAndeler = barnasAndeler)

        val søkerAktør = utvidetAndeler.first().aktør

        val perioderMedFullOvergangsstønadTidslinje =
            InternPeriodeOvergangsstønadTidslinje(perioderMedFullOvergangsstønad)

        val utvidetBarnetrygdTidslinje = AndelTilkjentYtelseTidslinje(andelerTilkjentYtelse = utvidetAndeler)

        val barnSomGirRettTilSmåbarnstilleggTidslinje = lagTidslinjeForPerioderMedBarnSomGirRettTilSmåbarnstillegg(
            barnasAndeler = barnasAndeler,
            barnasAktørerOgFødselsdatoer = barnasAktørerOgFødselsdatoer
        )

        val kombinertProsentTidslinje = kombinerAlleTidslinjerTilProsentTidslinje(
            perioderMedFullOvergangsstønadTidslinje,
            utvidetBarnetrygdTidslinje,
            barnSomGirRettTilSmåbarnstilleggTidslinje
        )

        return kombinertProsentTidslinje.filtrerIkkeNull().lagSmåbarnstilleggAndeler(
            søkerAktør = søkerAktør
        )
    }

    fun lagSmåbarnstilleggAndelerGammel(
        perioderMedFullOvergangsstønad: List<InternPeriodeOvergangsstønad>,
        andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        barnasAktørerOgFødselsdatoer: List<Pair<Aktør, LocalDate>>
    ): List<AndelTilkjentYtelseMedEndreteUtbetalinger> {
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
            lagPerioderMedBarnSomGirRettTilSmåbarnstilleggGammel(
                barnasAktørOgFødselsdatoer = barnasAktørerOgFødselsdatoer,
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
                val aty = AndelTilkjentYtelse(
                    behandlingId = behandlingId,
                    tilkjentYtelse = tilkjentYtelse,
                    aktør = søkerAktør
                        ?: error("Genererer andeler for småbarnstillegg uten noen perioder med full overgangsstønad"),
                    stønadFom = it.fom.toYearMonth(),
                    stønadTom = it.tom.toYearMonth(),
                    kalkulertUtbetalingsbeløp = ordinærSatsForPeriode,
                    nasjonaltPeriodebeløp = ordinærSatsForPeriode,
                    type = YtelseType.SMÅBARNSTILLEGG,
                    sats = ordinærSatsForPeriode,
                    prosent = BigDecimal(100)
                )

                AndelTilkjentYtelseMedEndreteUtbetalinger(aty, emptyList(), null)
            }
    }

    private fun Tidslinje<SmåbarnstilleggPeriode, Måned>.lagSmåbarnstilleggAndeler(
        søkerAktør: Aktør
    ): List<AndelTilkjentYtelseMedEndreteUtbetalinger> {
        return this.perioder().map {
            val stønadFom = it.fraOgMed.tilYearMonth()
            val stønadTom = it.tilOgMed.tilYearMonth()

            val ordinærSatsForPeriode = SatsService.hentGyldigSatsFor(
                satstype = SatsType.SMA,
                stønadFraOgMed = stønadFom,
                stønadTilOgMed = stønadTom
            ).singleOrNull()?.sats
                ?: throw Feil("Skal finnes én ordinær sats for gitt segment oppdelt basert på andeler")

            val prosentIPeriode = it.innhold?.prosent ?: throw Feil("Skal finnes prosent for gitt periode")

            val beløpIPeriode = ordinærSatsForPeriode.avrundetHeltallAvProsent(prosent = prosentIPeriode)

            val aty = AndelTilkjentYtelse(
                behandlingId = behandlingId,
                tilkjentYtelse = tilkjentYtelse,
                aktør = søkerAktør,
                stønadFom = stønadFom,
                stønadTom = stønadTom,
                kalkulertUtbetalingsbeløp = beløpIPeriode,
                nasjonaltPeriodebeløp = beløpIPeriode,
                type = YtelseType.SMÅBARNSTILLEGG,
                sats = ordinærSatsForPeriode,
                prosent = prosentIPeriode
            )

            AndelTilkjentYtelseMedEndreteUtbetalinger(aty, emptyList(), null)
        }
    }

    fun lagPerioderMedBarnSomGirRettTilSmåbarnstilleggGammel(
        barnasAktørOgFødselsdatoer: List<Pair<Aktør, LocalDate>>,
        barnasAndeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>
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
                                listOf(BarnSinRettTilSmåbarnstilleggKombinatorGammel.UTBETALING)
                            )
                        }
                )

                val barnetsUnder3ÅrTidslinje = LocalDateTimeline(
                    listOf(
                        LocalDateSegment(
                            fødselsdato.førsteDagIInneværendeMåned(),
                            fødselsdato.plusYears(3).sisteDagIMåned(),
                            listOf(BarnSinRettTilSmåbarnstilleggKombinatorGammel.UNDER_3_ÅR)
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
                                BarnSinRettTilSmåbarnstilleggKombinatorGammel.UTBETALING,
                                BarnSinRettTilSmåbarnstilleggKombinatorGammel.UNDER_3_ÅR
                            )
                        )
                    }.map { segement -> DatoIntervallEntitet(fom = segement.fom, tom = segement.tom) }
            }.flatten().slåSammenSammenhengendePerioder()
        ).map { MånedPeriode(fom = it.fom!!.toYearMonth(), tom = it.tom!!.toYearMonth()) }
    }

    private fun kombinerTidslinjerForÅLageBarnasPerioderMedRettPåSmåbarnstillegg(
        sammenlagtTidslinje: LocalDateTimeline<List<BarnSinRettTilSmåbarnstilleggKombinatorGammel>>,
        tidslinje: LocalDateTimeline<List<BarnSinRettTilSmåbarnstilleggKombinatorGammel>>
    ): LocalDateTimeline<List<BarnSinRettTilSmåbarnstilleggKombinatorGammel>> {
        val sammenlagt =
            sammenlagtTidslinje.combine(
                tidslinje,
                StandardCombinators::bothValues,
                LocalDateTimeline.JoinStyle.CROSS_JOIN
            ) as LocalDateTimeline<List<List<BarnSinRettTilSmåbarnstilleggKombinatorGammel>>>

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

    enum class BarnSinRettTilSmåbarnstilleggKombinatorGammel {
        UTBETALING,
        UNDER_3_ÅR
    }
}
