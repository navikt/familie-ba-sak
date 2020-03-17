package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.vedtak.VedtakPerson
import java.time.LocalDate

data class RestVedtakBarn(
        val barn: String?,
        val beløp: Int,
        val stønadFom: LocalDate
)

fun VedtakPerson.toRestVedtakBarn() = RestVedtakBarn(
        barn = this.person.personIdent.ident,
        beløp = this.ytelsePerioder.first().beløp, // TODO endre her til å støtte liste
        stønadFom = this.ytelsePerioder.first().stønadFom
)