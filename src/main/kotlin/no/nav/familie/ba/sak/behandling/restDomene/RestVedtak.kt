package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.vedtak.UtbetalingBegrunnelse
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelse
import java.time.LocalDate

data class RestVedtak(
        val aktiv: Boolean,
        val vedtaksdato: LocalDate?,
        val personBeregninger: List<RestVedtakPerson>,
        val utbetalingBegrunnelser: List<RestUtbetalingBegrunnelse>,
        val id: Long
)

data class RestUtbetalingBegrunnelse(
        val id: Long?,
        val fom: LocalDate,
        val tom: LocalDate,
        val resultat: BehandlingResultatType?,
        var vedtakBegrunnelse: VedtakBegrunnelse?
)

data class RestPutUtbetalingBegrunnelse(
        val resultat: BehandlingResultatType?,
        val vedtakBegrunnelse: VedtakBegrunnelse?
)

data class RestVedtakBegrunnelse(
        val id: VedtakBegrunnelse,
        val navn: String
)

fun Vedtak.toRestVedtak(restVedtakPerson: List<RestVedtakPerson>) = RestVedtak(
        aktiv = this.aktiv,
        personBeregninger = restVedtakPerson,
        vedtaksdato = this.vedtaksdato,
        id = this.id,
        utbetalingBegrunnelser = this.utbetalingBegrunnelser.map { utbetalingBegrunnelse ->
            RestUtbetalingBegrunnelse(id = utbetalingBegrunnelse.id,
                                      fom = utbetalingBegrunnelse.fom,
                                      tom = utbetalingBegrunnelse.tom,
                                      vedtakBegrunnelse = utbetalingBegrunnelse.vedtakBegrunnelse,
                                      resultat = utbetalingBegrunnelse.resultat)
        }
)

fun UtbetalingBegrunnelse.toRestUtbetalingBegrunnelse() =
        RestUtbetalingBegrunnelse(
                id = this.id,
                fom = this.fom,
                tom = this.tom,
                resultat = this.resultat,
                vedtakBegrunnelse = this.vedtakBegrunnelse
        )

