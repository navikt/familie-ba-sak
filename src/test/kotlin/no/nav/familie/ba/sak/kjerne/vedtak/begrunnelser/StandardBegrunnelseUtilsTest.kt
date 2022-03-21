package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import no.nav.familie.ba.sak.common.lagTriggesAv
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.brev.UtvidetScenarioForEndringsperiode
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertEndretAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class StandardBegrunnelseUtilsTest {
    
    @Test
    fun `placeholder for test`() {

        val triggesAv = lagTriggesAv(
            vilkår = setOf(Vilkår.UTVIDET_BARNETRYGD),
            etterEndretUtbetaling = false,
            endringsaarsaker = setOf(Årsak.DELT_BOSTED),
            endretUtbetalingSkalUtbetales = false,
        )
        val utvidetScenario = UtvidetScenarioForEndringsperiode.UTVIDET_YTELSE_IKKE_ENDRET
        val minimertEndretAndel = MinimertEndretAndel(
            aktørId = "",
            fom = null,
            tom = null,
            årsak = Årsak.DELT_BOSTED,
            prosent = BigDecimal.ZERO,
        )
        val ytelseTyperForPeriode = setOf(YtelseType.UTVIDET_BARNETRYGD)

        Assertions.assertTrue(
            triggesAv.erTriggereOppfyltForEndretUtbetaling(
                utvidetScenario = utvidetScenario,
                minimertEndretAndel = minimertEndretAndel,
                ytelseTyperForPeriode = ytelseTyperForPeriode,
                erIngenOverlappVedtaksperiodeToggelPå = true,
            )
        )

        Assertions.assertTrue(
            triggesAv.erTriggereOppfyltForEndretUtbetaling(
                utvidetScenario = utvidetScenario,
                minimertEndretAndel = minimertEndretAndel,
                ytelseTyperForPeriode = ytelseTyperForPeriode,
                erIngenOverlappVedtaksperiodeToggelPå = false,
            )
        )
    }
}
