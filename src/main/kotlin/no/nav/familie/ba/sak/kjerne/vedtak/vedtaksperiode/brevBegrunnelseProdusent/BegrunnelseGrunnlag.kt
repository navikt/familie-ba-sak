package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.brevBegrunnelseProdusent

import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.GrunnlagForPerson
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.GrunnlagForPersonInnvilget

sealed interface BegrunnelseGrunnlag

data class BegrunnelseGrunnlagMedVerdiIDennePerioden(
    val grunnlagForVedtaksperiode: GrunnlagForPerson,
    val grunnlagForForrigeVedtaksperiode: GrunnlagForPerson?,
) : BegrunnelseGrunnlag

data class BegrunnelseGrunnlagIngenVerdiIDenneBehandlingen(
    val erInnvilgetForrigeBehandling: Boolean,
) : BegrunnelseGrunnlag

fun lagBegrunnelseGrunnlag(
    dennePerioden: GrunnlagForPerson?,
    forrigePeriode: GrunnlagForPerson?,
    sammePeriodeForrigeBehandling: GrunnlagForPerson?,
) = if (dennePerioden == null) {
    BegrunnelseGrunnlagIngenVerdiIDenneBehandlingen(
        erInnvilgetForrigeBehandling = sammePeriodeForrigeBehandling is GrunnlagForPersonInnvilget,
    )
} else {
    BegrunnelseGrunnlagMedVerdiIDennePerioden(
        grunnlagForVedtaksperiode = dennePerioden,
        grunnlagForForrigeVedtaksperiode = forrigePeriode,
    )
}
