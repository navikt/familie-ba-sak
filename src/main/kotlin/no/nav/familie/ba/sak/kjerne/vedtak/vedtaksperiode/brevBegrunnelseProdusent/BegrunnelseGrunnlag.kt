package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.brevBegrunnelseProdusent

import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.VedtaksperiodeGrunnlagForPerson
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.VedtaksperiodeGrunnlagForPersonVilkårInnvilget

sealed interface BegrunnelseGrunnlag

data class BegrunnelseGrunnlagMedVerdiIDennePerioden(
    val grunnlagForVedtaksperiode: VedtaksperiodeGrunnlagForPerson,
    val grunnlagForForrigeVedtaksperiode: VedtaksperiodeGrunnlagForPerson?,
) : BegrunnelseGrunnlag

data class BegrunnelseGrunnlagIngenVerdiIDennePerioden(
    val erInnvilgetForrigeBehandling: Boolean,
) : BegrunnelseGrunnlag

fun lagBegrunnelseGrunnlag(
    dennePerioden: VedtaksperiodeGrunnlagForPerson?,
    forrigePeriode: VedtaksperiodeGrunnlagForPerson?,
    sammePeriodeForrigeBehandling: VedtaksperiodeGrunnlagForPerson?,
) = if (dennePerioden == null) {
    BegrunnelseGrunnlagIngenVerdiIDennePerioden(
        erInnvilgetForrigeBehandling = sammePeriodeForrigeBehandling is VedtaksperiodeGrunnlagForPersonVilkårInnvilget,
    )
} else {
    BegrunnelseGrunnlagMedVerdiIDennePerioden(
        grunnlagForVedtaksperiode = dennePerioden,
        grunnlagForForrigeVedtaksperiode = forrigePeriode,
    )
}
