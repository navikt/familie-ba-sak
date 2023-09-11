package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.brevBegrunnelseProdusent

import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype

sealed interface IBegrunnelseGrunnlagForPeriode {
    val dennePerioden: BegrunnelseGrunnlagForPersonIPeriode
    val forrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?
    val erSmåbarnstilleggIForrigeBehandlingPeriode: Boolean

    companion object {
        fun opprett(
            dennePerioden: BegrunnelseGrunnlagForPersonIPeriode,
            forrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?,
            sammePeriodeForrigeBehandling: BegrunnelseGrunnlagForPersonIPeriode?,
            periodetype: Vedtaksperiodetype,

        ): IBegrunnelseGrunnlagForPeriode =
            if (periodetype == Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING) {
                BegrunnelseGrunnlagForPeriodeMedReduksjonPåTversAvBehandlinger(
                    dennePerioden = dennePerioden,
                    forrigePeriode = forrigePeriode,
                    sammePeriodeForrigeBehandling = sammePeriodeForrigeBehandling,
                )
            } else {
                BegrunnelseGrunnlagForPeriode(dennePerioden, forrigePeriode, sammePeriodeForrigeBehandling?.andeler?.any { it.type == YtelseType.SMÅBARNSTILLEGG } == true)
            }
    }
}

data class BegrunnelseGrunnlagForPeriode(
    override val dennePerioden: BegrunnelseGrunnlagForPersonIPeriode,
    override val forrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?,
    override val erSmåbarnstilleggIForrigeBehandlingPeriode: Boolean,
) : IBegrunnelseGrunnlagForPeriode

data class BegrunnelseGrunnlagForPeriodeMedReduksjonPåTversAvBehandlinger(
    override val dennePerioden: BegrunnelseGrunnlagForPersonIPeriode,
    override val forrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?,
    val sammePeriodeForrigeBehandling: BegrunnelseGrunnlagForPersonIPeriode?,
) : IBegrunnelseGrunnlagForPeriode {
    override val erSmåbarnstilleggIForrigeBehandlingPeriode: Boolean
        get() = harPeriodeSmåbarnstillegg()

    private fun harPeriodeSmåbarnstillegg() = sammePeriodeForrigeBehandling?.andeler?.any { it.type == YtelseType.SMÅBARNSTILLEGG } == true
}
