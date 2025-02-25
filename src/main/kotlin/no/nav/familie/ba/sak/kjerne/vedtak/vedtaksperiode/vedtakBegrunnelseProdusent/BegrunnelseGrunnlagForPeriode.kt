package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent

import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype

sealed interface IBegrunnelseGrunnlagForPeriode {
    val dennePerioden: BegrunnelseGrunnlagForPersonIPeriode
    val forrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?
    val sammePeriodeForrigeBehandling: BegrunnelseGrunnlagForPersonIPeriode?

    fun erSmåbarnstilleggIForrigeBehandlingPeriode() = sammePeriodeForrigeBehandling?.andeler?.any { it.type == YtelseType.SMÅBARNSTILLEGG } == true

    companion object {
        fun opprett(
            dennePerioden: BegrunnelseGrunnlagForPersonIPeriode,
            forrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?,
            sammePeriodeForrigeBehandling: BegrunnelseGrunnlagForPersonIPeriode?,
            periodetype: Vedtaksperiodetype,
        ): IBegrunnelseGrunnlagForPeriode =
            when (periodetype) {
                Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING -> {
                    BegrunnelseGrunnlagForPeriodeMedReduksjonPåTversAvBehandlinger(
                        dennePerioden = dennePerioden,
                        forrigePeriode = forrigePeriode,
                        sammePeriodeForrigeBehandling = sammePeriodeForrigeBehandling,
                    )
                }

                Vedtaksperiodetype.OPPHØR -> {
                    BegrunnelseGrunnlagForPeriodeMedOpphør(
                        dennePerioden = dennePerioden,
                        forrigePeriode = forrigePeriode,
                        sammePeriodeForrigeBehandling = sammePeriodeForrigeBehandling,
                    )
                }

                else -> {
                    BegrunnelseGrunnlagForPeriode(
                        dennePerioden = dennePerioden,
                        forrigePeriode = forrigePeriode,
                        sammePeriodeForrigeBehandling = sammePeriodeForrigeBehandling,
                    )
                }
            }
    }
}

data class BegrunnelseGrunnlagForPeriode(
    override val dennePerioden: BegrunnelseGrunnlagForPersonIPeriode,
    override val forrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?,
    override val sammePeriodeForrigeBehandling: BegrunnelseGrunnlagForPersonIPeriode?,
) : IBegrunnelseGrunnlagForPeriode

data class BegrunnelseGrunnlagForPeriodeMedReduksjonPåTversAvBehandlinger(
    override val dennePerioden: BegrunnelseGrunnlagForPersonIPeriode,
    override val forrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?,
    override val sammePeriodeForrigeBehandling: BegrunnelseGrunnlagForPersonIPeriode?,
) : IBegrunnelseGrunnlagForPeriode

data class BegrunnelseGrunnlagForPeriodeMedOpphør(
    override val dennePerioden: BegrunnelseGrunnlagForPersonIPeriode,
    override val forrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?,
    override val sammePeriodeForrigeBehandling: BegrunnelseGrunnlagForPersonIPeriode?,
) : IBegrunnelseGrunnlagForPeriode
