package no.nav.familie.ba.sak.kjerne.eøs.tidslinjer

import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.common.oppfyltVilkår
import no.nav.familie.ba.sak.common.til18ÅrsVilkårsdato
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.konkatener
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.komprimer
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.rangeTo
import no.nav.familie.ba.sak.kjerne.tidslinje.tilTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.forskyv
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.tilMåned
import no.nav.familie.ba.sak.kjerne.tidslinje.util.apr
import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mai
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mar
import no.nav.familie.ba.sak.kjerne.tidslinje.util.print
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk.EØS_FORORDNINGEN
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk.NASJONALE_REGLER
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VilkårsresultatMånedTidslinjeTest {

    @Test
    fun `Virkningstidspunkt fra vilkårsvurdering er måneden etter at normalt vilkår er oppfylt`() {
        val dagTidslinje = (15.apr(2022)..14.apr(2040)).tilTidslinje { oppfyltVilkår(BOSATT_I_RIKET) }
        val faktiskMånedTidslinje = dagTidslinje.tilMånedTidslinje()
        val forventetMånedTidslinje = (mai(2022)..apr(2040)).tilTidslinje { oppfyltVilkår(BOSATT_I_RIKET) }

        assertEquals(
            forventetMånedTidslinje,
            faktiskMånedTidslinje
        )
    }

    @Test
    fun `Back to back perioder i månedsskiftet gir sammenhengende perioder`() {
        val periodeFom = LocalDate.of(2022, 4, 15)
        val periodeFom2 = LocalDate.of(2022, 7, 1)
        val senesteDato = periodeFom.til18ÅrsVilkårsdato() // 2040-04-14
        val vilkårsresultatMånedTidslinje = VilkårsresultatDagTidslinje(
            vilkårsresultater = listOf(
                lagVilkårResultat(
                    vilkårType = BOSATT_I_RIKET,
                    periodeFom = periodeFom,
                    periodeTom = periodeFom2.minusDays(1)
                ),
                lagVilkårResultat(
                    vilkårType = BOSATT_I_RIKET,
                    periodeFom = periodeFom2,
                    periodeTom = null
                )
            ),
            praktiskTidligsteDato = periodeFom,
            praktiskSenesteDato = senesteDato
        ).tilMånedTidslinje().also { it.print() }

        val forventetMånedstidslinje: Tidslinje<VilkårRegelverkResultat, Måned> =
            (mai(2022)..apr(2040)).tilTidslinje { oppfyltVilkår(BOSATT_I_RIKET, NASJONALE_REGLER) }

        assertEquals(forventetMånedstidslinje, vilkårsresultatMånedTidslinje)
    }

    @Test
    fun `Siste dag fom-måned og første dag i tom-måned gir oppfylt fra neste måned`() {
        val dagvilkårtidslinje: Tidslinje<VilkårRegelverkResultat, Dag> =
            (29.feb(2020)..1.mai(2020)).tilTidslinje { oppfyltVilkår(BOSATT_I_RIKET) }

        val forventetMånedstidslinje: Tidslinje<VilkårRegelverkResultat, Måned> =
            (mar(2020)..mai(2020)).tilTidslinje { oppfyltVilkår(BOSATT_I_RIKET) }

        val faktiskMånedstidslinje = dagvilkårtidslinje.tilMånedTidslinje()
        assertEquals(forventetMånedstidslinje, faktiskMånedstidslinje)
    }

    @Test
    fun `Bytte av regelverk innen en måned skal gi kontinuerlig oppfylt tidslinje`() {
        val dagvilkårtidslinje = konkatener(
            (26.feb(2020)..7.mar(2020)).tilTidslinje { oppfyltVilkår(BOSATT_I_RIKET, EØS_FORORDNINGEN) },
            (21.mar(2020)..13.mai(2020)).tilTidslinje { oppfyltVilkår(BOSATT_I_RIKET, NASJONALE_REGLER) },
        ).also { it.print() }

        val forventetMånedstidslinje = konkatener(
            mar(2020).tilTidslinje { oppfyltVilkår(BOSATT_I_RIKET, EØS_FORORDNINGEN) },
            (apr(2020)..mai(2020)).tilTidslinje { oppfyltVilkår(BOSATT_I_RIKET, NASJONALE_REGLER) },
        ).also { it.print() }

        val faktiskMånedstidslinje = dagvilkårtidslinje.tilMånedTidslinje().also { it.print() }
        assertEquals(forventetMånedstidslinje, faktiskMånedstidslinje)
    }
}

fun Tidslinje<VilkårRegelverkResultat, Dag>.tilMånedTidslinje(): Tidslinje<VilkårRegelverkResultat, Måned> {

    return this
        .tilMåned { it.last() } // Månedsverdien er verdien fra siste dag i måneden
        .komprimer() // Slå sammen perioder som nå er like
        .filtrerIkkeNull() // Ta bort alle perioder som har null-verdi
        .forskyv(1) // Flytt alt én måned senere
}
