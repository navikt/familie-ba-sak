package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.common.toYearMonth
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

fun PersonopplysningGrunnlag.tilRestPersonerMedAndeler(andelerKnyttetTilPersoner: List<AndelTilkjentYtelse>)
        : List<RestPersonMedAndeler> =
        andelerKnyttetTilPersoner
                .groupBy { it.personIdent }
                .map { andelerForPerson ->
                    val personId = andelerForPerson.key
                    val andeler = andelerForPerson.value
                    RestPersonMedAndeler(
                            personIdent = this.personer.find { person -> person.personIdent.ident == personId }?.personIdent?.ident,
                            beløp = andeler.map { it.beløp }.sum(),
                            stønadFom = andeler.map { it.stønadFom }.minOrNull() ?: LocalDate.MIN.toYearMonth(),
                            stønadTom = andeler.map { it.stønadTom }.maxOrNull() ?: LocalDate.MAX.toYearMonth(),
                            ytelsePerioder = andeler.map { it1 ->
                                RestYtelsePeriode(it1.beløp,
                                                  it1.stønadFom,
                                                  it1.stønadTom,
                                                  it1.type)
                            }
                    )
                }