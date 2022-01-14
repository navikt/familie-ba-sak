package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import io.mockk.mockk
import no.nav.familie.ba.sak.common.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.common.lagTriggesAv
import no.nav.familie.ba.sak.kjerne.brev.UtvidetScenarioForEndringsperiode
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityVilkår
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class TriggesAvTest {

    val vilkårUtenUtvidetBarnetrygd: List<SanityVilkår> = emptyList()
    val vilkårMedUtvidetBarnetrygd: List<SanityVilkår> = listOf(SanityVilkår.UTVIDET_BARNETRYGD)

    val utvidetScenarioErUtvidetIkkeEndret = UtvidetScenarioForEndringsperiode.UTVIDET_YTELSE_IKKE_ENDRET
    val utvidetScenarioIkkeUtvidet = UtvidetScenarioForEndringsperiode.IKKE_UTVIDET_YTELSE

    val endretUtbetalingAndelNull =
        lagEndretUtbetalingAndel(person = mockk(relaxed = true), prosent = BigDecimal.ZERO)
    val endretUtbetalingAndelIkkeNull =
        lagEndretUtbetalingAndel(person = mockk(relaxed = true), prosent = BigDecimal.ONE)

    val tiggesAvEtterEndretUtbetaling = lagTriggesAv(etterEndretUtbetaling = true, endretUtbetaingSkalUtbetales = true)

    val tiggesIkkeAvSkalUtbetales =
        lagTriggesAv(endretUtbetaingSkalUtbetales = false, etterEndretUtbetaling = false)
    val tiggesAvSkalUtbetales = lagTriggesAv(endretUtbetaingSkalUtbetales = true, etterEndretUtbetaling = false)

    @Test
    fun `Skal gi false dersom er etter endret utbetaling`() {
        val erEtterEndretUbetaling = tiggesAvEtterEndretUtbetaling.erTriggereOppfyltForEndretUtbetaling(
            vilkår = vilkårMedUtvidetBarnetrygd,
            utvidetScenario = utvidetScenarioErUtvidetIkkeEndret,
            endretUtbetalingAndel = endretUtbetalingAndelIkkeNull
        )

        Assertions.assertFalse(erEtterEndretUbetaling)
    }

    @Test
    fun `Skal gi riktig resultat for utbetaling`() {
        val skalUtbetalesMedUtbetaling = tiggesAvSkalUtbetales.erTriggereOppfyltForEndretUtbetaling(
            vilkår = vilkårMedUtvidetBarnetrygd,
            utvidetScenario = utvidetScenarioErUtvidetIkkeEndret,
            endretUtbetalingAndel = endretUtbetalingAndelIkkeNull
        )

        val skalUtbetalesUtenUtbetaling = tiggesAvSkalUtbetales.erTriggereOppfyltForEndretUtbetaling(
            vilkår = vilkårMedUtvidetBarnetrygd,
            utvidetScenario = utvidetScenarioErUtvidetIkkeEndret,
            endretUtbetalingAndel = endretUtbetalingAndelNull
        )

        val skalIkkeUtbetalesUtenUtbetaling = tiggesIkkeAvSkalUtbetales.erTriggereOppfyltForEndretUtbetaling(
            vilkår = vilkårMedUtvidetBarnetrygd,
            utvidetScenario = utvidetScenarioErUtvidetIkkeEndret,
            endretUtbetalingAndel = endretUtbetalingAndelNull
        )

        val skalIkkeUtbetalesMedUtbetaling = tiggesIkkeAvSkalUtbetales.erTriggereOppfyltForEndretUtbetaling(
            vilkår = vilkårMedUtvidetBarnetrygd,
            utvidetScenario = utvidetScenarioErUtvidetIkkeEndret,
            endretUtbetalingAndel = endretUtbetalingAndelIkkeNull
        )

        Assertions.assertTrue(skalUtbetalesMedUtbetaling)
        Assertions.assertFalse(skalUtbetalesUtenUtbetaling)
        Assertions.assertTrue(skalIkkeUtbetalesUtenUtbetaling)
        Assertions.assertFalse(skalIkkeUtbetalesMedUtbetaling)
    }

    @Test
    fun `Skal gi riktig resultat for utvidetScenario`() {
        val utvidetScenarioUtvidetVilkår = tiggesAvSkalUtbetales.erTriggereOppfyltForEndretUtbetaling(
            vilkår = vilkårMedUtvidetBarnetrygd,
            utvidetScenario = utvidetScenarioErUtvidetIkkeEndret,
            endretUtbetalingAndel = endretUtbetalingAndelIkkeNull
        )
        val utvidetScenarioIkkeUtvidetVilkår = tiggesAvSkalUtbetales.erTriggereOppfyltForEndretUtbetaling(
            vilkår = vilkårMedUtvidetBarnetrygd,
            utvidetScenario = utvidetScenarioIkkeUtvidet,
            endretUtbetalingAndel = endretUtbetalingAndelIkkeNull
        )
        val ikkeUtvidetScenarioIkkeUtvidetVilkår = tiggesAvSkalUtbetales.erTriggereOppfyltForEndretUtbetaling(
            vilkår = vilkårUtenUtvidetBarnetrygd,
            utvidetScenario = utvidetScenarioIkkeUtvidet,
            endretUtbetalingAndel = endretUtbetalingAndelIkkeNull
        )
        val ikkeUtvidetScenarioUtvidetVilkår = tiggesAvSkalUtbetales.erTriggereOppfyltForEndretUtbetaling(
            vilkår = vilkårUtenUtvidetBarnetrygd,
            utvidetScenario = utvidetScenarioErUtvidetIkkeEndret,
            endretUtbetalingAndel = endretUtbetalingAndelIkkeNull
        )

        Assertions.assertTrue(utvidetScenarioUtvidetVilkår)
        Assertions.assertFalse(utvidetScenarioIkkeUtvidetVilkår)
        Assertions.assertTrue(ikkeUtvidetScenarioIkkeUtvidetVilkår)
        Assertions.assertFalse(ikkeUtvidetScenarioUtvidetVilkår)
    }
}
