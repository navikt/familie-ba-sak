package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.slåSammenBack2BackAndelsperioderMedSammeBeløp
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import java.time.LocalDate
import java.time.YearMonth

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
    val ytelseType: YtelseType
)

fun PersonopplysningGrunnlag.tilRestPersonerMedAndeler(andelerKnyttetTilPersoner: List<AndelTilkjentYtelse>): List<RestPersonMedAndeler> =
    andelerKnyttetTilPersoner
        .groupBy { it.personIdent }
        .map { andelerForPerson ->
            val personId = andelerForPerson.key
            val andeler = andelerForPerson.value

            val sammenslåtteAndeler =
                andeler.groupBy { it.type }.flatMap { it.value.slåSammenBack2BackAndelsperioderMedSammeBeløp() }

            RestPersonMedAndeler(
                personIdent = this.personer.find { person -> person.personIdent.ident == personId }?.personIdent?.ident,
                beløp = sammenslåtteAndeler.sumOf { it.kalkulertUtbetalingsbeløp },
                stønadFom = sammenslåtteAndeler.map { it.stønadFom }.minOrNull() ?: LocalDate.MIN.toYearMonth(),
                stønadTom = sammenslåtteAndeler.map { it.stønadTom }.maxOrNull() ?: LocalDate.MAX.toYearMonth(),
                ytelsePerioder = sammenslåtteAndeler.map { it1 ->
                    RestYtelsePeriode(
                        it1.kalkulertUtbetalingsbeløp,
                        it1.stønadFom,
                        it1.stønadTom,
                        it1.type
                    )
                }
            )
        }
