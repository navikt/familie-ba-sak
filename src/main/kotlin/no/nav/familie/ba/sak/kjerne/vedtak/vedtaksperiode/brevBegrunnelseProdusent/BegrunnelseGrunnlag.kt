package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.brevBegrunnelseProdusent

sealed interface BegrunnelseGrunnlag

data class BegrunnelseGrunnlagMedVerdiIDennePerioden(
    val grunnlagForVedtaksperiode: BegrunnelseGrunnlagForPersonIPeriode,
    val grunnlagForForrigeVedtaksperiode: BegrunnelseGrunnlagForPersonIPeriode?,
) : BegrunnelseGrunnlag

data class BegrunnelseGrunnlagIngenVerdiIDennePerioden(
    val erInnvilgetForrigeBehandling: Boolean,
) : BegrunnelseGrunnlag

fun lagBegrunnelseGrunnlag(
    dennePerioden: BegrunnelseGrunnlagForPersonIPeriode?,
    forrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?,
    sammePeriodeForrigeBehandling: BegrunnelseGrunnlagForPersonIPeriode?,
) = if (dennePerioden == null) {
    BegrunnelseGrunnlagIngenVerdiIDennePerioden(
        // Setter Denne til false midlertidig
        erInnvilgetForrigeBehandling = false,
    )
} else {
    BegrunnelseGrunnlagMedVerdiIDennePerioden(
        grunnlagForVedtaksperiode = dennePerioden,
        grunnlagForForrigeVedtaksperiode = forrigePeriode,
    )
}
