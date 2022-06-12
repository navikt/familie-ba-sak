package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseTestController
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpTestController
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursTestController
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingTestController
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.junit.jupiter.api.Test
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

        val utvidetBehandling1 =
            vilkårsvurderingTestController.opprettBehandlingMedVilkårsvurdering(vilkårsvurderingRequest)
                .body?.data!!

        tilkjentYtelseTestController.lagInitiellTilkjentYtelse(utvidetBehandling1.behandlingId)
        /*
        tilkjentYtelseTestController.oppdaterEndretUtebetalingAndeler(
            utvidetBehandling1.behandlingId, deltBosteRequest
        )
         */

        kompetanseTestController.endreKompetanser(utvidetBehandling1.behandlingId, kompetanseRequest)
        utenlandskPeriodebeløpTestController.endreUtenlandskePeriodebeløp(
            utvidetBehandling1.behandlingId,
            utenlandskPeriodebeløpRequest
        )

        valutakursTestController.endreValutakurser(utvidetBehandling1.behandlingId, valutakursRequest)
    }
}
