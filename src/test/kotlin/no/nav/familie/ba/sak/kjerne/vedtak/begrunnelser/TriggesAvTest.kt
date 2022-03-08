package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import no.nav.familie.ba.sak.common.lagTriggesAv
import no.nav.familie.ba.sak.dataGenerator.endretUtbetaling.lagMinimertEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.brev.UtvidetScenarioForEndringsperiode
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityVilkår
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class TriggesAvTest {

    val vilkårUtenUtvidetBarnetrygd: List<SanityVilkår> = emptyList()
    val vilkårMedUtvidetBarnetrygd: List<SanityVilkår> = listOf(SanityVilkår.UTVIDET_BARNETRYGD)

    val utvidetScenarioErUtvidetIkkeEndret = UtvidetScenarioForEndringsperiode.UTVIDET_YTELSE_IKKE_ENDRET
    val utvidetScenarioIkkeUtvidet = UtvidetScenarioForEndringsperiode.IKKE_UTVIDET_YTELSE

    val endretUtbetalingAndelNull =
        lagMinimertEndretUtbetalingAndel(
            prosent = BigDecimal.ZERO,
            årsak = Årsak.DELT_BOSTED
        )
    val endretUtbetalingAndelIkkeNull =
        lagMinimertEndretUtbetalingAndel(
            prosent = BigDecimal.ONE,
            årsak = Årsak.DELT_BOSTED
        )

    val triggesAvEtterEndretUtbetaling = lagTriggesAv(
        etterEndretUtbetaling = true, endretUtbetalingSkalUtbetales = true,
        endringsaarsaker = setOf(
            Årsak.DELT_BOSTED
        )
    )

    val triggesIkkeAvSkalUtbetales =
        lagTriggesAv(
            endretUtbetalingSkalUtbetales = false, etterEndretUtbetaling = false,
            endringsaarsaker = setOf(
                Årsak.DELT_BOSTED
            )
        )
    val triggesAvSkalUtbetales = lagTriggesAv(
        endretUtbetalingSkalUtbetales = true, etterEndretUtbetaling = false,
        endringsaarsaker = setOf(
            Årsak.DELT_BOSTED
        )
    )

    @Test
    fun `Skal gi false dersom er etter endret utbetaling`() {
        val erEtterEndretUbetaling = triggesAvEtterEndretUtbetaling.erTriggereOppfyltForEndretUtbetaling(
            vilkår = vilkårMedUtvidetBarnetrygd,
            utvidetScenario = utvidetScenarioErUtvidetIkkeEndret,
            minimertEndretAndel = endretUtbetalingAndelIkkeNull
        )

        Assertions.assertFalse(erEtterEndretUbetaling)
    }

    @Test
    fun `Skal gi riktig resultat for utbetaling`() {
        val skalUtbetalesMedUtbetaling = triggesAvSkalUtbetales.erTriggereOppfyltForEndretUtbetaling(
            vilkår = vilkårMedUtvidetBarnetrygd,
            utvidetScenario = utvidetScenarioErUtvidetIkkeEndret,
            minimertEndretAndel = endretUtbetalingAndelIkkeNull
        )

        val skalUtbetalesUtenUtbetaling = triggesAvSkalUtbetales.erTriggereOppfyltForEndretUtbetaling(
            vilkår = vilkårMedUtvidetBarnetrygd,
            utvidetScenario = utvidetScenarioErUtvidetIkkeEndret,
            minimertEndretAndel = endretUtbetalingAndelNull
        )

        val skalIkkeUtbetalesUtenUtbetaling = triggesIkkeAvSkalUtbetales.erTriggereOppfyltForEndretUtbetaling(
            vilkår = vilkårMedUtvidetBarnetrygd,
            utvidetScenario = utvidetScenarioErUtvidetIkkeEndret,
            minimertEndretAndel = endretUtbetalingAndelNull
        )

        val skalIkkeUtbetalesMedUtbetaling = triggesIkkeAvSkalUtbetales.erTriggereOppfyltForEndretUtbetaling(
            vilkår = vilkårMedUtvidetBarnetrygd,
            utvidetScenario = utvidetScenarioErUtvidetIkkeEndret,
            minimertEndretAndel = endretUtbetalingAndelIkkeNull
        )

        Assertions.assertTrue(skalUtbetalesMedUtbetaling)
        Assertions.assertFalse(skalUtbetalesUtenUtbetaling)
        Assertions.assertTrue(skalIkkeUtbetalesUtenUtbetaling)
        Assertions.assertFalse(skalIkkeUtbetalesMedUtbetaling)
    }

    @Test
    fun `Skal gi riktig resultat for utvidetScenario`() {
        val utvidetScenarioUtvidetVilkår = triggesAvSkalUtbetales.erTriggereOppfyltForEndretUtbetaling(
            vilkår = vilkårMedUtvidetBarnetrygd,
            utvidetScenario = utvidetScenarioErUtvidetIkkeEndret,
            minimertEndretAndel = endretUtbetalingAndelIkkeNull
        )
        val utvidetScenarioIkkeUtvidetVilkår = triggesAvSkalUtbetales.erTriggereOppfyltForEndretUtbetaling(
            vilkår = vilkårMedUtvidetBarnetrygd,
            utvidetScenario = utvidetScenarioIkkeUtvidet,
            minimertEndretAndel = endretUtbetalingAndelIkkeNull
        )
        val ikkeUtvidetScenarioIkkeUtvidetVilkår = triggesAvSkalUtbetales.erTriggereOppfyltForEndretUtbetaling(
            vilkår = vilkårUtenUtvidetBarnetrygd,
            utvidetScenario = utvidetScenarioIkkeUtvidet,
            minimertEndretAndel = endretUtbetalingAndelIkkeNull
        )
        val ikkeUtvidetScenarioUtvidetVilkår = triggesAvSkalUtbetales.erTriggereOppfyltForEndretUtbetaling(
            vilkår = vilkårUtenUtvidetBarnetrygd,
            utvidetScenario = utvidetScenarioErUtvidetIkkeEndret,
            minimertEndretAndel = endretUtbetalingAndelIkkeNull
        )

        Assertions.assertTrue(utvidetScenarioUtvidetVilkår)
        Assertions.assertFalse(utvidetScenarioIkkeUtvidetVilkår)
        Assertions.assertTrue(ikkeUtvidetScenarioIkkeUtvidetVilkår)
        Assertions.assertFalse(ikkeUtvidetScenarioUtvidetVilkår)
    }

    @Test
    fun `Skal ikke være oppfylt hvis endringsperiode og triggesav ulik årsak`() {
        val endretUtbetalingAndel = lagMinimertEndretUtbetalingAndel(
            prosent = BigDecimal.ZERO,
            årsak = Årsak.ALLEREDE_UTBETALT
        )

        Assertions.assertFalse(
            triggesIkkeAvSkalUtbetales.erTriggereOppfyltForEndretUtbetaling(
                vilkår = vilkårUtenUtvidetBarnetrygd,
                utvidetScenario = utvidetScenarioIkkeUtvidet,
                minimertEndretAndel = endretUtbetalingAndel
            )
        )
    }
}
