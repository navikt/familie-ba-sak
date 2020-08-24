package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.vedtak.StønadBrevBegrunnelse
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import java.time.LocalDate

data class RestVedtak(
        val aktiv: Boolean,
        val vedtaksdato: LocalDate?,
        val personBeregninger: List<RestVedtakPerson>,
        val stønadBrevBegrunnelser: List<RestStønadBrevBegrunnelse>,
        val id: Long
)

data class RestStønadBrevBegrunnelse(
        val id: Long?,
        val fom: LocalDate,
        val tom: LocalDate,
        val begrunnelse: String,
        val årsak: String
)

fun Vedtak.toRestVedtak(restVedtakPerson: List<RestVedtakPerson>) = RestVedtak(
        aktiv = this.aktiv,
        personBeregninger = restVedtakPerson,
        vedtaksdato = this.vedtaksdato,
        id = this.id,
        stønadBrevBegrunnelser = this.stønadBrevBegrunnelser.map { begrunnelse ->
            RestStønadBrevBegrunnelse(id = begrunnelse.id,
                                      fom = begrunnelse.fom,
                                      tom = begrunnelse.tom,
                                      begrunnelse = begrunnelse.begrunnelse,
                                      årsak = begrunnelse.årsak)
        }
)

fun StønadBrevBegrunnelse.toRestStønadBrevBegrunnelse() =
        RestStønadBrevBegrunnelse(
                id = this.id,
                fom = this.fom,
                tom = this.tom,
                begrunnelse = this.begrunnelse,
                årsak = this.årsak
        )

