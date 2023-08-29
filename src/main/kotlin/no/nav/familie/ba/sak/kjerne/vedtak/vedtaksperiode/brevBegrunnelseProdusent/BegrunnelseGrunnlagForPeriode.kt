package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.brevBegrunnelseProdusent

import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype

sealed interface IBegrunnelseGrunnlagForPeriode {
    val dennePerioden: BegrunnelseGrunnlagForPersonIPeriode
    val forrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?

    companion object {
        fun opprett(
            dennePerioden: BegrunnelseGrunnlagForPersonIPeriode,
            forrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?,
            sammePeriodeForrigeBehandling: BegrunnelseGrunnlagForPersonIPeriode?,
            periodetype: Vedtaksperiodetype,

        ): IBegrunnelseGrunnlagForPeriode =
            if (periodetype == Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING) {
                BegrunnelseGrunnlagForPeriodeReduksjonPåTversAvbehanlding(
                    dennePerioden = dennePerioden,
                    forrigePeriode = forrigePeriode,
                    sammePeriodeForrigeBehandling = sammePeriodeForrigeBehandling,
                )
            } else {
                BegrunnelseGrunnlagForPeriode(dennePerioden, forrigePeriode)
            }
    }
}

data class BegrunnelseGrunnlagForPeriode(
    override val dennePerioden: BegrunnelseGrunnlagForPersonIPeriode,
    override val forrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?,
) : IBegrunnelseGrunnlagForPeriode

data class BegrunnelseGrunnlagForPeriodeReduksjonPåTversAvbehanlding(
    override val dennePerioden: BegrunnelseGrunnlagForPersonIPeriode,
    override val forrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?,
    val sammePeriodeForrigeBehandling: BegrunnelseGrunnlagForPersonIPeriode?,
) : IBegrunnelseGrunnlagForPeriode
