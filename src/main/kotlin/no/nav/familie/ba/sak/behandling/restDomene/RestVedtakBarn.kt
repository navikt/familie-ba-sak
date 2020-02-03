package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.domene.vedtak.VedtakBarn
import java.time.LocalDate

data class RestVedtakBarn(
        val barn: String?,
        val beløp: Int,
        val stønadFom: LocalDate
)

fun VedtakBarn.toRestVedtakBarn() = RestVedtakBarn(
        barn = this.barn.personIdent.ident,
        beløp = this.beløp,
        stønadFom = this.stønadFom
)