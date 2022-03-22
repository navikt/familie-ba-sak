package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.brev.UtvidetScenarioForEndringsperiode
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertEndretAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import java.math.BigDecimal

data class TriggesAv(
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
}

fun triggesAvSkalUtbetales(
    endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
    triggesAv: TriggesAv
): Boolean {
    if (triggesAv.etterEndretUtbetaling) return false

    val inneholderAndelSomSkalUtbetales = endretUtbetalingAndeler.any { it.prosent!! != BigDecimal.ZERO }
    val inneholderAndelSomIkkeSkalUtbetales = endretUtbetalingAndeler.any { it.prosent!! == BigDecimal.ZERO }

    return if (triggesAv.endretUtbetalingSkalUtbetales) {
        inneholderAndelSomSkalUtbetales
    } else {
        inneholderAndelSomIkkeSkalUtbetales
    }
}
fun TriggesAv.erTriggereOppfyltForEndretUtbetaling(
    utvidetScenario: UtvidetScenarioForEndringsperiode,
    minimertEndretAndel: MinimertEndretAndel,
    ytelseTyperForPeriode: Set<YtelseType>,
    erIngenOverlappVedtaksperiodeToggelPå: Boolean,
): Boolean {
    val hørerTilEtterEndretUtbetaling = this.etterEndretUtbetaling

    val oppfyllerSkalUtbetalesTrigger = minimertEndretAndel.oppfyllerSkalUtbetalesTrigger(this)

    val oppfyllerUtvidetScenario =
        oppfyllerUtvidetScenario(
            utvidetScenario,
            this.vilkår,
            ytelseTyperForPeriode,
            erIngenOverlappVedtaksperiodeToggelPå
        )

    val erAvSammeÅrsak = this.endringsaarsaker.contains(minimertEndretAndel.årsak)

    return !hørerTilEtterEndretUtbetaling &&
        oppfyllerSkalUtbetalesTrigger &&
        oppfyllerUtvidetScenario && erAvSammeÅrsak
}

fun MinimertEndretAndel.oppfyllerSkalUtbetalesTrigger(
    triggesAv: TriggesAv
): Boolean {
    val inneholderAndelSomSkalUtbetales = this.prosent!! != BigDecimal.ZERO
    return triggesAv.endretUtbetalingSkalUtbetales == inneholderAndelSomSkalUtbetales
}

private fun oppfyllerUtvidetScenario(
    utvidetScenario: UtvidetScenarioForEndringsperiode,
    vilkår: Set<Vilkår>?,
    ytelseTyperForPeriode: Set<YtelseType>,
    erIngenOverlappVedtaksperiodeToggelPå: Boolean,
): Boolean {
    return if (erIngenOverlappVedtaksperiodeToggelPå) {
        val begrunnelseGjelderUtvidet = vilkår?.contains(Vilkår.UTVIDET_BARNETRYGD) ?: false

        begrunnelseGjelderUtvidet == ytelseTyperForPeriode.contains(YtelseType.UTVIDET_BARNETRYGD)
    } else {
        val erUtvidetYtelseUtenEndring =
            utvidetScenario == UtvidetScenarioForEndringsperiode.UTVIDET_YTELSE_IKKE_ENDRET

        val begrunnelseSkalVisesVedutvidetYtelseUtenEndring =
            vilkår?.contains(Vilkår.UTVIDET_BARNETRYGD) ?: false

        erUtvidetYtelseUtenEndring == begrunnelseSkalVisesVedutvidetYtelseUtenEndring
    }
}
