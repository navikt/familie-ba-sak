package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.brevBegrunnelseProdusent

data class BegrunnelseGrunnlagForPeriode(
    val dennePerioden: BegrunnelseGrunnlagForPersonIPeriode,
    val forrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?,
    val sammePeriodeForrigeBehandling: BegrunnelseGrunnlagForPersonIPeriode?,
)
