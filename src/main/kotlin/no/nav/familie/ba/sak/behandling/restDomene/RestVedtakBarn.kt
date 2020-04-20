package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.vedtak.AndelTilkjentYtelse
import no.nav.familie.ba.sak.behandling.vedtak.YtelseType
import java.time.LocalDate

data class RestVedtakBarn(
        val barn: String?,
        val beløp: Int,
        val stønadFom: LocalDate,
        val ytelsePerioder : List<RestYtelsePeriode>
)

data class RestYtelsePeriode (
        val beløp: Int,
        val stønadFom: LocalDate,
        val stønadTom: LocalDate,
        val type: YtelseType
)

fun lagRestVedtakBarn(andelerTilkjentYtelse: List<AndelTilkjentYtelse>, personopplysningGrunnlag: PersonopplysningGrunnlag?)
        : List<RestVedtakBarn>{

    val idBarnMap = personopplysningGrunnlag?.barna?.associateBy { it.id }
    return andelerTilkjentYtelse.groupBy { it.personId }
            .map {
                RestVedtakBarn(
                        barn = idBarnMap?.get(it.key)?.personIdent?.ident,
                        beløp = it.value.map { it.beløp }.sum(),
                        stønadFom = it.value.map { it.stønadFom }.min() ?: LocalDate.MIN,
                        ytelsePerioder = it.value.map { it1->RestYtelsePeriode(it1.beløp, it1.stønadFom, it1.stønadTom, it1.type) }
                )
            }
}
