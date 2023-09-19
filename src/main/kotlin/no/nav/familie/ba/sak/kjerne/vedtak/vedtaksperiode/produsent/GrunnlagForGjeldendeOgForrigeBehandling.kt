package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent

data class GrunnlagForGjeldendeOgForrigeBehandling(
    val gjeldende: VedtaksperiodeGrunnlagForPerson?,
    val erReduksjonSidenForrigeBehandling: Boolean,
)

fun erReduksjonSidenForrigeBehandling(
    grunnlagForPersonGjeldendePeriode: VedtaksperiodeGrunnlagForPerson?,
    grunnlagForPersonGjeldendePeriodeForrigeBehandling: VedtaksperiodeGrunnlagForPerson?,
    grunnlagForPersonForrigePeriode: VedtaksperiodeGrunnlagForPerson?,
): Boolean {
    val gjeldendePeriodeErInnvilget =
        grunnlagForPersonGjeldendePeriode != null && grunnlagForPersonGjeldendePeriode.erInnvilget()
    val gjeldendePeriodeForrigeBehandlingErInnvilget =
        grunnlagForPersonGjeldendePeriodeForrigeBehandling is VedtaksperiodeGrunnlagForPersonVilkårInnvilget
    val forrigePeriodeErInnvilget =
        grunnlagForPersonForrigePeriode != null && grunnlagForPersonForrigePeriode.erInnvilget()

    return !gjeldendePeriodeErInnvilget &&
        !forrigePeriodeErInnvilget &&
        gjeldendePeriodeForrigeBehandlingErInnvilget
}
