package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import no.nav.familie.ba.sak.common.lagTriggesAv
import no.nav.familie.ba.sak.dataGenerator.brev.lagMinimertUtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.dataGenerator.endretUtbetaling.lagMinimertEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.brev.UtvidetScenarioForEndringsperiode
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class TriggesAvTest {

    val vilkårUtenUtvidetBarnetrygd: Set<Vilkår> = emptySet()
    val vilkårMedUtvidetBarnetrygd: Set<Vilkår> = setOf(Vilkår.UTVIDET_BARNETRYGD)

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
            utvidetScenario = utvidetScenarioErUtvidetIkkeEndret,
            minimertEndretAndel = endretUtbetalingAndelIkkeNull,
            erIngenOverlappVedtaksperiodeToggelPå = false,
            minimerteUtbetalingsperiodeDetaljer = emptyList()
        )

        Assertions.assertFalse(erEtterEndretUbetaling)

        val erEtterEndretUbetalingMedToggle = triggesAvEtterEndretUtbetaling.erTriggereOppfyltForEndretUtbetaling(
            utvidetScenario = utvidetScenarioErUtvidetIkkeEndret,
            minimertEndretAndel = endretUtbetalingAndelIkkeNull,
            erIngenOverlappVedtaksperiodeToggelPå = true,
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
            utvidetScenario = utvidetScenarioErUtvidetIkkeEndret,
            minimertEndretAndel = endretUtbetalingAndelIkkeNull,
            erIngenOverlappVedtaksperiodeToggelPå = false,
            minimerteUtbetalingsperiodeDetaljer = emptyList()

        )

        val skalUtbetalesUtenUtbetaling = triggesAvSkalUtbetalesMedUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
            utvidetScenario = utvidetScenarioErUtvidetIkkeEndret,
            minimertEndretAndel = endretUtbetalingAndelNull,
            erIngenOverlappVedtaksperiodeToggelPå = false,
            minimerteUtbetalingsperiodeDetaljer = emptyList()

        )

        val skalIkkeUtbetalesUtenUtbetaling =
            triggesIkkeAvSkalUtbetalesMedUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                utvidetScenario = utvidetScenarioErUtvidetIkkeEndret,
                minimertEndretAndel = endretUtbetalingAndelNull,
                erIngenOverlappVedtaksperiodeToggelPå = false,
                minimerteUtbetalingsperiodeDetaljer = emptyList()

            )

        val skalIkkeUtbetalesMedUtbetaling =
            triggesIkkeAvSkalUtbetalesMedUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                utvidetScenario = utvidetScenarioErUtvidetIkkeEndret,
                minimertEndretAndel = endretUtbetalingAndelIkkeNull,
                erIngenOverlappVedtaksperiodeToggelPå = false,
                minimerteUtbetalingsperiodeDetaljer = emptyList()

            )

        Assertions.assertTrue(skalUtbetalesMedUtbetaling)
        Assertions.assertFalse(skalUtbetalesUtenUtbetaling)
        Assertions.assertTrue(skalIkkeUtbetalesUtenUtbetaling)
        Assertions.assertFalse(skalIkkeUtbetalesMedUtbetaling)

        val skalUtbetalesMedUtbetalingMedToggle =
            triggesAvSkalUtbetalesMedUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                utvidetScenario = utvidetScenarioErUtvidetIkkeEndret,
                minimertEndretAndel = endretUtbetalingAndelIkkeNull,
                erIngenOverlappVedtaksperiodeToggelPå = true,
                minimerteUtbetalingsperiodeDetaljer = listOf(
                    lagMinimertUtbetalingsperiodeDetalj(
                        ytelseType = YtelseType.UTVIDET_BARNETRYGD
                    )
                )

            )

        val skalUtbetalesUtenUtbetalingMedToggle =
            triggesAvSkalUtbetalesMedUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                utvidetScenario = utvidetScenarioErUtvidetIkkeEndret,
                minimertEndretAndel = endretUtbetalingAndelNull,
                erIngenOverlappVedtaksperiodeToggelPå = true,
                minimerteUtbetalingsperiodeDetaljer = listOf(
                    lagMinimertUtbetalingsperiodeDetalj(
                        ytelseType = YtelseType.UTVIDET_BARNETRYGD
                    )
                )

            )

        val skalIkkeUtbetalesUtenUtbetalingMedToggle =
            triggesIkkeAvSkalUtbetalesMedUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                utvidetScenario = utvidetScenarioErUtvidetIkkeEndret,
                minimertEndretAndel = endretUtbetalingAndelNull,
                erIngenOverlappVedtaksperiodeToggelPå = true,
                minimerteUtbetalingsperiodeDetaljer = listOf(
                    lagMinimertUtbetalingsperiodeDetalj(
                        ytelseType = YtelseType.UTVIDET_BARNETRYGD
                    )
                )

            )

        val skalIkkeUtbetalesMedUtbetalingMedToggle =
            triggesIkkeAvSkalUtbetalesMedUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                utvidetScenario = utvidetScenarioErUtvidetIkkeEndret,
                minimertEndretAndel = endretUtbetalingAndelIkkeNull,
                erIngenOverlappVedtaksperiodeToggelPå = true,
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
            utvidetScenario = utvidetScenarioErUtvidetIkkeEndret,
            minimertEndretAndel = endretUtbetalingAndelIkkeNull,
            erIngenOverlappVedtaksperiodeToggelPå = false,
            minimerteUtbetalingsperiodeDetaljer = emptyList()

        )
        val utvidetScenarioIkkeUtvidetVilkår =
            triggesAvSkalUtbetalesMedUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                utvidetScenario = utvidetScenarioIkkeUtvidet,
                minimertEndretAndel = endretUtbetalingAndelIkkeNull,
                erIngenOverlappVedtaksperiodeToggelPå = false,
                minimerteUtbetalingsperiodeDetaljer = emptyList()

            )
        val ikkeUtvidetScenarioIkkeUtvidetVilkår =
            triggesAvSkalUtbetalesUtenUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                utvidetScenario = utvidetScenarioIkkeUtvidet,
                minimertEndretAndel = endretUtbetalingAndelIkkeNull,
                erIngenOverlappVedtaksperiodeToggelPå = false,
                minimerteUtbetalingsperiodeDetaljer = emptyList()

            )
        val ikkeUtvidetScenarioUtvidetVilkår =
            triggesAvSkalUtbetalesUtenUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                utvidetScenario = utvidetScenarioErUtvidetIkkeEndret,
                minimertEndretAndel = endretUtbetalingAndelIkkeNull,
                erIngenOverlappVedtaksperiodeToggelPå = false,
                minimerteUtbetalingsperiodeDetaljer = emptyList()

            )

        Assertions.assertTrue(utvidetScenarioUtvidetVilkår)
        Assertions.assertFalse(utvidetScenarioIkkeUtvidetVilkår)
        Assertions.assertTrue(ikkeUtvidetScenarioIkkeUtvidetVilkår)
        Assertions.assertFalse(ikkeUtvidetScenarioUtvidetVilkår)

        val utvidetScenarioUtvidetVilkårMedToggle =
            triggesAvSkalUtbetalesMedUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                utvidetScenario = utvidetScenarioErUtvidetIkkeEndret,
                minimertEndretAndel = endretUtbetalingAndelIkkeNull,
                erIngenOverlappVedtaksperiodeToggelPå = true,
                minimerteUtbetalingsperiodeDetaljer = listOf(
                    lagMinimertUtbetalingsperiodeDetalj(
                        ytelseType = YtelseType.UTVIDET_BARNETRYGD
                    )
                )

            )
        val utvidetScenarioIkkeUtvidetVilkårMedToggle =
            triggesAvSkalUtbetalesMedUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                utvidetScenario = utvidetScenarioIkkeUtvidet,
                minimertEndretAndel = endretUtbetalingAndelIkkeNull,
                erIngenOverlappVedtaksperiodeToggelPå = true,
                minimerteUtbetalingsperiodeDetaljer = listOf(
                    lagMinimertUtbetalingsperiodeDetalj(
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD
                    )
                )

            )
        val ikkeUtvidetScenarioIkkeUtvidetVilkårMedToggle =
            triggesAvSkalUtbetalesUtenUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                utvidetScenario = utvidetScenarioIkkeUtvidet,
                minimertEndretAndel = endretUtbetalingAndelIkkeNull,
                erIngenOverlappVedtaksperiodeToggelPå = true,
                minimerteUtbetalingsperiodeDetaljer = listOf(
                    lagMinimertUtbetalingsperiodeDetalj(
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD
                    )
                )

            )
        val ikkeUtvidetScenarioUtvidetVilkårMedToggle =
            triggesAvSkalUtbetalesUtenUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                utvidetScenario = utvidetScenarioErUtvidetIkkeEndret,
                minimertEndretAndel = endretUtbetalingAndelIkkeNull,
                erIngenOverlappVedtaksperiodeToggelPå = true,
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
                utvidetScenario = utvidetScenarioIkkeUtvidet,
                minimertEndretAndel = endretUtbetalingAndel,
                erIngenOverlappVedtaksperiodeToggelPå = false,
                minimerteUtbetalingsperiodeDetaljer = emptyList()

            )
        )

        Assertions.assertFalse(
            triggesIkkeAvSkalUtbetalesUtenUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                utvidetScenario = utvidetScenarioIkkeUtvidet,
                minimertEndretAndel = endretUtbetalingAndel,
                erIngenOverlappVedtaksperiodeToggelPå = true,
                minimerteUtbetalingsperiodeDetaljer = listOf(
                    lagMinimertUtbetalingsperiodeDetalj(
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD
                    )
                )

            )
        )
    }
}
