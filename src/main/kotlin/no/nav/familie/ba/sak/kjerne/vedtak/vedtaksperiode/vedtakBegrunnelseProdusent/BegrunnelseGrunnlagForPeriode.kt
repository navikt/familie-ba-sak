package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent

import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

sealed interface IBegrunnelseGrunnlagForPeriode {
    val dennePerioden: BegrunnelseGrunnlagForPersonIPeriode
    val forrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?
    val sammePeriodeForrigeBehandling: BegrunnelseGrunnlagForPersonIPeriode?

    fun erSmåbarnstilleggIForrigeBehandlingPeriode() = sammePeriodeForrigeBehandling?.andeler?.any { it.type == YtelseType.SMÅBARNSTILLEGG } == true

    fun harKravPåFinnmarkstilleggIForrigeBehandlingPeriode() =
        sammePeriodeForrigeBehandling?.andeler?.any { it.type == YtelseType.FINNMARKSTILLEGG } == true ||
            sammePeriodeForrigeBehandling?.vilkårResultater?.any {
                it.vilkårType == Vilkår.BOSATT_I_RIKET &&
                    it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS)
            } == true

    fun harKravPåSvalbardtilleggIForrigeBehandlingPeriode() =
        sammePeriodeForrigeBehandling?.andeler?.any { it.type == YtelseType.SVALBARDTILLEGG } == true ||
            sammePeriodeForrigeBehandling?.vilkårResultater?.any {
                it.vilkårType == Vilkår.BOSATT_I_RIKET &&
                    it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD)
            } == true

    fun sjekkOmHarKravPåFinnmarkstilleggDennePeriode() =
        dennePerioden.andeler.any { it.type == YtelseType.FINNMARKSTILLEGG } ||
            dennePerioden.vilkårResultater
                .any {
                    it.vilkårType == Vilkår.BOSATT_I_RIKET &&
                        it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS)
                }

    fun sjekkOmHarKravPåFinnmarkstilleggForrigePeriode() =
        forrigePeriode?.andeler?.any { it.type == YtelseType.FINNMARKSTILLEGG } == true ||
            forrigePeriode
                ?.vilkårResultater
                ?.any {
                    it.vilkårType == Vilkår.BOSATT_I_RIKET &&
                        it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS)
                } == true

    fun sjekkOmHarHravPåSvalbardtilleggForrigePeriode() =
        forrigePeriode?.andeler?.any { it.type == YtelseType.SVALBARDTILLEGG } == true ||
            forrigePeriode
                ?.vilkårResultater
                ?.any {
                    it.vilkårType == Vilkår.BOSATT_I_RIKET &&
                        it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD)
                } == true

    fun sjekkOmHarKravPåSvalbardtilleggDennePeriode() =
        dennePerioden.andeler.any { it.type == YtelseType.SVALBARDTILLEGG } ||
            dennePerioden.vilkårResultater
                .any {
                    it.vilkårType == Vilkår.BOSATT_I_RIKET &&
                        it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD)
                }

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
