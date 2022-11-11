package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.kjerne.beregning.Prosent.alt
import no.nav.familie.ba.sak.kjerne.beregning.Prosent.halvparten
import no.nav.familie.ba.sak.kjerne.eøs.util.barn
import no.nav.familie.ba.sak.kjerne.eøs.util.der
import no.nav.familie.ba.sak.kjerne.eøs.util.død
import no.nav.familie.ba.sak.kjerne.eøs.util.etter
import no.nav.familie.ba.sak.kjerne.eøs.util.født
import no.nav.familie.ba.sak.kjerne.eøs.util.har
import no.nav.familie.ba.sak.kjerne.eøs.util.med
import no.nav.familie.ba.sak.kjerne.eøs.util.og
import no.nav.familie.ba.sak.kjerne.eøs.util.oppfylt
import no.nav.familie.ba.sak.kjerne.eøs.util.søker
import no.nav.familie.ba.sak.kjerne.eøs.util.til18ÅrVilkårsdato
import no.nav.familie.ba.sak.kjerne.eøs.util.uendelig
import no.nav.familie.ba.sak.kjerne.eøs.util.vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.rangeTo
import no.nav.familie.ba.sak.kjerne.tidslinje.util.VilkårsvurderingBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.aug
import no.nav.familie.ba.sak.kjerne.tidslinje.util.des
import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jun
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mai
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mar
import no.nav.familie.ba.sak.kjerne.tidslinje.util.nov
import no.nav.familie.ba.sak.kjerne.tidslinje.util.sep
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk.EØS_FORORDNINGEN
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk.NASJONALE_REGLER
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.DELT_BOSTED
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOR_MED_SØKER
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.GIFT_PARTNERSKAP
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.LOVLIG_OPPHOLD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.UNDER_18_ÅR
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class BeregnAndelerTilkjentYtelseForBarnaTest {

    @Test
    fun `minimal vilkårsvurdering ett barn`() {
        val søker = søker født 19.nov(1995)
        val barn = barn født 14.des(2019)

        val vurdering = vilkårsvurdering der
            søker har
            (BOSATT_I_RIKET oppfylt 26.jan(2020)..uendelig) og
            (LOVLIG_OPPHOLD oppfylt 26.jan(2020)..uendelig) der
            barn har
            (UNDER_18_ÅR oppfylt 26.jan(2020)..barn.til18ÅrVilkårsdato()) og
            (GIFT_PARTNERSKAP oppfylt 26.jan(2020)..uendelig) og
            (BOR_MED_SØKER oppfylt 26.jan(2020)..uendelig) og
            (BOSATT_I_RIKET oppfylt 26.jan(2020)..uendelig) og
            (LOVLIG_OPPHOLD oppfylt 26.jan(2020)..uendelig)

        val forventedeAndeler = listOf(
            barn får alt av 1054 i feb(2020)..aug(2020),
            barn får alt av 1354 i sep(2020)..aug(2021),
            barn får alt av 1654 i sep(2021)..des(2021),
            barn får alt av 1676 i jan(2022)..nov(2025),
            barn får alt av 1054 i des(2025)..nov(2037)
        )

        assertEquals(forventedeAndeler, vurdering.beregnAndelerTilkjentYtelseForBarna())
    }

    @Test
    fun `minimal vilkårsvurdering ett barn og delt bosted`() {
        val søker = søker født 19.nov(1995)
        val barn = barn født 14.des(2019)

        val vurdering = vilkårsvurdering der
            søker har
            (BOSATT_I_RIKET oppfylt 26.jan(2020)..uendelig) og
            (LOVLIG_OPPHOLD oppfylt 26.jan(2020)..uendelig) der
            barn har
            (UNDER_18_ÅR oppfylt 26.jan(2020)..barn.til18ÅrVilkårsdato()) og
            (GIFT_PARTNERSKAP oppfylt 26.jan(2020)..uendelig) og
            (BOR_MED_SØKER oppfylt 26.jan(2020)..13.feb(2022) med DELT_BOSTED) og
            (BOR_MED_SØKER oppfylt 14.feb(2022)..uendelig) og
            (BOSATT_I_RIKET oppfylt 26.jan(2020)..uendelig) og
            (LOVLIG_OPPHOLD oppfylt 26.jan(2020)..uendelig)

        val forventedeAndeler = listOf(
            barn får halvparten av 1054 i feb(2020)..aug(2020),
            barn får halvparten av 1354 i sep(2020)..aug(2021),
            barn får halvparten av 1654 i sep(2021)..des(2021),
            barn får halvparten av 1676 i jan(2022)..feb(2022),
            barn får alt av 1676 i mar(2022)..nov(2025),
            barn får alt av 1054 i des(2025)..nov(2037)
        )

        assertEquals(forventedeAndeler, vurdering.beregnAndelerTilkjentYtelseForBarna())
    }

    @Test
    fun `Sjekk overgang fra oppfylt nasjonalt til oppfylt EØS dagen andre dag i måneden`() {
        val søker = søker født 19.nov(1995)
        val barn = barn født 14.des(2019)

        val vurdering = vilkårsvurdering der
            søker har
            (BOSATT_I_RIKET oppfylt 26.jan(2020)..1.mai(2021) etter NASJONALE_REGLER) og
            (BOSATT_I_RIKET oppfylt 2.mai(2021)..30.nov(2021) etter EØS_FORORDNINGEN) og
            (LOVLIG_OPPHOLD oppfylt 26.jan(2020)..1.mai(2021) etter NASJONALE_REGLER) og
            (LOVLIG_OPPHOLD oppfylt 2.mai(2021)..30.nov(2021) etter EØS_FORORDNINGEN) der
            barn har
            (UNDER_18_ÅR oppfylt 26.jan(2020)..30.nov(2021)) og
            (GIFT_PARTNERSKAP oppfylt 26.jan(2020)..30.nov(2021)) og
            (BOR_MED_SØKER oppfylt 26.jan(2020)..1.mai(2021) etter NASJONALE_REGLER) og
            (BOR_MED_SØKER oppfylt 2.mai(2021)..30.nov(2021) etter EØS_FORORDNINGEN) og
            (BOSATT_I_RIKET oppfylt 26.jan(2020)..1.mai(2021) etter NASJONALE_REGLER) og
            (BOSATT_I_RIKET oppfylt 2.mai(2021)..30.nov(2021) etter EØS_FORORDNINGEN) og
            (LOVLIG_OPPHOLD oppfylt 26.jan(2020)..1.mai(2021) etter NASJONALE_REGLER) og
            (LOVLIG_OPPHOLD oppfylt 2.mai(2021)..30.nov(2021) etter EØS_FORORDNINGEN)

        val forventedeAndeler = listOf(
            barn får alt av 1054 i feb(2020)..aug(2020),
            barn får alt av 1354 i sep(2020)..mai(2021),
            barn får alt av 1354 i jun(2021)..aug(2021),
            barn får alt av 1654 i sep(2021)..nov(2021)
        )

        assertEquals(forventedeAndeler, vurdering.beregnAndelerTilkjentYtelseForBarna())
    }

    /**
     * Litt overraskende tester
     */

    @Test
    fun `vilkårsvurdering ett barn som dør`() {
        val søker = søker født 19.nov(1995)
        val barn = barn født 14.des(2019) død 9.des(2024)

        val vurdering = vilkårsvurdering der
            søker har
            (BOSATT_I_RIKET oppfylt 26.jan(2020)..uendelig) og
            (LOVLIG_OPPHOLD oppfylt 26.jan(2020)..uendelig) der
            barn har
            (UNDER_18_ÅR oppfylt 26.jan(2020)..barn.til18ÅrVilkårsdato()) og
            (GIFT_PARTNERSKAP oppfylt 26.jan(2020)..uendelig) og
            (BOR_MED_SØKER oppfylt 26.jan(2020)..uendelig) og
            (BOSATT_I_RIKET oppfylt 26.jan(2020)..uendelig) og
            (LOVLIG_OPPHOLD oppfylt 26.jan(2020)..uendelig)

        val forventedeAndeler = listOf(
            barn får alt av 1054 i feb(2020)..aug(2020),
            barn får alt av 1354 i sep(2020)..aug(2021),
            barn får alt av 1654 i sep(2021)..des(2021),
            barn får alt av 1676 i jan(2022)..nov(2025),
            barn får alt av 1054 i des(2025)..des(2037)
        )

        assertEquals(forventedeAndeler, vurdering.beregnAndelerTilkjentYtelseForBarna())
    }
}

internal fun <T : Tidsenhet> VilkårsvurderingBuilder.PersonResultatBuilder<T>.beregnAndelerTilkjentYtelseForBarna(): List<BeregnetAndel> =
    TilkjentYtelseUtils.beregnAndelerTilkjentYtelseForBarna(
        this.byggPersonopplysningGrunnlag(),
        this.byggVilkårsvurdering()
    )
