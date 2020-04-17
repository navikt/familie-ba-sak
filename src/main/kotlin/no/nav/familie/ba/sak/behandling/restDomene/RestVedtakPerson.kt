package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.vedtak.AndelTilkjentYtelse
import no.nav.familie.ba.sak.behandling.vedtak.Ytelsetype
import java.time.LocalDate

data class RestVedtakPerson(
        val personIdent: String?,
        val beløp: Int,
        val stønadFom: LocalDate,
        val ytelsePerioder : List<RestYtelsePeriode>
)

data class RestYtelsePeriode (
        val beløp: Int,
        val stønadFom: LocalDate,
        val stønadTom: LocalDate,
        val type: Ytelsetype
)

fun lagRestVedtakPerson(andelerTilkjentYtelse: List<AndelTilkjentYtelse>, personopplysningGrunnlag: PersonopplysningGrunnlag?)
        : List<RestVedtakPerson>{

    return andelerTilkjentYtelse.groupBy { it.personId }
            .map {
                RestVedtakPerson(
                        personIdent = personopplysningGrunnlag?.personer?.find { person -> person.id == it.key }?.personIdent?.ident,
                        beløp = it.value.map { it.beløp }.sum(),
                        stønadFom = it.value.map { it.stønadFom }.min() ?: LocalDate.MIN,
                        ytelsePerioder = it.value.map { it1->RestYtelsePeriode(it1.beløp, it1.stønadFom, it1.stønadTom, it1.type) }
                )
            }
}
