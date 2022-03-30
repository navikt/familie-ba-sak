package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.common.til18ÅrsVilkårsdato
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer.VilkårsresultatDagTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer.tilVilkårsresultaterMånedTidslinje
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VilkårsresultatMånedTidslinjeTest {

    @Test
    fun `Virkningstidspunkt fra vilkårsvurdering er måneden etter at normalt vilkår er oppfylt`() {
        val periodeFom = LocalDate.of(2022, 4, 15)
        val vilkårsresultatMånedTidslinje = VilkårsresultatDagTidslinje(
            vilkårsresultater = listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    periodeFom = periodeFom,
                    periodeTom = null
                )
            ),
            praktiskTidligsteDato = periodeFom,
            praktiskSenesteDato = periodeFom.til18ÅrsVilkårsdato()
        ).tilVilkårsresultaterMånedTidslinje()

        assertEquals(
            periodeFom.plusMonths(1).toYearMonth(),
            vilkårsresultatMånedTidslinje.perioder().toList().minOf { it.fraOgMed.tilYearMonth() }
        )
    }

    @Test
    fun `Virkningstidspunkt for vilkårsvurdering varer frem til måneden før barnet fyller 18 år`() {
        val periodeFom = LocalDate.of(2022, 4, 15)
        val vilkårsresultatMånedTidslinje = VilkårsresultatDagTidslinje(
            vilkårsresultater = listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.UNDER_18_ÅR,
                    periodeFom = periodeFom,
                    periodeTom = periodeFom.til18ÅrsVilkårsdato()
                )
            ),
            praktiskTidligsteDato = periodeFom,
            praktiskSenesteDato = periodeFom.til18ÅrsVilkårsdato()
        ).tilVilkårsresultaterMånedTidslinje()

        assertEquals(
            periodeFom.til18ÅrsVilkårsdato().minusMonths(1).toYearMonth(),
            vilkårsresultatMånedTidslinje.perioder().toList().maxOf { it.tilOgMed.tilYearMonth() }
        )
    }

    @Test
    fun `Back to back perioder i månedsskiftet gir sammenhengende perioder`() {
        val periodeFom = LocalDate.of(2022, 4, 15)
        val periodeFom2 = LocalDate.of(2022, 7, 1)
        val vilkårsresultatMånedTidslinje = VilkårsresultatDagTidslinje(
            vilkårsresultater = listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    periodeFom = periodeFom,
                    periodeTom = periodeFom2.minusDays(1)
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    periodeFom = periodeFom2,
                    periodeTom = null
                )
            ),
            praktiskTidligsteDato = periodeFom,
            praktiskSenesteDato = periodeFom.til18ÅrsVilkårsdato()
        ).tilVilkårsresultaterMånedTidslinje()

        val sortertePerioder = vilkårsresultatMånedTidslinje.perioder().sortedBy { it.fraOgMed }

        assertEquals(2, sortertePerioder.size)
        assertEquals(periodeFom2.toYearMonth(), sortertePerioder.first().tilOgMed.tilYearMonth())
        assertEquals(periodeFom2.toYearMonth().plusMonths(1), sortertePerioder.last().fraOgMed.tilYearMonth())
    }
}
