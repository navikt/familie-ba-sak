package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.brevBegrunnelseProdusent

import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.GrunnlagForPerson
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.GrunnlagForPersonVilkårInnvilget

sealed interface BegrunnelseGrunnlag

data class BegrunnelseGrunnlagMedVerdiIDennePerioden(
    val grunnlagForVedtaksperiode: GrunnlagForPerson,
    val grunnlagForForrigeVedtaksperiode: GrunnlagForPerson?,
) : BegrunnelseGrunnlag

data class BegrunnelseGrunnlagIngenVerdiIDennePerioden(
    val erInnvilgetForrigeBehandling: Boolean,
) : BegrunnelseGrunnlag

fun lagBegrunnelseGrunnlag(
    dennePerioden: GrunnlagForPerson?,
    forrigePeriode: GrunnlagForPerson?,
    sammePeriodeForrigeBehandling: GrunnlagForPerson?,
) = if (dennePerioden == null) {
    BegrunnelseGrunnlagIngenVerdiIDennePerioden(
        erInnvilgetForrigeBehandling = sammePeriodeForrigeBehandling is GrunnlagForPersonVilkårInnvilget,
    )
} else {
    BegrunnelseGrunnlagMedVerdiIDennePerioden(
        grunnlagForVedtaksperiode = dennePerioden,
        grunnlagForForrigeVedtaksperiode = forrigePeriode,
    )
}
