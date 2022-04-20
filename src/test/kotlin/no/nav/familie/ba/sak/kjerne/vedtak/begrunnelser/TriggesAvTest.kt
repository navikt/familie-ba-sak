package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import no.nav.familie.ba.sak.common.lagTriggesAv
import no.nav.familie.ba.sak.dataGenerator.brev.lagMinimertUtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.dataGenerator.endretUtbetaling.lagMinimertEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class TriggesAvTest {

    val vilkårUtenUtvidetBarnetrygd: Set<Vilkår> = emptySet()
    val vilkårMedUtvidetBarnetrygd: Set<Vilkår> = setOf(Vilkår.UTVIDET_BARNETRYGD)

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
        ),
        vilkår = vilkårMedUtvidetBarnetrygd
    )

    val triggesIkkeAvSkalUtbetalesMedUtvidetVilkår =
        lagTriggesAv(
            endretUtbetalingSkalUtbetales = false, etterEndretUtbetaling = false,
            endringsaarsaker = setOf(
                Årsak.DELT_BOSTED
            ),
            vilkår = vilkårMedUtvidetBarnetrygd
        )

    val triggesIkkeAvSkalUtbetalesUtenUtvidetVilkår =
        lagTriggesAv(
            endretUtbetalingSkalUtbetales = false, etterEndretUtbetaling = false,
            endringsaarsaker = setOf(
                Årsak.DELT_BOSTED
            ),
            vilkår = vilkårUtenUtvidetBarnetrygd
        )

    val triggesAvSkalUtbetalesMedUtvidetVilkår = lagTriggesAv(
        endretUtbetalingSkalUtbetales = true, etterEndretUtbetaling = false,
        endringsaarsaker = setOf(
            Årsak.DELT_BOSTED
        ),
        vilkår = vilkårMedUtvidetBarnetrygd
    )

    val triggesAvSkalUtbetalesUtenUtvidetVilkår = lagTriggesAv(
        endretUtbetalingSkalUtbetales = true, etterEndretUtbetaling = false,
        endringsaarsaker = setOf(
            Årsak.DELT_BOSTED
        ),
        vilkår = vilkårUtenUtvidetBarnetrygd
    )

    @Test
    fun `Skal gi false dersom er etter endret utbetaling`() {
        val erEtterEndretUbetaling = triggesAvEtterEndretUtbetaling.erTriggereOppfyltForEndretUtbetaling(
            minimertEndretAndel = endretUtbetalingAndelIkkeNull,
            minimerteUtbetalingsperiodeDetaljer = emptyList()
        )

        Assertions.assertFalse(erEtterEndretUbetaling)

        val erEtterEndretUbetalingMedToggle = triggesAvEtterEndretUtbetaling.erTriggereOppfyltForEndretUtbetaling(
            minimertEndretAndel = endretUtbetalingAndelIkkeNull,
            minimerteUtbetalingsperiodeDetaljer = listOf(
                lagMinimertUtbetalingsperiodeDetalj(
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD
                )
            )

        )
        Assertions.assertFalse(erEtterEndretUbetalingMedToggle)
    }

    @Test
    fun `Skal gi riktig resultat for om endring skal utbetaling`() {
        val skalUtbetalesMedUtbetaling = triggesAvSkalUtbetalesMedUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
            minimertEndretAndel = endretUtbetalingAndelIkkeNull,
            minimerteUtbetalingsperiodeDetaljer = emptyList()

        )

        val skalUtbetalesUtenUtbetaling = triggesAvSkalUtbetalesMedUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
            minimertEndretAndel = endretUtbetalingAndelNull,
            minimerteUtbetalingsperiodeDetaljer = emptyList()

        )

        val skalIkkeUtbetalesUtenUtbetaling =
            triggesIkkeAvSkalUtbetalesMedUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                minimertEndretAndel = endretUtbetalingAndelNull,
                minimerteUtbetalingsperiodeDetaljer = emptyList(),
            )

        val skalIkkeUtbetalesMedUtbetaling =
            triggesIkkeAvSkalUtbetalesMedUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                minimertEndretAndel = endretUtbetalingAndelIkkeNull,
                minimerteUtbetalingsperiodeDetaljer = emptyList(),
            )

        Assertions.assertTrue(skalUtbetalesMedUtbetaling)
        Assertions.assertFalse(skalUtbetalesUtenUtbetaling)
        Assertions.assertTrue(skalIkkeUtbetalesUtenUtbetaling)
        Assertions.assertFalse(skalIkkeUtbetalesMedUtbetaling)

        val skalUtbetalesMedUtbetalingMedToggle =
            triggesAvSkalUtbetalesMedUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                minimertEndretAndel = endretUtbetalingAndelIkkeNull,
                minimerteUtbetalingsperiodeDetaljer = listOf(
                    lagMinimertUtbetalingsperiodeDetalj(
                        ytelseType = YtelseType.UTVIDET_BARNETRYGD
                    )
                )

            )

        val skalUtbetalesUtenUtbetalingMedToggle =
            triggesAvSkalUtbetalesMedUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                minimertEndretAndel = endretUtbetalingAndelNull,
                minimerteUtbetalingsperiodeDetaljer = listOf(
                    lagMinimertUtbetalingsperiodeDetalj(
                        ytelseType = YtelseType.UTVIDET_BARNETRYGD
                    )
                )

            )

        val skalIkkeUtbetalesUtenUtbetalingMedToggle =
            triggesIkkeAvSkalUtbetalesMedUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                minimertEndretAndel = endretUtbetalingAndelNull,
                minimerteUtbetalingsperiodeDetaljer = listOf(
                    lagMinimertUtbetalingsperiodeDetalj(
                        ytelseType = YtelseType.UTVIDET_BARNETRYGD
                    )
                )

            )

        val skalIkkeUtbetalesMedUtbetalingMedToggle =
            triggesIkkeAvSkalUtbetalesMedUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                minimertEndretAndel = endretUtbetalingAndelIkkeNull,
                minimerteUtbetalingsperiodeDetaljer = listOf(
                    lagMinimertUtbetalingsperiodeDetalj(
                        ytelseType = YtelseType.UTVIDET_BARNETRYGD
                    )
                )
            )

        Assertions.assertTrue(skalUtbetalesMedUtbetalingMedToggle)
        Assertions.assertFalse(skalUtbetalesUtenUtbetalingMedToggle)
        Assertions.assertTrue(skalIkkeUtbetalesUtenUtbetalingMedToggle)
        Assertions.assertFalse(skalIkkeUtbetalesMedUtbetalingMedToggle)
    }

    @Test
    fun `Skal gi riktig resultat for utvidetScenario`() {
        val utvidetScenarioUtvidetVilkår = triggesAvSkalUtbetalesMedUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(

            minimertEndretAndel = endretUtbetalingAndelIkkeNull,

            minimerteUtbetalingsperiodeDetaljer = emptyList()

        )
        val utvidetScenarioIkkeUtvidetVilkår =
            triggesAvSkalUtbetalesMedUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                minimertEndretAndel = endretUtbetalingAndelIkkeNull, minimerteUtbetalingsperiodeDetaljer = emptyList()

            )
        val ikkeUtvidetScenarioIkkeUtvidetVilkår =
            triggesAvSkalUtbetalesUtenUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                minimertEndretAndel = endretUtbetalingAndelIkkeNull, minimerteUtbetalingsperiodeDetaljer = emptyList()

            )
        val ikkeUtvidetScenarioUtvidetVilkår =
            triggesAvSkalUtbetalesUtenUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                minimertEndretAndel = endretUtbetalingAndelIkkeNull, minimerteUtbetalingsperiodeDetaljer = emptyList()

            )

        Assertions.assertTrue(utvidetScenarioUtvidetVilkår)
        Assertions.assertFalse(utvidetScenarioIkkeUtvidetVilkår)
        Assertions.assertTrue(ikkeUtvidetScenarioIkkeUtvidetVilkår)
        Assertions.assertFalse(ikkeUtvidetScenarioUtvidetVilkår)

        val utvidetScenarioUtvidetVilkårMedToggle =
            triggesAvSkalUtbetalesMedUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                minimertEndretAndel = endretUtbetalingAndelIkkeNull,
                minimerteUtbetalingsperiodeDetaljer = listOf(
                    lagMinimertUtbetalingsperiodeDetalj(
                        ytelseType = YtelseType.UTVIDET_BARNETRYGD
                    )
                )

            )
        val utvidetScenarioIkkeUtvidetVilkårMedToggle =
            triggesAvSkalUtbetalesMedUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                minimertEndretAndel = endretUtbetalingAndelIkkeNull,
                minimerteUtbetalingsperiodeDetaljer = listOf(
                    lagMinimertUtbetalingsperiodeDetalj(
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD
                    )
                )

            )
        val ikkeUtvidetScenarioIkkeUtvidetVilkårMedToggle =
            triggesAvSkalUtbetalesUtenUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                minimertEndretAndel = endretUtbetalingAndelIkkeNull,
                minimerteUtbetalingsperiodeDetaljer = listOf(
                    lagMinimertUtbetalingsperiodeDetalj(
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD
                    )
                )

            )
        val ikkeUtvidetScenarioUtvidetVilkårMedToggle =
            triggesAvSkalUtbetalesUtenUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                minimertEndretAndel = endretUtbetalingAndelIkkeNull,
                minimerteUtbetalingsperiodeDetaljer = listOf(
                    lagMinimertUtbetalingsperiodeDetalj(
                        ytelseType = YtelseType.UTVIDET_BARNETRYGD
                    )
                )

            )

        Assertions.assertTrue(utvidetScenarioUtvidetVilkårMedToggle)
        Assertions.assertFalse(utvidetScenarioIkkeUtvidetVilkårMedToggle)
        Assertions.assertTrue(ikkeUtvidetScenarioIkkeUtvidetVilkårMedToggle)
        Assertions.assertFalse(ikkeUtvidetScenarioUtvidetVilkårMedToggle)
    }

    @Test
    fun `Skal ikke være oppfylt hvis endringsperiode og triggesav ulik årsak`() {
        val endretUtbetalingAndel = lagMinimertEndretUtbetalingAndel(
            prosent = BigDecimal.ZERO,
            årsak = Årsak.ALLEREDE_UTBETALT
        )

        Assertions.assertFalse(
            triggesIkkeAvSkalUtbetalesUtenUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                minimertEndretAndel = endretUtbetalingAndel,
                minimerteUtbetalingsperiodeDetaljer = emptyList(),
            )
        )

        Assertions.assertFalse(
            triggesIkkeAvSkalUtbetalesUtenUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                minimertEndretAndel = endretUtbetalingAndel,
                minimerteUtbetalingsperiodeDetaljer = listOf(
                    lagMinimertUtbetalingsperiodeDetalj(
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD
                    )
                )

            )
        )
    }
}
