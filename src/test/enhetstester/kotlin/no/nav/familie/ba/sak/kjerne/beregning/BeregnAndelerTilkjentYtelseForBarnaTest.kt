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
import no.nav.familie.ba.sak.kjerne.eøs.util.uendelig
import no.nav.familie.ba.sak.kjerne.eøs.util.under18år
import no.nav.familie.ba.sak.kjerne.eøs.util.vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tidsrom.rangeTo
import no.nav.familie.ba.sak.kjerne.tidslinje.util.VilkårsvurderingBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.apr
import no.nav.familie.ba.sak.kjerne.tidslinje.util.aug
import no.nav.familie.ba.sak.kjerne.tidslinje.util.des
import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jul
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jun
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mai
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mar
import no.nav.familie.ba.sak.kjerne.tidslinje.util.nov
import no.nav.familie.ba.sak.kjerne.tidslinje.util.okt
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
    fun `tom vilkårsvurdering gir ingen utbetalinger`() {
        assertEquals(emptyList<BeregnetAndel>(), vilkårsvurdering.beregnAndelerTilkjentYtelseForBarna())
    }

    @Test
    fun `minimal oppfylt vilkårsvurdering ett barn skal gi utbetalinger etter satsendringer`() {
        val søker = søker født 19.nov(1995)
        val barn = barn født 14.nov(2017)

        val vurdering = vilkårsvurdering der
            søker har
            (BOSATT_I_RIKET og LOVLIG_OPPHOLD oppfylt 26.jan(2018)..uendelig) der
            barn har
            (UNDER_18_ÅR oppfylt barn.under18år()) og
            (GIFT_PARTNERSKAP og BOR_MED_SØKER og BOSATT_I_RIKET og LOVLIG_OPPHOLD oppfylt 26.jan(2018)..uendelig)

        val forventedeAndeler = listOf(
            barn får alt av 970 i feb(2018)..feb(2019),
            barn får alt av 1054 i mar(2019)..aug(2020),
            barn får alt av 1354 i sep(2020)..aug(2021),
            barn får alt av 1654 i sep(2021)..des(2021),
            barn får alt av 1676 i jan(2022)..okt(2023),
            barn får alt av 1054 i nov(2023)..okt(2035)
        )

        assertEquals(forventedeAndeler, vurdering.beregnAndelerTilkjentYtelseForBarna())
    }

    @Test
    fun `minimal oppfylt vilkårsvurdering to barn skal gi utbetalinger etter satsendringer`() {
        val søker = søker født 19.nov(1995)
        val barn1 = barn født 14.nov(2017)
        val barn2 = barn født 1.mai(2013)

        val vurdering = vilkårsvurdering der
            søker har
            (BOSATT_I_RIKET og LOVLIG_OPPHOLD oppfylt 26.jan(2011)..uendelig) der
            barn1 har
            (UNDER_18_ÅR oppfylt barn1.under18år()) og
            (GIFT_PARTNERSKAP og BOR_MED_SØKER og BOSATT_I_RIKET og LOVLIG_OPPHOLD oppfylt 26.jan(2018)..uendelig) der
            barn2 har
            (UNDER_18_ÅR oppfylt barn2.under18år()) og
            (GIFT_PARTNERSKAP og BOR_MED_SØKER og BOSATT_I_RIKET og LOVLIG_OPPHOLD oppfylt 1.jun(2013)..uendelig)

        val forventedeAndeler = listOf(
            // barn 1
            barn1 får alt av 970 i feb(2018)..feb(2019),
            barn1 får alt av 1054 i mar(2019)..aug(2020),
            barn1 får alt av 1354 i sep(2020)..aug(2021),
            barn1 får alt av 1654 i sep(2021)..des(2021),
            barn1 får alt av 1676 i jan(2022)..okt(2023),
            barn1 får alt av 1054 i nov(2023)..okt(2035),
            // barn 2
            barn2 får alt av 970 i jun(2013)..feb(2019),
            barn2 får alt av 1054 i mar(2019)..apr(2031)
        )

        assertEquals(forventedeAndeler, vurdering.beregnAndelerTilkjentYtelseForBarna())
    }

    @Test
    fun `vilkårsvurdering med søker og ett barn der søker mangler vurdering av ett vilkår, skal ikke gi utbetalinger`() {
        val søker = søker født 19.nov(1995)
        val barn = barn født 14.des(2019)

        val vurdering = vilkårsvurdering der
            søker har
            (BOSATT_I_RIKET oppfylt 26.jan(2020)..uendelig) der
            // mangler LOVLIG_OPPHOLD
            barn har
            (UNDER_18_ÅR oppfylt barn.under18år()) og
            (GIFT_PARTNERSKAP og BOR_MED_SØKER og BOSATT_I_RIKET og LOVLIG_OPPHOLD oppfylt 26.jan(2020)..uendelig)

        assertEquals(emptyList<BeregnetAndel>(), vurdering.beregnAndelerTilkjentYtelseForBarna())
    }

    @Test
    fun `vilkårsvurdering med søker og ett barn der barn mangler vurdering av ett vilkår, skal ikke gi utbetalinger`() {
        val søker = søker født 19.nov(1995)
        val barn = barn født 14.des(2019)

        val vurdering = vilkårsvurdering der
            søker har
            (BOSATT_I_RIKET og LOVLIG_OPPHOLD oppfylt 26.jan(2020)..uendelig) der
            barn har
            (UNDER_18_ÅR oppfylt barn.under18år()) og
            // mangler LOVLIG_OPPHOLD
            (GIFT_PARTNERSKAP og BOR_MED_SØKER og BOSATT_I_RIKET oppfylt 26.jan(2020)..uendelig)

        assertEquals(emptyList<BeregnetAndel>(), vurdering.beregnAndelerTilkjentYtelseForBarna())
    }

    @Test
    fun `vilkårsvurdering der søkers vilkårsresultater ikke overlapper for noen vilkår, skal ikke gi utbetalinger`() {
        val søker = søker født 19.nov(1995)
        val barn = barn født 14.des(2019)

        val vurdering = vilkårsvurdering der
            søker har
            (BOSATT_I_RIKET oppfylt 26.jan(2020)..25.apr(2024)) og
            (LOVLIG_OPPHOLD oppfylt 26.apr(2024)..uendelig) der
            barn har
            (UNDER_18_ÅR oppfylt barn.under18år()) og
            (GIFT_PARTNERSKAP og BOR_MED_SØKER og BOSATT_I_RIKET og LOVLIG_OPPHOLD oppfylt 26.jan(2020)..uendelig)

        assertEquals(emptyList<BeregnetAndel>(), vurdering.beregnAndelerTilkjentYtelseForBarna())
    }

    @Test
    fun `vilkårsvurdering der barnet vilkårsresultater ikke overlapper for noen vilkår, skal ikke gi utbetalinger`() {
        val søker = søker født 19.nov(1995)
        val barn = barn født 14.des(2019)

        val vurdering = vilkårsvurdering der
            søker har
            (BOSATT_I_RIKET og LOVLIG_OPPHOLD oppfylt 26.jan(2020)..uendelig) der
            barn har
            (UNDER_18_ÅR oppfylt 26.jan(2020)..30.apr(2022)) og
            (GIFT_PARTNERSKAP oppfylt 1.mai(2022)..29.feb(2024)) og
            (BOR_MED_SØKER oppfylt 1.mar(2024)..31.jul(2027)) og
            (BOSATT_I_RIKET oppfylt 1.aug(2027)..31.des(2031)) og
            (LOVLIG_OPPHOLD oppfylt 1.jan(2032)..uendelig)

        assertEquals(emptyList<BeregnetAndel>(), vurdering.beregnAndelerTilkjentYtelseForBarna())
    }

    @Test
    fun `minimal vilkårsvurdering ett barn og delt bosted`() {
        val søker = søker født 19.nov(1995)
        val barn = barn født 14.des(2019)

        val vurdering = vilkårsvurdering der
            søker har
            (BOSATT_I_RIKET og LOVLIG_OPPHOLD oppfylt 26.jan(2020)..uendelig) der
            barn har
            (UNDER_18_ÅR oppfylt barn.under18år()) og
            (BOR_MED_SØKER oppfylt 26.jan(2020)..13.feb(2022) med DELT_BOSTED) og
            (BOR_MED_SØKER oppfylt 14.feb(2022)..uendelig) og
            (BOSATT_I_RIKET og LOVLIG_OPPHOLD og GIFT_PARTNERSKAP oppfylt 26.jan(2020)..uendelig)

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
            (UNDER_18_ÅR oppfylt barn.under18år()) og
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

internal fun <T : Tidsenhet> VilkårsvurderingBuilder<T>.beregnAndelerTilkjentYtelseForBarna(): List<BeregnetAndel> =
    TilkjentYtelseUtils.beregnAndelerTilkjentYtelseForBarnaDeprecated(
        this.byggPersonopplysningGrunnlag(),
        this.byggVilkårsvurdering()
    )

internal fun <T : Tidsenhet> VilkårsvurderingBuilder.PersonResultatBuilder<T>.beregnAndelerTilkjentYtelseForBarna(): List<BeregnetAndel> =
    TilkjentYtelseUtils.beregnAndelerTilkjentYtelseForBarnaDeprecated(
        this.byggPersonopplysningGrunnlag(),
        this.byggVilkårsvurdering()
    )
