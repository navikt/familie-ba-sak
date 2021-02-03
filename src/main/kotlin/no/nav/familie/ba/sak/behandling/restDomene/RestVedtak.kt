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
        @Deprecated("Bruk begrunnelser")
        val utbetalingBegrunnelser: List<RestVedtakBegrunnelse>,
        val begrunnelser: List<RestVedtakBegrunnelse>,
        val id: Long
)

data class RestVedtakBegrunnelse(
        val id: Long?,
        val fom: LocalDate,
        val tom: LocalDate,
        @Deprecated("Bruk begrunnelse")
        var vedtakBegrunnelse: VedtakBegrunnelse?,
        val begrunnelseType: VedtakBegrunnelseType?,
        var begrunnelse: VedtakBegrunnelse?,
        val opprettetTidspunkt: LocalDateTime
)

@Deprecated("Endring på begrunnelse er ikke tillatt lenger")
data class RestPutUtbetalingBegrunnelse(
        val vedtakBegrunnelseType: VedtakBegrunnelseType?,
        val vedtakBegrunnelse: VedtakBegrunnelse?
)

data class RestPostVedtakBegrunnelse(
        val fom: LocalDate,
        val tom: LocalDate,
        val vedtakBegrunnelse: VedtakBegrunnelse
)

fun RestPostVedtakBegrunnelse.tilUtbetalingBegrunnelse(vedtak: Vedtak, brevBegrunnelse: String) =
        UtbetalingBegrunnelse(vedtak = vedtak,
                              fom = this.fom,
                              tom = this.tom,
                              begrunnelseType = this.vedtakBegrunnelse.vedtakBegrunnelseType,
                              vedtakBegrunnelse = this.vedtakBegrunnelse,
                              brevBegrunnelse = brevBegrunnelse)

data class RestVedtakBegrunnelseTilknyttetVilkår(
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
                    it.tilRestVedtakBegrunnelse()
                }.sortedBy { it.opprettetTidspunkt },
                begrunnelser = this.utbetalingBegrunnelser.map {
                    it.tilRestVedtakBegrunnelse()
                }.sortedBy { it.opprettetTidspunkt }
        )

fun UtbetalingBegrunnelse.tilRestVedtakBegrunnelse() =
        RestVedtakBegrunnelse(
                id = this.id,
                fom = this.fom,
                tom = this.tom,
                begrunnelseType = this.vedtakBegrunnelse?.vedtakBegrunnelseType,
                vedtakBegrunnelse = this.vedtakBegrunnelse,
                begrunnelse = this.vedtakBegrunnelse,
                opprettetTidspunkt = this.opprettetTidspunkt
        )

