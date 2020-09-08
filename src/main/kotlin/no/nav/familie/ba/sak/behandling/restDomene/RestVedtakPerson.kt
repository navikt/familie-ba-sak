package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import java.time.LocalDate

data class RestVedtakPerson(
        val personIdent: String?,
        val beløp: Int,
        val stønadFom: LocalDate,
        val ytelsePerioder: List<RestYtelsePeriode>
)

data class RestYtelsePeriode(
        val beløp: Int,
        val stønadFom: LocalDate,
        val stønadTom: LocalDate,
        val ytelseType: YtelseType
)

fun lagRestVedtakPerson(andelerTilkjentYtelse: List<AndelTilkjentYtelse>, personopplysningGrunnlag: PersonopplysningGrunnlag?)
        : List<RestVedtakPerson> {

    return andelerTilkjentYtelse.groupBy { it.personIdent }
            .map { andelerForPerson ->
                val personId = andelerForPerson.key
                val andeler = andelerForPerson.value
                RestVedtakPerson(
                        personIdent = personopplysningGrunnlag?.personer?.find { person -> person.personIdent.ident == personId }?.personIdent?.ident,
                        beløp = andeler.map { it.beløp }.sum(),
                        stønadFom = andeler.map { it.stønadFom }.minOrNull() ?: LocalDate.MIN,
                        ytelsePerioder = andeler.map { it1 ->
                            RestYtelsePeriode(it1.beløp,
                                              it1.stønadFom,
                                              it1.stønadTom,
                                              it1.type)
                        }
                )
            }
}