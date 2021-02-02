package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.vedtak.UtbetalingBegrunnelse
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelse
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseType
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import java.time.LocalDate
import java.time.LocalDateTime

data class RestVedtak(
        val aktiv: Boolean,
        val vedtaksdato: LocalDateTime?,
        val utbetalingBegrunnelser: List<RestUtbetalingBegrunnelse>,
        val id: Long
)

data class RestUtbetalingBegrunnelse(
        val id: Long?,
        val fom: LocalDate,
        val tom: LocalDate,
        val begrunnelseType: VedtakBegrunnelseType?,
        var vedtakBegrunnelse: VedtakBegrunnelse?,
        val opprettetTidspunkt: LocalDateTime
)

@Deprecated("Endring på begrunnelse er ikke tillatt lenger")
data class RestPutUtbetalingBegrunnelse(
        val vedtakBegrunnelseType: VedtakBegrunnelseType?,
        val vedtakBegrunnelse: VedtakBegrunnelse?
)

data class RestPostUtbetalingBegrunnelse(
        val fom: LocalDate,
        val tom: LocalDate,
        val vedtakBegrunnelse: VedtakBegrunnelse
)

data class RestVedtakBegrunnelse(
        val id: VedtakBegrunnelse,
        val navn: String,
        val vilkår: Vilkår?
)

fun Vedtak.tilRestVedtak() =
        RestVedtak(
                aktiv = this.aktiv,
                vedtaksdato = this.vedtaksdato,
                id = this.id,
                utbetalingBegrunnelser = this.utbetalingBegrunnelser.map {
                    it.tilRestUtbetalingBegrunnelse()
                }.sortedBy { it.opprettetTidspunkt }
        )

fun UtbetalingBegrunnelse.tilRestUtbetalingBegrunnelse() =
        RestUtbetalingBegrunnelse(
                id = this.id,
                fom = this.fom,
                tom = this.tom,
                begrunnelseType = this.begrunnelseType,
                vedtakBegrunnelse = this.vedtakBegrunnelse,
                opprettetTidspunkt = this.opprettetTidspunkt
        )

