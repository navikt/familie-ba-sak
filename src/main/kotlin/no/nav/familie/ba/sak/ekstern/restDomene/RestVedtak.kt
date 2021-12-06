package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.`no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.BrevPeriodeGrunnlag`
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import java.time.LocalDateTime

data class RestVedtak(
    val aktiv: Boolean,
    val vedtaksdato: LocalDateTime?,
    val vedtaksperioderMedBegrunnelser: List<`no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.BrevPeriodeGrunnlag`>,
    val id: Long
)

data class RestVedtakBegrunnelseTilknyttetVilkår(
    val id: VedtakBegrunnelseSpesifikasjon,
    val navn: String,
    val vilkår: Vilkår?
)

fun Vedtak.tilRestVedtak(vedtaksperioderMedBegrunnelser: List<`no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.BrevPeriodeGrunnlag`>) =
    RestVedtak(
        aktiv = this.aktiv,
        vedtaksdato = this.vedtaksdato,
        id = this.id,
        vedtaksperioderMedBegrunnelser = vedtaksperioderMedBegrunnelser,
    )
