package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import java.time.LocalDate
import java.time.LocalDateTime

data class RestVedtak(
        val aktiv: Boolean,
        val vedtaksdato: LocalDateTime?,
        val vedtaksperioderMedBegrunnelser: List<RestVedtaksperiodeMedBegrunnelser>,
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

data class RestVedtakBegrunnelseTilknyttetVilkår(
        val id: VedtakBegrunnelseSpesifikasjon,
        val navn: String,
        val vilkår: Vilkår?
)

fun Vedtak.tilRestVedtak(vedtaksperioderMedBegrunnelser: List<RestVedtaksperiodeMedBegrunnelser>) =
        RestVedtak(
                aktiv = this.aktiv,
                vedtaksdato = this.vedtaksdato,
                id = this.id,
                vedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser,
        )

