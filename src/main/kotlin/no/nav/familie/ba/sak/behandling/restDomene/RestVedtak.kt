package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.vedtak.StønadBrevBegrunnelse
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelse
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
        val resultat: BehandlingResultatType?,
        var begrunnelse: VedtakBegrunnelse?
)

data class RestVedtakBegrunnelse(
        val id: VedtakBegrunnelse,
        val navn: String
)

// TODO fix begrunnelse
fun Vedtak.toRestVedtak(restVedtakPerson: List<RestVedtakPerson>) = RestVedtak(
        aktiv = this.aktiv,
        personBeregninger = restVedtakPerson,
        vedtaksdato = this.vedtaksdato,
        id = this.id,
        stønadBrevBegrunnelser = this.stønadBrevBegrunnelser.map { begrunnelse ->
            RestStønadBrevBegrunnelse(id = begrunnelse.id,
                                      fom = begrunnelse.fom,
                                      tom = begrunnelse.tom,
                                      begrunnelse = null, //begrunnelse.begrunnelse,
                                      resultat = begrunnelse.resultat)
        }
)

fun StønadBrevBegrunnelse.toRestStønadBrevBegrunnelse() =
        RestStønadBrevBegrunnelse(
                id = this.id,
                fom = this.fom,
                tom = this.tom,
                resultat = this.resultat,
                begrunnelse = null //this.begrunnelse
        )

