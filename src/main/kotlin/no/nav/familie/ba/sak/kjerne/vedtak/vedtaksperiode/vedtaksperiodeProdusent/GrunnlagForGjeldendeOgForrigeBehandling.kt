package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent

data class GrunnlagForGjeldendeOgForrigeBehandling(
    val gjeldende: VedtaksperiodeGrunnlagForPerson?,
    val erReduksjonSidenForrigeBehandling: Boolean = false,
)

fun erReduksjonSidenForrigeBehandling(
    erInnvilget: Boolean,
    erInnvilgetForrigePeriode: Boolean,
    erInnvilgetIForrigeBehandling: Boolean,
    erInnvilgetIForrigePeriodeIForrigeBehandling: Boolean,
): Boolean {
    val erLøpendeReduksjon =
        !erInnvilget && !erInnvilgetIForrigeBehandling && !erInnvilgetForrigePeriode && erInnvilgetIForrigePeriodeIForrigeBehandling

    return (!erInnvilget && erInnvilgetIForrigeBehandling) || erLøpendeReduksjon
}
