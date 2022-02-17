package no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene

import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.common.rangeTo
import no.nav.familie.ba.sak.kjerne.eøs.TestUtil.apr
import no.nav.familie.ba.sak.kjerne.eøs.TestUtil.aug
import no.nav.familie.ba.sak.kjerne.eøs.TestUtil.feb
import no.nav.familie.ba.sak.kjerne.eøs.TestUtil.jul
import no.nav.familie.ba.sak.kjerne.eøs.TestUtil.jun
import no.nav.familie.ba.sak.kjerne.eøs.TestUtil.mai
import no.nav.familie.ba.sak.kjerne.eøs.TestUtil.mar
import no.nav.familie.ba.sak.kjerne.eøs.TestUtil.tilVilkårResultatMåneder
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOR_MED_SØKER
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.LOVLIG_OPPHOLD
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VilkårResultatMånedKtTest {

    @Test
    fun test() {
        val vilkårResultatMåneder = listOf(
            lagVilkårResultat(BOSATT_I_RIKET, Regelverk.EØS_FORORDNINGEN, feb(2022), null),
            lagVilkårResultat(LOVLIG_OPPHOLD, Regelverk.EØS_FORORDNINGEN, apr(2022), null),
            lagVilkårResultat(BOR_MED_SØKER, Regelverk.EØS_FORORDNINGEN, aug(2022), null),
        ).tilVilkårResultatMåneder()

        val ekspandertePerioder = vilkårResultatMåneder.ekspanderÅpnePerioder().sortedBy { it.måned }

        assertEquals(6, vilkårResultatMåneder.size)
        assertEquals(8 + 6 + 2, ekspandertePerioder.size)

        val ekspandertePerioderMap = ekspandertePerioder.groupBy { it.vilkårType }
        assertEquals(
            listOf(feb(), mar(), apr(), mai(), jun(), jul(), aug(), MAX_MÅNED),
            ekspandertePerioderMap[BOSATT_I_RIKET]?.map { it.måned }
        )
        assertEquals(
            listOf(apr(), mai(), jun(), jul(), aug(), MAX_MÅNED),
            ekspandertePerioderMap[LOVLIG_OPPHOLD]?.map { it.måned }
        )
        assertEquals(
            listOf(aug(), MAX_MÅNED),
            ekspandertePerioderMap[BOR_MED_SØKER]?.map { it.måned }
        )
    }
}
