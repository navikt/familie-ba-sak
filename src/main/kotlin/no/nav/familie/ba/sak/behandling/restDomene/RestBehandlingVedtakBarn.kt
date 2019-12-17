package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.domene.vedtak.BehandlingVedtakBarn
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import java.time.LocalDate

data class RestBehandlingVedtakBarn(
    val barn: String?,
    val beløp: Int,
    val stønadFom: LocalDate
)

fun BehandlingVedtakBarn.toRestBehandlingVedtakBarn() = RestBehandlingVedtakBarn(
    barn = this.barn.personIdent?.ident,
    beløp = this.beløp,
    stønadFom = this.stønadFom
)