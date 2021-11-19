package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

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
    val småbarnstillegg: Boolean = false
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
