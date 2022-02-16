package no.nav.familie.ba.sak.kjerne.eøs

import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.common.rangeTo
import no.nav.familie.ba.sak.kjerne.eøs.RegelverkPeriodeUtil.slåSammenRegelverkMåneder
import no.nav.familie.ba.sak.kjerne.eøs.TestUtil.apr
import no.nav.familie.ba.sak.kjerne.eøs.TestUtil.aug
import no.nav.familie.ba.sak.kjerne.eøs.TestUtil.des
import no.nav.familie.ba.sak.kjerne.eøs.TestUtil.feb
import no.nav.familie.ba.sak.kjerne.eøs.TestUtil.jan
import no.nav.familie.ba.sak.kjerne.eøs.TestUtil.jul
import no.nav.familie.ba.sak.kjerne.eøs.TestUtil.jun
import no.nav.familie.ba.sak.kjerne.eøs.TestUtil.mai
import no.nav.familie.ba.sak.kjerne.eøs.TestUtil.mar
import no.nav.familie.ba.sak.kjerne.eøs.TestUtil.nov
import no.nav.familie.ba.sak.kjerne.eøs.TestUtil.okt
import no.nav.familie.ba.sak.kjerne.eøs.TestUtil.sep
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.MAX_MÅNED
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.RegelverkMåned
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.RegelverkPeriode
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk.EØS_FORORDNINGEN
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk.NASJONALE_REGLER
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOR_MED_SØKER
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.LOVLIG_OPPHOLD
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class RegelverkPeriodeUtilTest {

    @Test
    fun `Lag VilkårResultatMåneder fra ett vilkårResultat`() {
        // Vilkårsvurdering -> Personresultater -> vilkårsresultater
        val vilkårResultater = listOf(
            lagVilkårResultat(BOR_MED_SØKER, EØS_FORORDNINGEN, jan(), sep())
        )

        val vilkårResultatMåneder = RegelverkPeriodeUtil.lagVilkårResultatMåneder(vilkårResultater)
        assertEquals(9, vilkårResultatMåneder.size)
        assertTrue(vilkårResultatMåneder.all { it.vilkårType == BOR_MED_SØKER })
        assertTrue(vilkårResultatMåneder.all { it.vurderesEtter == EØS_FORORDNINGEN })
        assertEquals(vilkårResultatMåneder.map { it.måned }.sorted(), (jan()..sep()).toList())
    }

    @Test
    fun `Lag VilkårResultatMåneder fra flere vilkårResultat`() {
        // Vilkårsvurdering -> Personresultater -> vilkårsresultater
        val vilkårResultater = listOf(
            lagVilkårResultat(BOR_MED_SØKER, EØS_FORORDNINGEN, jan(), sep()),
            lagVilkårResultat(LOVLIG_OPPHOLD, NASJONALE_REGLER, feb(), okt()),
            lagVilkårResultat(BOSATT_I_RIKET, EØS_FORORDNINGEN, mar(), aug())
        )

        val vilkårResultatMåneder = RegelverkPeriodeUtil.lagVilkårResultatMåneder(vilkårResultater)
        assertEquals(24, vilkårResultatMåneder.size)
        val vilkårResultatMånederMap = vilkårResultatMåneder.groupBy { it.vilkårType }
        assertEquals(3, vilkårResultatMånederMap.size)
        assertTrue(vilkårResultatMånederMap[BOR_MED_SØKER]!!.all { it.vurderesEtter == EØS_FORORDNINGEN })
        assertEquals(vilkårResultatMånederMap[BOR_MED_SØKER]!!.map { it.måned }.sorted(), (jan()..sep()).toList())
        assertTrue(vilkårResultatMånederMap[LOVLIG_OPPHOLD]!!.all { it.vurderesEtter == NASJONALE_REGLER })
        assertEquals(vilkårResultatMånederMap[LOVLIG_OPPHOLD]!!.map { it.måned }.sorted(), (feb()..okt()).toList())
        assertTrue(vilkårResultatMånederMap[BOSATT_I_RIKET]!!.all { it.vurderesEtter == EØS_FORORDNINGEN })
        assertEquals(vilkårResultatMånederMap[BOSATT_I_RIKET]!!.map { it.måned }.sorted(), (mar()..aug()).toList())
    }

    @Test
    fun `Lag VilkårResultatMåneder med åpen til-og-med`() {
        // Vilkårsvurdering -> Personresultater -> vilkårsresultater
        val vilkårResultater = listOf(
            lagVilkårResultat(LOVLIG_OPPHOLD, NASJONALE_REGLER, feb(), null),
        )

        val vilkårResultatMåneder =
            RegelverkPeriodeUtil.lagVilkårResultatMåneder(vilkårResultater).sortedBy { it.måned }
        assertEquals(2, vilkårResultatMåneder.size)
        assertEquals(feb(), vilkårResultatMåneder[0].måned)
        assertEquals(LOVLIG_OPPHOLD, vilkårResultatMåneder[0].vilkårType)
        assertEquals(NASJONALE_REGLER, vilkårResultatMåneder[0].vurderesEtter)

        assertEquals(MAX_MÅNED, vilkårResultatMåneder[1].måned)
        assertEquals(LOVLIG_OPPHOLD, vilkårResultatMåneder[1].vilkårType)
        assertEquals(NASJONALE_REGLER, vilkårResultatMåneder[1].vurderesEtter)
    }

    @Test
    fun `slå sammen perioder fra samme regelverk`() {
        val regelverkMåneder =
            lagRegelverkMåneder(EØS_FORORDNINGEN, feb(), mar(), apr(), jun(), jul(), sep(), okt(), des())

        val regelverkPerioder = slåSammenRegelverkMåneder(regelverkMåneder)
        assertEquals(
            listOf(
                RegelverkPeriode(feb(), apr(), EØS_FORORDNINGEN),
                RegelverkPeriode(jun(), jul(), EØS_FORORDNINGEN),
                RegelverkPeriode(sep(), okt(), EØS_FORORDNINGEN),
                RegelverkPeriode(des(), des(), EØS_FORORDNINGEN),
            ),
            regelverkPerioder
        )
    }

    @Test
    fun `slå sammen perioder med duplikater`() {
        val regelverkMåneder =
            lagRegelverkMåneder(
                EØS_FORORDNINGEN,
                feb(), mar(), apr(), jun(), mar(), jul(), sep(), okt(), des(), feb(), mar()
            )

        val regelverkPerioder = slåSammenRegelverkMåneder(regelverkMåneder)
        assertEquals(
            listOf(
                RegelverkPeriode(feb(), apr(), EØS_FORORDNINGEN),
                RegelverkPeriode(jun(), jul(), EØS_FORORDNINGEN),
                RegelverkPeriode(sep(), okt(), EØS_FORORDNINGEN),
                RegelverkPeriode(des(), des(), EØS_FORORDNINGEN),
            ),
            regelverkPerioder
        )
    }

    @Test
    fun `slå sammen blandete regelverk`() {
        val regelverkMåneder =
            (
                lagRegelverkMåneder(
                    EØS_FORORDNINGEN,
                    feb(), mar(), apr(), jun(), jul(), sep(), okt(), des()
                ) +
                    lagRegelverkMåneder(
                        NASJONALE_REGLER,
                        mar(), mai(), jun(), jul(), aug(), sep(), okt(), nov()
                    )
                ).sortedBy { it.måned }

        val regelverkPerioder = slåSammenRegelverkMåneder(regelverkMåneder)
        assertEquals(
            listOf(
                RegelverkPeriode(feb(), apr(), EØS_FORORDNINGEN),
                RegelverkPeriode(jun(), jul(), EØS_FORORDNINGEN),
                RegelverkPeriode(sep(), okt(), EØS_FORORDNINGEN),
                RegelverkPeriode(des(), des(), EØS_FORORDNINGEN),
                RegelverkPeriode(mar(), mar(), NASJONALE_REGLER),
                RegelverkPeriode(mai(), nov(), NASJONALE_REGLER),
            ),
            regelverkPerioder
        )
    }

    private fun lagRegelverkMåneder(regelverk: Regelverk, vararg måneder: YearMonth) =
        måneder.map { RegelverkMåned(it, regelverk) }
}
