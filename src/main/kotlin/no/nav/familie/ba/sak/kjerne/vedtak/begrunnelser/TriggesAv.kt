package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import no.nav.familie.ba.sak.kjerne.brev.UtvidetScenarioForEndringsperiode
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityVilkår
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
    val valgbar: Boolean = true,
    val endringsaarsaker: Set<Årsak> = emptySet(),
    val etterEndretUtbetaling: Boolean = false,
    val endretUtbetaingSkalUtbetales: Boolean = false,
    val småbarnstillegg: Boolean = false,
    val gjelderFørstePeriode: Boolean = false,
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

    return if (triggesAv.endretUtbetaingSkalUtbetales) {
        inneholderAndelSomSkalUtbetales
    } else {
        inneholderAndelSomIkkeSkalUtbetales
    }
}

fun EndretUtbetalingAndel.oppfyllerSkalUtbetalesTrigger(
    triggesAv: TriggesAv
): Boolean {
    val inneholderAndelSomSkalUtbetales = this.prosent!! != BigDecimal.ZERO
    return triggesAv.endretUtbetaingSkalUtbetales == inneholderAndelSomSkalUtbetales
}

fun TriggesAv.erTriggereOppfyltForEndretUtbetaling(
    vilkår: List<SanityVilkår>?,
    utvidetScenario: UtvidetScenarioForEndringsperiode,
    endretUtbetalingAndel: EndretUtbetalingAndel
): Boolean {
    val hørerTilEtterEndretUtbetaling = this.etterEndretUtbetaling

    val oppfyllerSkalUtbetalesTrigger = endretUtbetalingAndel.oppfyllerSkalUtbetalesTrigger(this)

    val oppfyllerUtvidetScenario = oppfyllerUtvidetScenario(utvidetScenario, vilkår)

    return !hørerTilEtterEndretUtbetaling &&
        oppfyllerSkalUtbetalesTrigger &&
        oppfyllerUtvidetScenario
}

private fun oppfyllerUtvidetScenario(
    utvidetScenario: UtvidetScenarioForEndringsperiode,
    vilkår: List<SanityVilkår>?
): Boolean {
    val erUtvidetYtelseUtenEndring =
        utvidetScenario == UtvidetScenarioForEndringsperiode.UTVIDET_YTELSE_IKKE_ENDRET

    val begrunnelseSkalVisesVedutvidetYtelseUtenEndring =
        vilkår?.contains(SanityVilkår.UTVIDET_BARNETRYGD) ?: false

    return erUtvidetYtelseUtenEndring == begrunnelseSkalVisesVedutvidetYtelseUtenEndring
}
