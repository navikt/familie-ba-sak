package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.RestYtelsePeriodeTidslinje
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.slåSammenBack2BackAndelsperioderMedSammeBeløp
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.beregning.KompetanseTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.Tidslinjer
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.snittKombinerMed
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import java.time.LocalDate
import java.time.YearMonth

data class BeregningOppsummering(
    val regelverk: Regelverk? = null,
    val status: BeregningOppsummeringStatus? = null,
    val kompetentLand: KompetanseResultat? = null,
)

enum class BeregningOppsummeringStatus {
    VURDERT,
    IKKE_VURDERT
}

data class RestPersonMedAndeler(
    val personIdent: String?,
    val beløp: Int,
    val stønadFom: YearMonth,
    val stønadTom: YearMonth,
    val ytelsePerioder: List<RestYtelsePeriode>
)

data class RestYtelsePeriode(
    val beløp: Int,
    val stønadFom: YearMonth,
    val stønadTom: YearMonth,
    val ytelseType: YtelseType,
    val beregningOppsummering: BeregningOppsummering
)

fun PersonopplysningGrunnlag.tilRestPersonerMedAndeler(
    andelerKnyttetTilPersoner: List<AndelTilkjentYtelse>,
    tidslinjer: Tidslinjer?,
    kompetanser: List<Kompetanse>
): List<RestPersonMedAndeler> =
    andelerKnyttetTilPersoner
        .groupBy { it.aktør }
        .map { (aktør, andeler) ->

            val sammenslåtteAndeler =
                andeler.groupBy { it.type }.flatMap { it.value.slåSammenBack2BackAndelsperioderMedSammeBeløp() }

            // Støtter kun for barn enn så lenge
            val barnetsTidslinjer = tidslinjer?.barnasTidslinjer()?.get(aktør)
            val kompetanserPåBarn = kompetanser.filter { it.barnAktører.contains(aktør) }

            RestPersonMedAndeler(
                personIdent = this.søkerOgBarn.find { person -> person.aktør == aktør }?.aktør?.aktivFødselsnummer(),
                beløp = sammenslåtteAndeler.sumOf { it.kalkulertUtbetalingsbeløp },
                stønadFom = sammenslåtteAndeler.map { it.stønadFom }.minOrNull() ?: LocalDate.MIN.toYearMonth(),
                stønadTom = sammenslåtteAndeler.map { it.stønadTom }.maxOrNull() ?: LocalDate.MAX.toYearMonth(),
                ytelsePerioder = sammenslåtteAndeler.tilRestYtelsePerioder(barnetsTidslinjer, kompetanserPåBarn)
            )
        }

fun List<AndelTilkjentYtelse>.tilRestYtelsePerioder(
    barnetsTidslinjer: Tidslinjer.BarnetsTidslinjer?,
    kompetanser: List<Kompetanse>
): List<RestYtelsePeriode> {
    val initielleRestYtelsePerioder = this.map {
        RestYtelsePeriode(
            beløp = it.kalkulertUtbetalingsbeløp,
            stønadFom = it.stønadFom,
            stønadTom = it.stønadTom,
            ytelseType = it.type,
            beregningOppsummering = BeregningOppsummering(),
        )
    }

    if (barnetsTidslinjer == null) throw Feil("Tidslinjer mangler for barn")

    val restYtelseTidslinjeMedRegelverk = RestYtelsePeriodeTidslinje(initielleRestYtelsePerioder)
        .snittKombinerMed(barnetsTidslinjer.regelverkTidslinje) { restYtelsePeriode, regelverk, ->
            when {
                regelverk == null -> restYtelsePeriode
                restYtelsePeriode == null -> null
                else -> restYtelsePeriode.copy(
                    beregningOppsummering = restYtelsePeriode.beregningOppsummering.copy(regelverk = regelverk)
                )
            }
        }

    val restYtelseTidslinjeMedRegelverkOgKompetentLand =
        restYtelseTidslinjeMedRegelverk.snittKombinerMed(KompetanseTidslinje(kompetanser)) { restYtelsePeriode, kompetanse, ->
            when {
                kompetanse == null -> restYtelsePeriode
                restYtelsePeriode == null -> null
                else -> restYtelsePeriode.copy(
                    beregningOppsummering = restYtelsePeriode.beregningOppsummering.copy(kompetentLand = kompetanse.resultat)
                )
            }
        }

    return restYtelseTidslinjeMedRegelverkOgKompetentLand
        .perioder()
        .mapNotNull { it.innhold }
}
