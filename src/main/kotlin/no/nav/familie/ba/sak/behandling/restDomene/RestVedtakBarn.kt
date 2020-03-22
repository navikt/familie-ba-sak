package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.vedtak.VedtakPerson
import no.nav.familie.ba.sak.behandling.vedtak.Ytelsetype
import java.time.LocalDate

data class RestVedtakBarn(
        val barn: String?,
        val beløp: Int,
        val stønadFom: LocalDate,
        val stønadTom: LocalDate,
        val type: Ytelsetype
)

fun VedtakPerson.toRestVedtakBarn(personopplysningGrunnlag: PersonopplysningGrunnlag?) : RestVedtakBarn {

    val idBarnMap = personopplysningGrunnlag?.barna?.associateBy { it.id }

    return RestVedtakBarn(
        barn = idBarnMap?.get(this.personId)?.personIdent?.ident,
        beløp = this.beløp, // TODO endre her til å støtte liste
        stønadFom = this.stønadFom,
        stønadTom = this.stønadTom,
        type = this.type
    )
}