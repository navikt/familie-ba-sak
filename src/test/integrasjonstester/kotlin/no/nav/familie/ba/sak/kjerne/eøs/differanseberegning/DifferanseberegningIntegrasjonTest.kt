package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import io.mockk.every
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseTestController
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpTestController
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursTestController
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.fraOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.innholdForTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilMånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.rangeTo
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tilOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Utbetalingsperiode
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingTestController
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class DifferanseberegningIntegrasjonTest : AbstractSpringIntegrationTest() {

    @Autowired
    lateinit var vilkårsvurderingTestController: VilkårsvurderingTestController

    @Autowired
    lateinit var tilkjentYtelseTestController: TilkjentYtelseTestController

    @Autowired
    lateinit var kompetanseTestController: KompetanseTestController

    @Autowired
    lateinit var utenlandskPeriodebeløpTestController: UtenlandskPeriodebeløpTestController

    @Autowired
    lateinit var valutakursTestController: ValutakursTestController

    @Autowired
    lateinit var featureToggleService: FeatureToggleService

    @Test
    fun `vilkårsvurdering med EØS-perioder + kompetanser med sekundærland fører til skjemaer med valutakurser`() {
        val søkerStartdato = 1.jan(2020).tilLocalDate()
        val barnStartdato = 2.jan(2020).tilLocalDate()

        val vilkårsvurderingRequest = mapOf(
            søkerStartdato to mapOf(
                Vilkår.BOSATT_I_RIKET /*    */ to "EEEEEEEEEEEEEEEE",
                Vilkår.LOVLIG_OPPHOLD /*    */ to "EEEEEEEEEEEEEEEE"
            ),
            barnStartdato to mapOf(
                Vilkår.UNDER_18_ÅR /*       */ to "++++++++++++++++",
                Vilkår.GIFT_PARTNERSKAP /*  */ to "++++++++++++++++",
                Vilkår.BOSATT_I_RIKET /*    */ to "EEEEEEEEEEEEEEEE",
                Vilkår.LOVLIG_OPPHOLD /*    */ to "EEEEEEEEEEEEEEEE",
                Vilkår.BOR_MED_SØKER /*     */ to "EEEEEEEEEEEEEEEE"
            )
        )

        val deltBosteRequest = mapOf(
            barnStartdato /*                */ to "/////00000011111"
        )

        val kompetanseRequest = mapOf(
            barnStartdato /*                */ to "PPPSSSSSSPPSSS--"
        )

        val utenlandskPeriodebeløpRequest = mapOf(
            barnStartdato /*                */ to "3333444566677777"
        )

        val valutakursRequest = mapOf(
            barnStartdato /*                */ to "5555566644234489"
        )

        val utvidetBehandlingFørsteGang =
            vilkårsvurderingTestController.opprettBehandlingMedVilkårsvurdering(vilkårsvurderingRequest)
                .body?.data!!

        val sumUtbetalingFørsteGang = utvidetBehandlingFørsteGang.utbetalingsperioder.sumUtbetaling()

        // tilkjentYtelseTestController.lagInitiellTilkjentYtelse(utvidetBehandling1.behandlingId)
        val sumUtbetalingDelt = tilkjentYtelseTestController
            .oppdaterEndretUtebetalingAndeler(utvidetBehandlingFørsteGang.behandlingId, deltBosteRequest)
            .body?.data!!.utbetalingsperioder.sumUtbetaling()

        kompetanseTestController.endreKompetanser(utvidetBehandlingFørsteGang.behandlingId, kompetanseRequest)
        utenlandskPeriodebeløpTestController.endreUtenlandskePeriodebeløp(
            utvidetBehandlingFørsteGang.behandlingId,
            utenlandskPeriodebeløpRequest
        )

        val utvidetbehandlingDifferanseberegnet = valutakursTestController
            .endreValutakurser(utvidetBehandlingFørsteGang.behandlingId, valutakursRequest)
            .body?.data!!

        val sumUtbetalingDifferanseberegnet =
            utvidetbehandlingDifferanseberegnet.utbetalingsperioder.sumUtbetaling()

        Assertions.assertEquals(3, utvidetbehandlingDifferanseberegnet.endretUtbetalingAndeler.size)
        Assertions.assertEquals(10, utvidetbehandlingDifferanseberegnet.utbetalingsperioder.size)

        val vilkårsvurderingRequest2 = mapOf(
            søkerStartdato to mapOf(
                Vilkår.BOSATT_I_RIKET /*    */ to "NNNNNNNNNNNNNNNN",
                Vilkår.LOVLIG_OPPHOLD /*    */ to "EEEEEEEEEEEEEEEE"
            ),
            barnStartdato to mapOf(
                Vilkår.UNDER_18_ÅR /*       */ to "++++++++++++++++",
                Vilkår.GIFT_PARTNERSKAP /*  */ to "++++++++++++++++",
                Vilkår.BOSATT_I_RIKET /*    */ to "EEEEEEEEEEEEEEEE",
                Vilkår.LOVLIG_OPPHOLD /*    */ to "EEEEEEEEEEEEEEEE",
                Vilkår.BOR_MED_SØKER /*     */ to "EEEEEEEEEEEEEEEE"
            )
        )

        val utvidetBehandlingTilbakestilt = vilkårsvurderingTestController
            .oppdaterVilkårsvurderingIBehandling(
                utvidetbehandlingDifferanseberegnet.behandlingId,
                vilkårsvurderingRequest2
            )
            .body?.data!!

        val sumUtbetalingTilbakestilt =
            utvidetBehandlingTilbakestilt.utbetalingsperioder.sumUtbetaling()

        Assertions.assertEquals(3, utvidetBehandlingTilbakestilt.endretUtbetalingAndeler.size)
        Assertions.assertEquals(3, utvidetBehandlingTilbakestilt.utbetalingsperioder.size)

        Assertions.assertTrue(sumUtbetalingFørsteGang > 0)
        Assertions.assertTrue(sumUtbetalingDelt < sumUtbetalingFørsteGang)
        Assertions.assertTrue(sumUtbetalingDifferanseberegnet < sumUtbetalingDelt)
        Assertions.assertTrue(sumUtbetalingTilbakestilt == sumUtbetalingDelt)
    }

    @Test
    fun `differanseberegning som git nullutbetaling skal gi feil hvis funksjonsbryter er av`() {
        every {
            featureToggleService.isEnabled(
                FeatureToggleConfig.KAN_BEHANDLE_EØS_NULLUTBETALING,
                false
            )
        } returns false

        val søkerStartdato = 1.jan(2020).tilLocalDate()
        val barnStartdato = 2.jan(2020).tilLocalDate()

        val vilkårsvurderingRequest = mapOf(
            søkerStartdato to mapOf(
                Vilkår.BOSATT_I_RIKET /*    */ to "EEEEEEEEEEEEEEEE",
                Vilkår.LOVLIG_OPPHOLD /*    */ to "EEEEEEEEEEEEEEEE"
            ),
            barnStartdato to mapOf(
                Vilkår.UNDER_18_ÅR /*       */ to "++++++++++++++++",
                Vilkår.GIFT_PARTNERSKAP /*  */ to "++++++++++++++++",
                Vilkår.BOSATT_I_RIKET /*    */ to "EEEEEEEEEEEEEEEE",
                Vilkår.LOVLIG_OPPHOLD /*    */ to "EEEEEEEEEEEEEEEE",
                Vilkår.BOR_MED_SØKER /*     */ to "EEEEEEEEEEEEEEEE"
            )
        )

        val deltBosteRequest = mapOf(
            barnStartdato /*                */ to "/////00000011111"
        )

        val kompetanseRequest = mapOf(
            barnStartdato /*                */ to "PPPSSSSSSPPSSS--"
        )

        val utenlandskPeriodebeløpRequest = mapOf(
            barnStartdato /*                */ to "3333444566677777"
        )

        val valutakursRequest = mapOf(
            barnStartdato /*                */ to "5555566644234489"
        )

        val utvidetBehandlingFørsteGang =
            vilkårsvurderingTestController.opprettBehandlingMedVilkårsvurdering(vilkårsvurderingRequest)
                .body?.data!!

        tilkjentYtelseTestController
            .oppdaterEndretUtebetalingAndeler(utvidetBehandlingFørsteGang.behandlingId, deltBosteRequest)
            .body?.data!!.utbetalingsperioder.sumUtbetaling()

        kompetanseTestController.endreKompetanser(utvidetBehandlingFørsteGang.behandlingId, kompetanseRequest)
        utenlandskPeriodebeløpTestController.endreUtenlandskePeriodebeløp(
            utvidetBehandlingFørsteGang.behandlingId,
            utenlandskPeriodebeløpRequest
        )

        assertThrows<Feil> {
            valutakursTestController
                .endreValutakurser(utvidetBehandlingFørsteGang.behandlingId, valutakursRequest)
                .body?.data!!
        }
    }
}

fun Iterable<Utbetalingsperiode>.sumUtbetaling(): Int {
    val tidslinje = tidslinje {
        this.map {
            Periode(
                it.periodeFom.tilMånedTidspunkt(),
                it.periodeTom.tilMånedTidspunkt(),
                it.utbetaltPerMnd
            )
        }
    }

    return (tidslinje.fraOgMed()..tidslinje.tilOgMed()).fold(0) { sum, tidspunkt ->
        sum + (tidslinje.innholdForTidspunkt(tidspunkt) ?: 0)
    }
}
