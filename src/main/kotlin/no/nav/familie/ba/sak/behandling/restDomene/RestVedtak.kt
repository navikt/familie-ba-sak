package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakBegrunnelse
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseType
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import java.time.LocalDate
import java.time.LocalDateTime

data class RestVedtak(
        val aktiv: Boolean,
        val vedtaksdato: LocalDateTime?,
        val vedtaksperioderMedBegrunnelser: List<RestVedtaksperiodeMedBegrunnelser>,
        val begrunnelser: List<RestVedtakBegrunnelse>,
        val avslagBegrunnelser: List<RestAvslagBegrunnelser>?,
        val id: Long
)

data class RestVedtakBegrunnelse(
        val id: Long?,
        val fom: LocalDate?,
        val tom: LocalDate?,
        val begrunnelseType: VedtakBegrunnelseType?,
        var begrunnelse: VedtakBegrunnelseSpesifikasjon?,
        val brevBegrunnelse: String?,
        val opprettetTidspunkt: LocalDateTime
)

data class RestPostVedtakBegrunnelse(
        val fom: LocalDate,
        val tom: LocalDate?,
        val vedtakBegrunnelse: VedtakBegrunnelseSpesifikasjon
)

data class RestPostFritekstVedtakBegrunnelser(
        val fom: LocalDate?,
        val tom: LocalDate?,
        val fritekster: List<String>,
        val vedtaksperiodetype: Vedtaksperiodetype
)

data class RestDeleteVedtakBegrunnelser(
        val fom: LocalDate,
        val tom: LocalDate?,
        val vedtakbegrunnelseTyper: List<VedtakBegrunnelseType>
)

fun RestPostVedtakBegrunnelse.tilVedtakBegrunnelse(vedtak: Vedtak, brevBegrunnelse: String) =
        VedtakBegrunnelse(vedtak = vedtak,
                          fom = fom,
                          tom = tom,
                          begrunnelse = vedtakBegrunnelse,
                          brevBegrunnelse = brevBegrunnelse)

data class RestVedtakBegrunnelseTilknyttetVilkår(
        val id: VedtakBegrunnelseSpesifikasjon,
        val navn: String,
        val vilkår: Vilkår?
)

fun Vedtak.tilRestVedtak(avslagBegrunnelser: List<RestAvslagBegrunnelser>,
                         vedtaksperioderMedBegrunnelser: List<RestVedtaksperiodeMedBegrunnelser>) =
        RestVedtak(
                aktiv = this.aktiv,
                vedtaksdato = this.vedtaksdato,
                id = this.id,
                vedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser,
                begrunnelser = this.vedtakBegrunnelser.map {
                    it.tilRestVedtakBegrunnelse()
                }.sortedBy { it.opprettetTidspunkt },
                avslagBegrunnelser = avslagBegrunnelser,
        )

fun VedtakBegrunnelse.tilRestVedtakBegrunnelse() =
        RestVedtakBegrunnelse(
                id = this.id,
                fom = this.fom,
                tom = this.tom,
                begrunnelseType = this.begrunnelse.vedtakBegrunnelseType,
                begrunnelse = this.begrunnelse,
                brevBegrunnelse = this.brevBegrunnelse,
                opprettetTidspunkt = this.opprettetTidspunkt
        )

data class RestAvslagBegrunnelser(val fom: LocalDate?,
                                  val tom: LocalDate?,
                                  val brevBegrunnelser: List<String>)
