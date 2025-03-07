package no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ba.sak.datagenerator.ikkeOppfyltVilkår
import no.nav.familie.ba.sak.datagenerator.lagVilkårResultat
import no.nav.familie.ba.sak.datagenerator.oppfyltVilkår
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.transformasjon.tilMåned
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.util.apr
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.util.jul
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.util.konkatenerTidslinjer
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.util.mai
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.util.mar
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.util.nov
import no.nav.familie.ba.sak.kjerne.tidslinjefamiliefelles.util.periode
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk.EØS_FORORDNINGEN
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk.NASJONALE_REGLER
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import no.nav.familie.tidslinje.tilTidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VilkårsresultatMånedTidslinjeTest {
    @Test
    fun `Virkningstidspunkt fra vilkårsvurdering er måneden etter at normalt vilkår er oppfylt`() {
        val dagTidslinje = periode(oppfyltVilkår(BOSATT_I_RIKET), 15.apr(2022), 14.apr(2040)).tilTidslinje()
        val faktiskMånedTidslinje = dagTidslinje.tilMånedsbasertTidslinjeForVilkårRegelverkResultat()
        val forventetMånedTidslinje =
            periode(oppfyltVilkår(BOSATT_I_RIKET), mai(2022), apr(2040)).tilTidslinje().tilMåned { it.first() }

        assertEquals(forventetMånedTidslinje, faktiskMånedTidslinje)
    }

    @Test
    fun `Back to back perioder i månedsskiftet gir sammenhengende perioder`() {
        val periodeFom = 15.apr(2022)
        val periodeFom2 = 1.jul(2022)
        val faktiskMånedTidslinje =
            listOf(
                lagVilkårResultat(
                    vilkårType = BOSATT_I_RIKET,
                    periodeFom = periodeFom,
                    periodeTom = periodeFom2.minusDays(1),
                ),
                lagVilkårResultat(
                    vilkårType = BOSATT_I_RIKET,
                    periodeFom = periodeFom2,
                    periodeTom = null,
                ),
            ).tilVilkårRegelverkResultatTidslinje()
                .tilMånedsbasertTidslinjeForVilkårRegelverkResultat()

        val forventetMånedTidslinje =
            periode(oppfyltVilkår(BOSATT_I_RIKET), mai(2022), null).tilTidslinje().tilMåned { it.first() }

        assertEquals(forventetMånedTidslinje, faktiskMånedTidslinje)
    }

    @Test
    fun `Siste dag fom-måned og første dag i tom-måned gir oppfylt fra neste måned`() {
        val dagTidslinje = periode(oppfyltVilkår(BOSATT_I_RIKET), 29.feb(2020), 1.mai(2020)).tilTidslinje()
        val faktiskMånedstidslinje = dagTidslinje.tilMånedsbasertTidslinjeForVilkårRegelverkResultat()
        val forventetMånedTidslinje =
            periode(oppfyltVilkår(BOSATT_I_RIKET), mar(2020), mai(2020)).tilTidslinje().tilMåned { it.first() }

        assertEquals(forventetMånedTidslinje, faktiskMånedstidslinje)
    }

    @Test
    fun `Bytte av regelverk innen en måned skal gi kontinuerlig oppfylt tidslinje`() {
        val dagvilkårtidslinje =
            konkatenerTidslinjer(
                periode(oppfyltVilkår(BOSATT_I_RIKET, EØS_FORORDNINGEN), 26.feb(2020), 7.mar(2020)).tilTidslinje(),
                periode(oppfyltVilkår(BOSATT_I_RIKET, NASJONALE_REGLER), 21.mar(2020), 13.mai(2020)).tilTidslinje(),
            )

        val faktiskMånedstidslinje = dagvilkårtidslinje.tilMånedsbasertTidslinjeForVilkårRegelverkResultat()

        val forventetMånedstidslinje =
            konkatenerTidslinjer(
                periode(oppfyltVilkår(BOSATT_I_RIKET, EØS_FORORDNINGEN), mar(2020), mar(2020)).tilTidslinje(),
                periode(oppfyltVilkår(BOSATT_I_RIKET, NASJONALE_REGLER), apr(2020), mai(2020)).tilTidslinje(),
            ).tilMåned { it.first() }

        assertEquals(forventetMånedstidslinje, faktiskMånedstidslinje)
    }

    @Test
    fun `Hvis vilkåret er oppfylt siste dag i måneden, skal kun gi oppfylt frem til og med den måneden`() {
        val dagTidslinje = periode(oppfyltVilkår(BOSATT_I_RIKET), 15.apr(2022), 30.nov(2022)).tilTidslinje()
        val faktiskMånedTidslinje = dagTidslinje.tilMånedsbasertTidslinjeForVilkårRegelverkResultat()
        val forventetMånedTidslinje =
            periode(oppfyltVilkår(BOSATT_I_RIKET), mai(2022), nov(2022)).tilTidslinje().tilMåned { it.first() }

        assertEquals(forventetMånedTidslinje, faktiskMånedTidslinje)
    }

    @Test
    fun `Hvis regelverk byttes i månedskiftet, skal det være kontinuerlig oppfylt vilkår`() {
        val dagvilkårtidslinje =
            konkatenerTidslinjer(
                periode(oppfyltVilkår(BOSATT_I_RIKET, EØS_FORORDNINGEN), 15.jan(2018), 31.mar(2020)).tilTidslinje(),
                periode(oppfyltVilkår(BOSATT_I_RIKET, NASJONALE_REGLER), 1.apr(2020), null).tilTidslinje(),
            )

        val faktiskMånedstidslinje = dagvilkårtidslinje.tilMånedsbasertTidslinjeForVilkårRegelverkResultat()

        val forventetMånedstidslinje =
            konkatenerTidslinjer(
                periode(oppfyltVilkår(BOSATT_I_RIKET, EØS_FORORDNINGEN), feb(2018), mar(2020)).tilTidslinje(),
                periode(oppfyltVilkår(BOSATT_I_RIKET, NASJONALE_REGLER), apr(2020), null).tilTidslinje(),
            ).tilMåned { it.first() }

        assertEquals(forventetMånedstidslinje, faktiskMånedstidslinje)
    }

    @Test
    fun `Hvis det byttes fra oppfylt til ikke oppfylt i månedskiftet, skal kun gi oppfylt til og med denne måneden`() {
        val dagvilkårtidslinje =
            konkatenerTidslinjer(
                periode(oppfyltVilkår(BOSATT_I_RIKET, EØS_FORORDNINGEN), 15.jan(2018), 31.mar(2020)).tilTidslinje(),
                periode(ikkeOppfyltVilkår(BOSATT_I_RIKET), 1.apr(2020), null).tilTidslinje(),
            )

        val faktiskMånedstidslinje = dagvilkårtidslinje.tilMånedsbasertTidslinjeForVilkårRegelverkResultat()

        val forventetMånedstidslinje =
            periode(oppfyltVilkår(BOSATT_I_RIKET, EØS_FORORDNINGEN), feb(2018), mar(2020))
                .tilTidslinje()
                .tilMåned { it.first() }

        assertEquals(forventetMånedstidslinje, faktiskMånedstidslinje)
    }

    @Test
    fun `Hvis regelverk byttes dagen før månedskiftet, skal det være kontinuerlig oppfylt vilkår`() {
        val dagvilkårtidslinje =
            konkatenerTidslinjer(
                periode(oppfyltVilkår(BOSATT_I_RIKET, NASJONALE_REGLER), 15.jan(2018), 29.apr(2020)).tilTidslinje(),
                periode(oppfyltVilkår(BOSATT_I_RIKET, EØS_FORORDNINGEN), 30.apr(2020), null).tilTidslinje(),
            )

        val forventetMånedstidslinje =
            konkatenerTidslinjer(
                periode(oppfyltVilkår(BOSATT_I_RIKET, NASJONALE_REGLER), feb(2018), apr(2020)).tilTidslinje(),
                periode(oppfyltVilkår(BOSATT_I_RIKET, EØS_FORORDNINGEN), mai(2020), null).tilTidslinje(),
            ).tilMåned { it.first() }

        val faktiskMånedstidslinje = dagvilkårtidslinje.tilMånedsbasertTidslinjeForVilkårRegelverkResultat()
        assertEquals(forventetMånedstidslinje, faktiskMånedstidslinje)
    }
}
