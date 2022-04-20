package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertEndretAndel
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertUtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertVilkårResultat
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import java.math.BigDecimal

data class BegrunnelseTriggere(
    val vilkår: Set<Vilkår> = emptySet(),
    val personTyper: Set<PersonType> = setOf(PersonType.BARN, PersonType.SØKER),
    val personerManglerOpplysninger: Boolean = false,
    val satsendring: Boolean = false,
    val barnMedSeksårsdag: Boolean = false,
    val vurderingAnnetGrunnlag: Boolean = false,
    val medlemskap: Boolean = false,
    val deltbosted: Boolean = false,
    val deltBostedSkalIkkeDeles: Boolean = false,
    val valgbar: Boolean = true,
    val endringsaarsaker: Set<Årsak> = emptySet(),
    val etterEndretUtbetaling: Boolean = false,
    val endretUtbetalingSkalUtbetales: Boolean = false,
    val småbarnstillegg: Boolean = false,
    val gjelderFørstePeriode: Boolean = false,
    val gjelderFraInnvilgelsestidspunkt: Boolean = false,
) {
    fun erEndret() = endringsaarsaker.isNotEmpty()

    fun erUtdypendeVilkårsvurderingOppfylt(
        vilkårResultat: MinimertVilkårResultat
    ): Boolean {
        return erDeltBostedOppfylt(vilkårResultat) &&
            erSkjønnsmessigVurderingOppfylt(vilkårResultat) &&
            erMedlemskapOppfylt(vilkårResultat) &&
            erDeltBostedSkalIkkDelesOppfylt(vilkårResultat)
    }

    fun erUtdypendeVilkårsvurderingOppfyltReduksjon(
        vilkårSomAvsluttesRettFørDennePerioden: MinimertVilkårResultat,
        vilkårSomStarterIDennePerioden: MinimertVilkårResultat?,
    ): Boolean {
        return erDeltBostedOppfyltReduksjon(
            vilkårSomAvsluttesRettFørDennePerioden = vilkårSomAvsluttesRettFørDennePerioden,
            vilkårSomStarterIDennePerioden = vilkårSomStarterIDennePerioden
        ) &&
            erSkjønnsmessigVurderingOppfylt(vilkårSomAvsluttesRettFørDennePerioden) &&
            erMedlemskapOppfylt(vilkårSomAvsluttesRettFørDennePerioden) &&
            erDeltBostedSkalIkkDelesOppfylt(vilkårSomAvsluttesRettFørDennePerioden)
    }

    private fun erMedlemskapOppfylt(vilkårResultat: MinimertVilkårResultat): Boolean {
        val vilkårResultatInneholderMedlemsskap =
            vilkårResultat.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP)

        return this.medlemskap == vilkårResultatInneholderMedlemsskap
    }

    private fun erSkjønnsmessigVurderingOppfylt(vilkårResultat: MinimertVilkårResultat): Boolean {
        val vilkårResultatInneholderVurderingAnnetGrunnlag =
            vilkårResultat.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.VURDERING_ANNET_GRUNNLAG)

        return this.vurderingAnnetGrunnlag == vilkårResultatInneholderVurderingAnnetGrunnlag
    }

    private fun erDeltBostedSkalIkkDelesOppfylt(vilkårResultat: MinimertVilkårResultat): Boolean {
        val vilkårResultatInnholderDeltBostedSkalIkkeDeles =
            vilkårResultat.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED_SKAL_IKKE_DELES)

        return this.deltBostedSkalIkkeDeles == vilkårResultatInnholderDeltBostedSkalIkkeDeles
    }

    private fun erDeltBostedOppfylt(vilkårResultat: MinimertVilkårResultat): Boolean {
        val vilkårResultatInneholderDeltBosted =
            vilkårResultat.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED)

        return this.deltbosted == vilkårResultatInneholderDeltBosted
    }

    private fun erDeltBostedOppfyltReduksjon(
        vilkårSomAvsluttesRettFørDennePerioden: MinimertVilkårResultat,
        vilkårSomStarterIDennePerioden: MinimertVilkårResultat?,
    ): Boolean {
        val avsluttetVilkårInneholdtDeltBosted =
            vilkårSomAvsluttesRettFørDennePerioden.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED)

        val påbegyntVilkårInneholderDeltBosted = vilkårSomStarterIDennePerioden?.utdypendeVilkårsvurderinger
            ?.contains(UtdypendeVilkårsvurdering.DELT_BOSTED) ?: false

        return if (this.deltbosted) {
            avsluttetVilkårInneholdtDeltBosted != påbegyntVilkårInneholderDeltBosted
        } else {
            !avsluttetVilkårInneholdtDeltBosted && !påbegyntVilkårInneholderDeltBosted
        }
    }
}

fun triggesAvSkalUtbetales(
    endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
    begrunnelseTriggere: BegrunnelseTriggere
): Boolean {
    if (begrunnelseTriggere.etterEndretUtbetaling) return false

    val inneholderAndelSomSkalUtbetales = endretUtbetalingAndeler.any { it.prosent!! != BigDecimal.ZERO }
    val inneholderAndelSomIkkeSkalUtbetales = endretUtbetalingAndeler.any { it.prosent!! == BigDecimal.ZERO }

    return if (begrunnelseTriggere.endretUtbetalingSkalUtbetales) {
        inneholderAndelSomSkalUtbetales
    } else {
        inneholderAndelSomIkkeSkalUtbetales
    }
}

fun BegrunnelseTriggere.erTriggereOppfyltForEndretUtbetaling(
    minimertEndretAndel: MinimertEndretAndel,
    minimerteUtbetalingsperiodeDetaljer: List<MinimertUtbetalingsperiodeDetalj>,
): Boolean {
    val hørerTilEtterEndretUtbetaling = this.etterEndretUtbetaling

    val oppfyllerSkalUtbetalesTrigger = minimertEndretAndel.oppfyllerSkalUtbetalesTrigger(this)

    val oppfyllerUtvidetScenario =
        oppfyllerUtvidetScenario(
            vilkårBegrunnelsenGjelderFor = this.vilkår,
            minimerteUtbetalingsperiodeDetaljer = minimerteUtbetalingsperiodeDetaljer,
        )

    val erAvSammeÅrsak = this.endringsaarsaker.contains(minimertEndretAndel.årsak)

    return !hørerTilEtterEndretUtbetaling &&
        oppfyllerSkalUtbetalesTrigger &&
        oppfyllerUtvidetScenario && erAvSammeÅrsak
}

fun MinimertEndretAndel.oppfyllerSkalUtbetalesTrigger(
    begrunnelseTriggere: BegrunnelseTriggere
): Boolean {
    val inneholderAndelSomSkalUtbetales = this.prosent!! != BigDecimal.ZERO
    return begrunnelseTriggere.endretUtbetalingSkalUtbetales == inneholderAndelSomSkalUtbetales
}

private fun oppfyllerUtvidetScenario(
    vilkårBegrunnelsenGjelderFor: Set<Vilkår>?,
    minimerteUtbetalingsperiodeDetaljer: List<MinimertUtbetalingsperiodeDetalj>,
): Boolean {

    val begrunnelseGjelderUtvidet = vilkårBegrunnelsenGjelderFor?.contains(Vilkår.UTVIDET_BARNETRYGD) ?: false

    val periodeInneholderUtvidetUtenEndring = minimerteUtbetalingsperiodeDetaljer.singleOrNull {
        it.ytelseType == YtelseType.UTVIDET_BARNETRYGD
    }?.erPåvirketAvEndring == false

    return begrunnelseGjelderUtvidet == periodeInneholderUtvidetUtenEndring
}
