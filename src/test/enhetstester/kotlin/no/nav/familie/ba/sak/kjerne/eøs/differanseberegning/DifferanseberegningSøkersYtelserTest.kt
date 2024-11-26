package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.util.TilkjentYtelseBuilder
import no.nav.familie.ba.sak.kjerne.eøs.util.barn
import no.nav.familie.ba.sak.kjerne.eøs.util.født
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.util.KompetanseBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.VilkårsvurderingBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.des
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jul
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jun
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DifferanseberegningSøkersYtelserTest {
    @Test
    fun `skal håndtere tre barn og utvidet barnetrygd og småbarnstillegg, der alle barna har underskudd i differanseberegning`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = barn født 13.des(2016)
        val barn2 = barn født 15.des(2017)
        val barn3 = barn født 9.des(2018)
        val barna = listOf(barn1, barn2, barn3)
        val behandling = lagBehandling()

        val kompetanser =
            KompetanseBuilder(jan(2017))
                //              |1 stk <3 år|2 stk <3 år|3 stk <3 år|2 stk <3 år|1 stk <3 år|
                //              |01-17      |01-18      |01-19      |01-20      |01-21      |01-22
                .medKompetanse("PPPPPPSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSPPPSSS", barn1)
                .medKompetanse("            SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSPPPPPPSSSSSSSSSSSSSSSSSSSSSSSS>", barn2)
                .medKompetanse("                        SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS>", barn3)
                .byggKompetanser()

        val tilkjentYtelse =
            TilkjentYtelseBuilder(jan(2017), behandling)
                .forPersoner(søker)
                //           |01-17      |01-18      |01-19      |01-20      |01-21      |01-22
                .medUtvidet("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$") { 1000 }
                .medSmåbarn("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$") { 1000 }
                .forPersoner(barn1)
                .medOrdinær("$$$$$$") { 1000 }
                .medOrdinær("      $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$", 100, { 1000 }, { -700 }) { 0 }
                .medOrdinær("                                                   $$$") { 1000 }
                .medOrdinær("                                                      $$$", 100, { 1000 }, { -700 }) { 0 }
                .forPersoner(barn2)
                .medOrdinær("            $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$", 100, { 1000 }, { -700 }) { 0 }
                .medOrdinær("                                          $$$$$$") { 1000 }
                .medOrdinær("                                                $$$>", 100, { 1000 }, { -700 }) { 0 }
                .forPersoner(barn3)
                .medOrdinær("                        $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$>", 100, { 1000 }, { -700 }) { 0 }
                .bygg()

        val personResultater =
            VilkårsvurderingBuilder<Måned>(behandling = behandling)
                .forPerson(barn1, des(2016))
                .medVilkår("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee", Vilkår.BOR_MED_SØKER)
                .forPerson(barn2, des(2017))
                .medVilkår("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee", Vilkår.BOR_MED_SØKER)
                .forPerson(barn3, des(2018))
                .medVilkår("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee", Vilkår.BOR_MED_SØKER)
                .byggVilkårsvurdering()
                .personResultater

        val nyeAndeler =
            tilkjentYtelse.andelerTilkjentYtelse.differanseberegnSøkersYtelser(barna, kompetanser, personResultater)

        val forventet =
            TilkjentYtelseBuilder(jan(2017), behandling)
                .forPersoner(søker)
                //           |01-17      |01-18      |01-19      |01-20      |01-21      |01-22
                .medUtvidet("$$$$$$") { 1000 }
                .medUtvidet("      $$$$$$", { 1000 }, { 300 }) { 300 }
                .medUtvidet("            $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$", { 1000 }, { 0 }) { 0 }
                .medUtvidet("                                          $$$$$$") { 1000 }
                .medUtvidet("                                                $$$", { 1000 }, { 0 }) { 0 }
                .medUtvidet("                                                   $$$") { 1000 }
                .medUtvidet("                                                      $$$$$$$$$$", { 1000 }, { 0 }) { 0 }
                .medSmåbarn("$$$$$$$$$$$$") { 1000 }
                .medSmåbarn("            $$$$$$$$$$$$", { 1000 }, { 600 }) { 600 }
                .medSmåbarn("                        $$$$$$$$$$$$", { 1000 }, { 0 }) { 0 }
                .medSmåbarn("                                    $$$$$$", { 1000 }, { 267 }) { 267 }
                .medSmåbarn("                                          $$$$$$") { 1000 }
                .medSmåbarn("                                                $$$", { 1000 }, { 633 }) { 633 }
                .medSmåbarn("                                                   $$$", { 1000 }, { 300 }) { 300 }
                .medSmåbarn("                                                      $$$", { 1000 }, { 633 }) { 633 }
                .medSmåbarn("                                                         $$$", { 1000 }, { 800 }) { 800 }
                .forPersoner(barn1)
                .medOrdinær("$$$$$$") { 1000 }
                .medOrdinær("      $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$", 100, { 1000 }, { -700 }) { 0 }
                .medOrdinær("                                                   $$$") { 1000 }
                .medOrdinær("                                                      $$$", 100, { 1000 }, { -700 }) { 0 }
                .forPersoner(barn2)
                .medOrdinær("            $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$", 100, { 1000 }, { -700 }) { 0 }
                .medOrdinær("                                          $$$$$$") { 1000 }
                .medOrdinær("                                                $$$>", 100, { 1000 }, { -700 }) { 0 }
                .forPersoner(barn3)
                .medOrdinær("                        $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$>", 100, { 1000 }, { -700 }) { 0 }
                .bygg()

        assertEquals(forventet.andelerTilkjentYtelse.sortert(), nyeAndeler.sortert())
    }

    @Test
    fun `differanseberegnet ordinær barnetrygd uten at søker har ytelser, skal gi uendrete andeler for barna`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = barn født 13.des(2016)
        val barn2 = barn født 15.des(2017)
        val barna = listOf(barn1, barn2)
        val behandling = lagBehandling()

        val kompetanser =
            KompetanseBuilder(jan(2017))
                //              |1 stk <3 år|2 stk <3 år|3 stk <3 år|2 stk <3 år|1 stk <3 år|
                //              |01-17      |01-18      |01-19      |01-20      |01-21      |01-22
                .medKompetanse("PPPPPPSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSPPPSSS", barn1)
                .medKompetanse("            SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSPPPPPPSSSSSSSSSSSSSSSSSSSSSSSS>", barn2)
                .byggKompetanser()

        val tilkjentYtelse =
            TilkjentYtelseBuilder(jan(2017), behandling)
                .forPersoner(søker)
                //           |01-17      |01-18      |01-19      |01-20      |01-21      |01-22
                .forPersoner(barn1)
                .medOrdinær("$$$$$$") { 1000 }
                .medOrdinær("      $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$", 100, { 1000 }, { -700 }) { 0 }
                .medOrdinær("                                                   $$$") { 1000 }
                .medOrdinær("                                                      $$$", 100, { 1000 }, { -700 }) { 0 }
                .forPersoner(barn2)
                .medOrdinær("            $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$", 100, { 1000 }, { -700 }) { 0 }
                .medOrdinær("                                          $$$$$$") { 1000 }
                .medOrdinær("                                                $$$>", 100, { 1000 }, { -700 }) { 0 }
                .bygg()

        val personResultater =
            VilkårsvurderingBuilder<Måned>(behandling = behandling)
                .forPerson(barn1, des(2016))
                .medVilkår("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee", Vilkår.BOR_MED_SØKER)
                .forPerson(barn2, des(2017))
                .medVilkår("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee", Vilkår.BOR_MED_SØKER)
                .byggVilkårsvurdering()
                .personResultater

        val nyeAndeler =
            tilkjentYtelse.andelerTilkjentYtelse.differanseberegnSøkersYtelser(barna, kompetanser, personResultater)

        assertEquals(tilkjentYtelse.andelerTilkjentYtelse.sortert(), nyeAndeler.sortert())
    }

    @Test
    fun `Ingen differranseberegning skal gi uendrete andeler`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = barn født 13.des(2016)
        val barn2 = barn født 15.des(2017)
        val barna = listOf(barn1, barn2)
        val behandling = lagBehandling()

        val kompetanser = emptyList<Kompetanse>()

        val tilkjentYtelse =
            TilkjentYtelseBuilder(jan(2017), behandling)
                .forPersoner(søker)
                //           |01-17      |01-18      |01-19      |01-20      |01-21      |01-22
                .medUtvidet("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$") { 1000 }
                .medSmåbarn("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$") { 1000 }
                .forPersoner(barn1)
                .medOrdinær("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$") { 1000 }
                .forPersoner(barn2)
                .medOrdinær("            $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$>") { 1000 }
                .bygg()

        val personResultater =
            VilkårsvurderingBuilder<Måned>(behandling = behandling)
                .forPerson(barn1, des(2016))
                .medVilkår("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee", Vilkår.BOR_MED_SØKER)
                .forPerson(barn2, des(2017))
                .medVilkår("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee", Vilkår.BOR_MED_SØKER)
                .byggVilkårsvurdering()
                .personResultater

        val nyeAndeler =
            tilkjentYtelse.andelerTilkjentYtelse.differanseberegnSøkersYtelser(barna, kompetanser, personResultater)

        assertEquals(tilkjentYtelse.andelerTilkjentYtelse.sortert(), nyeAndeler.sortert())
    }

    @Test
    fun `Tom tilkjent ytelse og ingen barn skal ikke gi feil`() {
        val tilkjentYtelse = lagInitiellTilkjentYtelse()

        val nyeAndeler =
            tilkjentYtelse.andelerTilkjentYtelse
                .differanseberegnSøkersYtelser(emptyList(), emptyList(), emptySet())

        assertEquals(emptyList<AndelTilkjentYtelse>(), nyeAndeler)
    }

    @Test
    fun `Søkers andel som har hatt differanseberegning, men ikke skal ha det lenger, skal fjerne differanseberegningen og slå sammen perioder som med sikkerhet kan slås sammen`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = barn født 13.des(2016)
        val barn2 = barn født 15.des(2017)
        val barn3 = barn født 9.des(2018)
        val barna = listOf(barn1, barn2, barn3)
        val behandling = lagBehandling()

        val kompetanser = emptyList<Kompetanse>()

        val tilkjentYtelse =
            TilkjentYtelseBuilder(jan(2017), behandling)
                .forPersoner(søker)
                //           |01-17      |01-18      |01-19      |01-20      |01-21      |01-22
                .medUtvidet("$$$$$$") { 1000 }
                .medUtvidet("      $$$$$$", { 1000 }, { 300 }) { 300 }
                .medUtvidet("            $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$", { 1000 }, { 0 }) { 0 }
                .medUtvidet("                                          $$$$$$") { 1000 }
                .medUtvidet("                                                $$$", { 1000 }, { 0 }) { 0 }
                .medUtvidet("                                                   $$$") { 1000 }
                .medUtvidet("                                                      $$$$$$$$$$", { 1000 }, { 0 }) { 0 }
                .medSmåbarn("$$$$$$$$$$$$") { 1000 }
                .medSmåbarn("            $$$$$$$$$$$$", { 1000 }, { 600 }) { 600 }
                .medSmåbarn("                        $$$$$$$$$$$$", { 1000 }, { 0 }) { 0 }
                .medSmåbarn("                                    $$$$$$", { 1000 }, { 267 }) { 267 }
                .medSmåbarn("                                          $$$$$$") { 1000 }
                .medSmåbarn("                                                $$$", { 1000 }, { 633 }) { 633 }
                .medSmåbarn("                                                   $$$") { 1000 }
                .medSmåbarn("                                                      $$$", { 1000 }, { 633 }) { 633 }
                .medSmåbarn("                                                         $$$", { 1000 }, { 800 }) { 800 }
                .forPersoner(barn1)
                .medOrdinær("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$") { 1000 }
                .forPersoner(barn2)
                .medOrdinær("            $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$>") { 1000 }
                .forPersoner(barn3)
                .medOrdinær("                        $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$>") { 1000 }
                .bygg()

        val personResultater =
            VilkårsvurderingBuilder<Måned>(behandling = behandling)
                .forPerson(barn1, des(2016))
                .medVilkår("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee", Vilkår.BOR_MED_SØKER)
                .forPerson(barn2, des(2017))
                .medVilkår("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee", Vilkår.BOR_MED_SØKER)
                .forPerson(barn3, des(2018))
                .medVilkår("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee", Vilkår.BOR_MED_SØKER)
                .byggVilkårsvurdering()
                .personResultater

        val nyeAndeler =
            tilkjentYtelse.andelerTilkjentYtelse.differanseberegnSøkersYtelser(barna, kompetanser, personResultater)

        // Dette er litt trist. Men selv om andelene er identiske, kan de ikke slås sammen fordi
        // de er til forveksling like som andeler som har en funksjonell årsak til å være splittet
        // Påfølgende andeler som begge har differanseberegning, KAN slås sammen
        val forventet =
            TilkjentYtelseBuilder(jan(2017), behandling)
                .forPersoner(søker)
                //           |01-17      |01-18      |01-19      |01-20      |01-21      |01-22
                .medUtvidet("$$$$$$") { 1000 }
                .medUtvidet("      $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$") { 1000 }
                .medUtvidet("                                          $$$$$$") { 1000 }
                .medUtvidet("                                                $$$") { 1000 }
                .medUtvidet("                                                   $$$") { 1000 }
                .medUtvidet("                                                      $$$$$$$$$$") { 1000 }
                .medSmåbarn("$$$$$$$$$$$$") { 1000 }
                .medSmåbarn("            $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$") { 1000 }
                .medSmåbarn("                                          $$$$$$") { 1000 }
                .medSmåbarn("                                                $$$") { 1000 }
                .medSmåbarn("                                                   $$$") { 1000 }
                .medSmåbarn("                                                      $$$$$$") { 1000 }
                .forPersoner(barn1)
                .medOrdinær("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$") { 1000 }
                .forPersoner(barn2)
                .medOrdinær("            $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$>") { 1000 }
                .forPersoner(barn3)
                .medOrdinær("                        $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$>") { 1000 }
                .bygg()

        assertEquals(forventet.andelerTilkjentYtelse.sortert(), nyeAndeler.sortert())
    }

    @Test
    fun `Søkers andel som har differanseberegning, men underskuddet reduseres, skal få oppdatert differanseberegningen`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = barn født 13.des(2016)
        val barna = listOf(barn1)
        val behandling = lagBehandling()

        val kompetanser =
            KompetanseBuilder(jan(2017))
                .medKompetanse("S>", barn1)
                .byggKompetanser()

        val tilkjentYtelse =
            TilkjentYtelseBuilder(jan(2017), behandling)
                .forPersoner(søker)
                .medUtvidet("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$", { 1000 }, { 0 }) { 0 }
                .medSmåbarn("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$", { 1000 }, { 300 }) { 300 }
                .forPersoner(barn1)
                .medOrdinær("$>", 100, { 1000 }, { -650 }) { 0 }
                .bygg()

        val personResultater =
            VilkårsvurderingBuilder<Måned>(behandling = behandling)
                .forPerson(barn1, des(2016))
                .medVilkår("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee", Vilkår.BOR_MED_SØKER)
                .byggVilkårsvurdering()
                .personResultater

        val nyeAndeler =
            tilkjentYtelse.andelerTilkjentYtelse.differanseberegnSøkersYtelser(barna, kompetanser, personResultater)

        val forventet =
            TilkjentYtelseBuilder(jan(2017), behandling)
                .forPersoner(søker)
                .medUtvidet("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$", { 1000 }, { 350 }) { 350 }
                .medSmåbarn("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$") { 1000 }
                .forPersoner(barn1)
                .medOrdinær("$>", 100, { 1000 }, { -650 }) { 0 }
                .bygg()

        assertEquals(forventet.andelerTilkjentYtelse.sortert(), nyeAndeler.sortert())
    }

    @Test
    fun `Skal tåle perioder der underskuddet på differanseberegning er større enn alle tilkjente ytelser`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = barn født 13.des(2016)
        val barna = listOf(barn1)
        val behandling = lagBehandling()

        val kompetanser =
            KompetanseBuilder(jan(2017))
                .medKompetanse("S>", barn1)
                .byggKompetanser()

        val tilkjentYtelse =
            TilkjentYtelseBuilder(jan(2017), behandling)
                .forPersoner(søker)
                .medUtvidet("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$") { 1000 }
                .medSmåbarn("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$") { 1000 }
                .forPersoner(barn1)
                .medOrdinær("$>", 100, { 1000 }, { -2650 }) { 0 }
                .bygg()

        val personResultater =
            VilkårsvurderingBuilder<Måned>(behandling = behandling)
                .forPerson(barn1, des(2016))
                .medVilkår("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee", Vilkår.BOR_MED_SØKER)
                .byggVilkårsvurdering()
                .personResultater

        val nyeAndeler =
            tilkjentYtelse.andelerTilkjentYtelse.differanseberegnSøkersYtelser(barna, kompetanser, personResultater)

        val forventet =
            TilkjentYtelseBuilder(jan(2017), behandling)
                .forPersoner(søker)
                .medUtvidet("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$", { 1000 }, { 0 }) { 0 }
                .medSmåbarn("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$", { 1000 }, { 0 }) { 0 }
                .forPersoner(barn1)
                .medOrdinær("$>", 100, { 1000 }, { -2650 }) { 0 }
                .bygg()

        assertEquals(forventet.andelerTilkjentYtelse.sortert(), nyeAndeler.sortert())
    }

    @Test
    fun `skal illustrere avrundingssproblematikk, der søker tjener`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = barn født 13.des(2016)
        val barn2 = barn født 15.des(2017)
        val barn3 = barn født 9.des(2018)
        val barna = listOf(barn1, barn2, barn3)
        val behandling = lagBehandling()

        val kompetanser =
            KompetanseBuilder(jul(2020))
                .medKompetanse("SSSSSS", barn1, barn2, barn3)
                .byggKompetanser()

        val tilkjentYtelse =
            TilkjentYtelseBuilder(jul(2020), behandling)
                .forPersoner(søker)
                .medUtvidet("$$$$$$") { 1054 }
                .forPersoner(barn1)
                .medOrdinær("$$$$$$", 100, { 1054 }, { -400 }) { 0 }
                .forPersoner(barn2, barn3)
                .medOrdinær("$$$$$$", 100, { 1054 }, { 554 }) { 554 }
                .bygg()

        val personResultater =
            VilkårsvurderingBuilder<Måned>(behandling = behandling)
                .forPerson(barn1, jun(2020))
                .medVilkår("eeeeeee", Vilkår.BOR_MED_SØKER)
                .forPerson(barn2, jun(2020))
                .medVilkår("eeeeeee", Vilkår.BOR_MED_SØKER)
                .forPerson(barn3, jun(2020))
                .medVilkår("eeeeeee", Vilkår.BOR_MED_SØKER)
                .byggVilkårsvurdering()
                .personResultater

        val nyeAndeler =
            tilkjentYtelse.andelerTilkjentYtelse.differanseberegnSøkersYtelser(barna, kompetanser, personResultater)

        val forventet =
            TilkjentYtelseBuilder(jul(2020), behandling)
                .forPersoner(søker)
                .medUtvidet("$$$$$$", { 1054 }, { 703 }) { 703 } // Egentlig 702,67
                .forPersoner(barn1)
                .medOrdinær("$$$$$$", 100, { 1054 }, { -400 }) { 0 }
                .forPersoner(barn2, barn3)
                .medOrdinær("$$$$$$", 100, { 1054 }, { 554 }) { 554 }
                .bygg()

        assertEquals(forventet.andelerTilkjentYtelse.sortert(), nyeAndeler.sortert())
    }

    @Test
    fun `skal illustrere avrundingssproblematikk, der søker taper`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = barn født 13.des(2016)
        val barn2 = barn født 15.des(2017)
        val barn3 = barn født 9.des(2018)
        val barna = listOf(barn1, barn2, barn3)
        val behandling = lagBehandling()

        val kompetanser =
            KompetanseBuilder(jul(2020))
                .medKompetanse("SSSSSS", barn1, barn2, barn3)
                .byggKompetanser()

        val tilkjentYtelse =
            TilkjentYtelseBuilder(jul(2020), behandling)
                .forPersoner(søker)
                .medUtvidet("$$$$$$") { 1054 }
                .forPersoner(barn1, barn2)
                .medOrdinær("$$$$$$", 100, { 1054 }, { -400 }) { 0 }
                .forPersoner(barn3)
                .medOrdinær("$$$$$$", 100, { 1054 }, { 554 }) { 554 }
                .bygg()

        val personResultater =
            VilkårsvurderingBuilder<Måned>(behandling = behandling)
                .forPerson(barn1, jun(2020))
                .medVilkår("eeeeeee", Vilkår.BOR_MED_SØKER)
                .forPerson(barn2, jun(2020))
                .medVilkår("eeeeeee", Vilkår.BOR_MED_SØKER)
                .forPerson(barn3, jun(2020))
                .medVilkår("eeeeeee", Vilkår.BOR_MED_SØKER)
                .byggVilkårsvurdering()
                .personResultater

        val nyeAndeler =
            tilkjentYtelse.andelerTilkjentYtelse.differanseberegnSøkersYtelser(barna, kompetanser, personResultater)

        val forventet =
            TilkjentYtelseBuilder(jul(2020), behandling)
                .forPersoner(søker)
                .medUtvidet("$$$$$$", { 1054 }, { 351 }) { 351 } // Egentlig 351,33
                .forPersoner(barn1, barn2)
                .medOrdinær("$$$$$$", 100, { 1054 }, { -400 }) { 0 }
                .forPersoner(barn3)
                .medOrdinær("$$$$$$", 100, { 1054 }, { 554 }) { 554 }
                .bygg()

        assertEquals(forventet.andelerTilkjentYtelse.sortert(), nyeAndeler.sortert())
    }

    @Test
    fun `skal differanseberegne utvidet i perioder med sekundærlandsbarn og primærlandsbarn som bor i EØS land med annen forelder`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = barn født 13.des(2016)
        val barn2 = barn født 15.des(2017)
        val barna = listOf(barn1, barn2)
        val behandling = lagBehandling()

        // Ett Sekundærlandsbarn og ett primærlandsbarn
        val kompetanser =
            KompetanseBuilder(jan(2017))
                //                 |--- 2017---|--- 2018---|
                .medKompetanse("SSSSSSSSSSSSSSSSSSSSSSSS", barn1)
                .medKompetanse("            PPPPPPPPPPPP", barn2)
                .byggKompetanser()

        // Søker har utvidet barnetrygd og barna har ordinær
        val tilkjenteYtelserEtterDifferanseberegningForBarna =
            TilkjentYtelseBuilder(jan(2017), behandling)
                .forPersoner(søker)
                //              |--- 2017---|--- 2018---|
                .medUtvidet("$$$$$$$$$$$$$$$$$$$$$$$$", nasjonalt = { 1000 })
                .forPersoner(barn1)
                .medOrdinær("$$$$$$$$$$$$$$$$$$$$$$$$", nasjonalt = { 1000 }, differanse = { -700 }, kalkulert = { 0 })
                .forPersoner(barn2)
                .medOrdinær("            $$$$$$$$$$$$", nasjonalt = { 1000 }, kalkulert = { 1000 })
                .bygg()

        // Primærlandsbarnet har utdypende vilkårsvurdering 'Bor i EØS med annen forelder' i hele perioden
        val personResultater =
            VilkårsvurderingBuilder<Måned>(behandling = behandling)
                .forPerson(barn1, des(2016))
                .medVilkår("eeeeeeeeeeeeeeeeeeeeeeeee", Vilkår.BOR_MED_SØKER)
                .forPerson(barn2, des(2017))
                //                                 |--- 2018---|
                .medUtdypendeVilkårsvurdering("$$$$$$$$$$$$$", Vilkår.BOR_MED_SØKER, UtdypendeVilkårsvurdering.BARN_BOR_I_EØS_MED_ANNEN_FORELDER)
                .byggVilkårsvurdering()
                .personResultater

        val tilkjenteYtelserEtterDiffernanseberegningForBarnaOgSøker =
            tilkjenteYtelserEtterDifferanseberegningForBarna.andelerTilkjentYtelse.differanseberegnSøkersYtelser(barna, kompetanser, personResultater)

        val forventet =
            TilkjentYtelseBuilder(jan(2017), behandling)
                .forPersoner(søker)
                //              |--- 2017---|--- 2018---|
                .medUtvidet("$$$$$$$$$$$$$$$$$$$$$$$$", nasjonalt = { 1000 }, differanse = { 300 }, kalkulert = { 300 })
                .forPersoner(barn1)
                .medOrdinær("$$$$$$$$$$$$$$$$$$$$$$$$", nasjonalt = { 1000 }, differanse = { -700 }, kalkulert = { 0 })
                .forPersoner(barn2)
                .medOrdinær("            $$$$$$$$$$$$", nasjonalt = { 1000 }, kalkulert = { 1000 })
                .bygg()

        assertEquals(forventet.andelerTilkjentYtelse.sortert(), tilkjenteYtelserEtterDiffernanseberegningForBarnaOgSøker.sortert())
    }

    @Test
    fun `skal differanseberegne utvidet i perioder med sekundærlandsbarn og primærlandsbarn som bor i Storbritannia med annen forelder`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = barn født 13.des(2016)
        val barn2 = barn født 15.des(2017)
        val barna = listOf(barn1, barn2)
        val behandling = lagBehandling()

        // Ett Sekundærlandsbarn og ett primærlandsbarn
        val kompetanser =
            KompetanseBuilder(jan(2017))
                //                 |--- 2017---|--- 2018---|
                .medKompetanse("SSSSSSSSSSSSSSSSSSSSSSSS", barn1)
                .medKompetanse("            PPPPPPPPPPPP", barn2)
                .byggKompetanser()

        // Søker har utvidet barnetrygd og barna har ordinær
        val tilkjenteYtelserEtterDifferanseberegningForBarna =
            TilkjentYtelseBuilder(jan(2017), behandling)
                .forPersoner(søker)
                //              |--- 2017---|--- 2018---|
                .medUtvidet("$$$$$$$$$$$$$$$$$$$$$$$$", nasjonalt = { 1000 })
                .forPersoner(barn1)
                .medOrdinær("$$$$$$$$$$$$$$$$$$$$$$$$", nasjonalt = { 1000 }, differanse = { -700 }, kalkulert = { 0 })
                .forPersoner(barn2)
                .medOrdinær("            $$$$$$$$$$$$", nasjonalt = { 1000 }, kalkulert = { 1000 })
                .bygg()

        // Primærlandsbarnet har utdypende vilkårsvurdering 'Bor i Storbritannia med annen forelder' i hele perioden
        val personResultater =
            VilkårsvurderingBuilder<Måned>(behandling = behandling)
                .forPerson(barn1, des(2016))
                .medVilkår("eeeeeeeeeeeeeeeeeeeeeeeee", Vilkår.BOR_MED_SØKER)
                .forPerson(barn2, des(2017))
                //                                |--- 2018---|
                .medUtdypendeVilkårsvurdering("$$$$$$$$$$$$$", Vilkår.BOR_MED_SØKER, UtdypendeVilkårsvurdering.BARN_BOR_I_STORBRITANNIA_MED_ANNEN_FORELDER)
                .byggVilkårsvurdering()
                .personResultater

        val tilkjenteYtelserEtterDiffernanseberegningForBarnaOgSøker =
            tilkjenteYtelserEtterDifferanseberegningForBarna.andelerTilkjentYtelse.differanseberegnSøkersYtelser(barna, kompetanser, personResultater)

        val forventet =
            TilkjentYtelseBuilder(jan(2017), behandling)
                .forPersoner(søker)
                //              |--- 2017---|--- 2018---|
                .medUtvidet("$$$$$$$$$$$$$$$$$$$$$$$$", nasjonalt = { 1000 }, differanse = { 300 }, kalkulert = { 300 })
                .forPersoner(barn1)
                .medOrdinær("$$$$$$$$$$$$$$$$$$$$$$$$", nasjonalt = { 1000 }, differanse = { -700 }, kalkulert = { 0 })
                .forPersoner(barn2)
                .medOrdinær("            $$$$$$$$$$$$", nasjonalt = { 1000 }, kalkulert = { 1000 })
                .bygg()

        assertEquals(forventet.andelerTilkjentYtelse.sortert(), tilkjenteYtelserEtterDiffernanseberegningForBarnaOgSøker.sortert())
    }

    @Test
    fun `skal ikke differanseberegne utvidet i perioder med sekundærlandsbarn og primærlandsbarn uten EØS og Storbritannia krav`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = barn født 13.des(2016)
        val barn2 = barn født 15.des(2017)
        val barna = listOf(barn1, barn2)
        val behandling = lagBehandling()

        // Ett Sekundærlandsbarn og ett primærlandsbarn
        val kompetanser =
            KompetanseBuilder(jan(2017))
                //                 |--- 2017---|--- 2018---|
                .medKompetanse("SSSSSSSSSSSSSSSSSSSSSSSS", barn1)
                .medKompetanse("            PPPPPPPPPPPP", barn2)
                .byggKompetanser()

        // Søker har utvidet barnetrygd og barna har ordinær
        val tilkjenteYtelserEtterDifferanseberegningForBarna =
            TilkjentYtelseBuilder(jan(2017), behandling)
                .forPersoner(søker)
                //              |--- 2017---|--- 2018---|
                .medUtvidet("$$$$$$$$$$$$$$$$$$$$$$$$", nasjonalt = { 1000 })
                .forPersoner(barn1)
                .medOrdinær("$$$$$$$$$$$$$$$$$$$$$$$$", nasjonalt = { 1000 }, differanse = { -700 }, kalkulert = { 0 })
                .forPersoner(barn2)
                .medOrdinær("            $$$$$$$$$$$$", nasjonalt = { 1000 }, kalkulert = { 1000 })
                .bygg()

        // Primærlandsbarnet har ikke utdypende vilkårsvurdering for BOR_MED_SØKER vilkåret
        val personResultater =
            VilkårsvurderingBuilder<Måned>(behandling = behandling)
                //             |--- 2017---|--- 2018---|
                .forPerson(barn1, des(2016))
                .medVilkår("eeeeeeeeeeeeeeeeeeeeeeeee", Vilkår.BOR_MED_SØKER)
                .forPerson(barn2, des(2017))
                .medVilkår("eeeeeeeeeeeee", Vilkår.BOR_MED_SØKER)
                .byggVilkårsvurdering()
                .personResultater

        val tilkjenteYtelserEtterDiffernanseberegningForBarnaOgSøker =
            tilkjenteYtelserEtterDifferanseberegningForBarna.andelerTilkjentYtelse.differanseberegnSøkersYtelser(barna, kompetanser, personResultater)

        val forventet =
            TilkjentYtelseBuilder(jan(2017), behandling)
                .forPersoner(søker)
                //              |--- 2017---|--- 2018---|
                .medUtvidet("$$$$$$$$$$$$            ", nasjonalt = { 1000 }, differanse = { 300 }, kalkulert = { 300 })
                .medUtvidet("            $$$$$$$$$$$$", nasjonalt = { 1000 }, kalkulert = { 1000 })
                .forPersoner(barn1)
                .medOrdinær("$$$$$$$$$$$$$$$$$$$$$$$$", nasjonalt = { 1000 }, differanse = { -700 }, kalkulert = { 0 })
                .forPersoner(barn2)
                .medOrdinær("            $$$$$$$$$$$$", nasjonalt = { 1000 }, kalkulert = { 1000 })
                .bygg()

        assertEquals(forventet.andelerTilkjentYtelse.sortert(), tilkjenteYtelserEtterDiffernanseberegningForBarnaOgSøker.sortert())
    }

    @Test
    fun `skal ikke differanseberegne utvidet i perioder med sekundærlandsbarn, primærlandsbarn med EØS eller Storbritannia krav og vanlig primærlandsbarn`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = barn født 13.des(2016)
        val barn2 = barn født 15.des(2017)
        val barn3 = barn født 13.des(2016)
        val barna = listOf(barn1, barn2, barn3)
        val behandling = lagBehandling()

        // Ett Sekundærlandsbarn og to primærlandsbarn
        val kompetanser =
            KompetanseBuilder(jan(2017))
                //                 |--- 2017---|--- 2018---|
                .medKompetanse("SSSSSSSSSSSSSSSSSSSSSSSS", barn1)
                .medKompetanse("            PPPPPPPPPPPP", barn2)
                .medKompetanse("PPPPPPPPPPPPPPPPPPPPPPPP", barn3)
                .byggKompetanser()

        // Søker har utvidet barnetrygd og barna har ordinær
        val tilkjenteYtelserEtterDifferanseberegningForBarna =
            TilkjentYtelseBuilder(jan(2017), behandling)
                .forPersoner(søker)
                //              |--- 2017---|--- 2018---|
                .medUtvidet("$$$$$$$$$$$$$$$$$$$$$$$$", nasjonalt = { 1000 })
                .forPersoner(barn1)
                .medOrdinær("$$$$$$$$$$$$$$$$$$$$$$$$", nasjonalt = { 1000 }, differanse = { -700 }, kalkulert = { 0 })
                .forPersoner(barn2)
                .medOrdinær("            $$$$$$$$$$$$", nasjonalt = { 1000 }, kalkulert = { 1000 })
                .forPersoner(barn3)
                .medOrdinær("$$$$$$$$$$$$$$$$$$$$$$$$", nasjonalt = { 1000 }, kalkulert = { 1000 })
                .bygg()

        // Det ene primærlandsbarnet har utdypende vilkårsvurdering 'Bor i EØS med annen forelder' i hele perioden
        val personResultater =
            VilkårsvurderingBuilder<Måned>(behandling = behandling)
                .forPerson(barn1, des(2016))
                .medVilkår("eeeeeeeeeeeeeeeeeeeeeeeee", Vilkår.BOR_MED_SØKER)
                .forPerson(barn2, des(2017))
                //             |--- 2018---|
                .medUtdypendeVilkårsvurdering("$$$$$$$$$$$$$", Vilkår.BOR_MED_SØKER, UtdypendeVilkårsvurdering.BARN_BOR_I_EØS_MED_ANNEN_FORELDER)
                .forPerson(barn3, des(2016))
                .medVilkår("eeeeeeeeeeeeeeeeeeeeeeeee", Vilkår.BOR_MED_SØKER)
                .byggVilkårsvurdering()
                .personResultater

        val tilkjenteYtelserEtterDiffernanseberegningForBarnaOgSøker =
            tilkjenteYtelserEtterDifferanseberegningForBarna.andelerTilkjentYtelse.differanseberegnSøkersYtelser(barna, kompetanser, personResultater)

        val forventet =
            TilkjentYtelseBuilder(jan(2017), behandling)
                .forPersoner(søker)
                //              |--- 2017---|--- 2018---|
                .medUtvidet("$$$$$$$$$$$$$$$$$$$$$$$$", nasjonalt = { 1000 }, kalkulert = { 1000 })
                .forPersoner(barn1)
                .medOrdinær("$$$$$$$$$$$$$$$$$$$$$$$$", nasjonalt = { 1000 }, differanse = { -700 }, kalkulert = { 0 })
                .forPersoner(barn2)
                .medOrdinær("            $$$$$$$$$$$$", nasjonalt = { 1000 }, kalkulert = { 1000 })
                .forPersoner(barn3)
                .medOrdinær("$$$$$$$$$$$$$$$$$$$$$$$$", nasjonalt = { 1000 }, kalkulert = { 1000 })
                .bygg()

        assertEquals(forventet.andelerTilkjentYtelse.sortert(), tilkjenteYtelserEtterDiffernanseberegningForBarnaOgSøker.sortert())
    }

    @Test
    fun `skal kun differanseberegne utvidet i rene sekundærlandperioder eller i kombinasjon med primærlandsbarn med EØS eller Storbritannia krav`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = barn født 13.des(2016)
        val barn2 = barn født 15.des(2017)
        val barn3 = barn født 13.des(2016)
        val barna = listOf(barn1, barn2, barn3)
        val behandling = lagBehandling()

        // Ett Sekundærlandsbarn og to primærlandsbarn
        val kompetanser =
            KompetanseBuilder(jan(2017))
                //                 |--- 2017---|--- 2018---|
                .medKompetanse("SSSSSSSSSSSSSSSSSSSSSSSS", barn1)
                .medKompetanse("            PPPPPPPPPPPP", barn2)
                .medKompetanse("PPPPSSPPPPPPPPPPPSSPPPPP", barn3)
                .byggKompetanser()

        // Søker har utvidet barnetrygd og barna har ordinær
        val tilkjenteYtelserEtterDifferanseberegningForBarna =
            TilkjentYtelseBuilder(jan(2017), behandling)
                .forPersoner(søker)
                //              |--- 2017---|--- 2018---|
                .medUtvidet("$$$$$$$$$$$$$$$$$$$$$$$$", nasjonalt = { 1000 })
                .forPersoner(barn1)
                .medOrdinær("$$$$$$$$$$$$$$$$$$$$$$$$", nasjonalt = { 1000 }, differanse = { -700 }, kalkulert = { 0 })
                .forPersoner(barn2)
                .medOrdinær("            $$$$$$$$$$$$", nasjonalt = { 1000 }, kalkulert = { 1000 })
                .forPersoner(barn3)
                .medOrdinær("$$$$", nasjonalt = { 1000 }, kalkulert = { 1000 })
                .medOrdinær("    $$", nasjonalt = { 1000 }, differanse = { -700 }, kalkulert = { 0 })
                .medOrdinær("      $$$$$$$$$$$", nasjonalt = { 1000 }, kalkulert = { 1000 })
                .medOrdinær("                 $$", nasjonalt = { 1000 }, differanse = { -700 }, kalkulert = { 0 })
                .medOrdinær("                   $$$$$", nasjonalt = { 1000 }, kalkulert = { 1000 })
                .bygg()

        // Det ene primærlandsbarnet har utdypende vilkårsvurdering 'Bor i EØS med annen forelder' i hele perioden
        val personResultater =
            VilkårsvurderingBuilder<Måned>(behandling = behandling)
                .forPerson(barn1, des(2016))
                .medVilkår("eeeeeeeeeeeeeeeeeeeeeeeee", Vilkår.BOR_MED_SØKER)
                .forPerson(barn2, des(2017))
                //             |--- 2018---|
                .medUtdypendeVilkårsvurdering("$$$$$$$$$$$$$", Vilkår.BOR_MED_SØKER, UtdypendeVilkårsvurdering.BARN_BOR_I_EØS_MED_ANNEN_FORELDER)
                .forPerson(barn3, des(2016))
                .medVilkår("eeeeeeeeeeeeeeeeeeeeeeeee", Vilkår.BOR_MED_SØKER)
                .byggVilkårsvurdering()
                .personResultater

        val tilkjenteYtelserEtterDiffernanseberegningForBarnaOgSøker =
            tilkjenteYtelserEtterDifferanseberegningForBarna.andelerTilkjentYtelse.differanseberegnSøkersYtelser(barna, kompetanser, personResultater)

        val forventet =
            TilkjentYtelseBuilder(jan(2017), behandling)
                .forPersoner(søker)
                //              |--- 2017---|--- 2018---|
                .medUtvidet("$$$$", nasjonalt = { 1000 }, kalkulert = { 1000 })
                .medUtvidet("    $$", nasjonalt = { 1000 }, differanse = { 0 }, kalkulert = { 0 })
                .medUtvidet("      $$$$$$$$$$$", nasjonalt = { 1000 }, kalkulert = { 1000 })
                .medUtvidet("                 $$", nasjonalt = { 1000 }, differanse = { 0 }, kalkulert = { 0 })
                .medUtvidet("                   $$$$$", nasjonalt = { 1000 }, kalkulert = { 1000 })
                .forPersoner(barn1)
                .medOrdinær("$$$$$$$$$$$$$$$$$$$$$$$$", nasjonalt = { 1000 }, differanse = { -700 }, kalkulert = { 0 })
                .forPersoner(barn2)
                .medOrdinær("            $$$$$$$$$$$$", nasjonalt = { 1000 }, kalkulert = { 1000 })
                .forPersoner(barn3)
                .medOrdinær("$$$$", nasjonalt = { 1000 }, kalkulert = { 1000 })
                .medOrdinær("    $$", nasjonalt = { 1000 }, differanse = { -700 }, kalkulert = { 0 })
                .medOrdinær("      $$$$$$$$$$$", nasjonalt = { 1000 }, kalkulert = { 1000 })
                .medOrdinær("                 $$", nasjonalt = { 1000 }, differanse = { -700 }, kalkulert = { 0 })
                .medOrdinær("                   $$$$$", nasjonalt = { 1000 }, kalkulert = { 1000 })
                .bygg()

        assertEquals(forventet.andelerTilkjentYtelse.sortert(), tilkjenteYtelserEtterDiffernanseberegningForBarnaOgSøker.sortert())
    }

    // https://confluence.adeo.no/display/TFA/Differanseberegning
    @Test
    fun `eksempel-scenario fra confluence`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = barn født 1.jan(2016)
        val barn2 = barn født 1.jan(2019)
        val barna = listOf(barn1, barn2)
        val behandling = lagBehandling()

        // To sekundærlandsbarn
        val kompetanser =
            KompetanseBuilder(jan(2016))
                //                 |--- 2016---|--- 2017---|--- 2018---|--- 2019---|
                .medKompetanse(" SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", barn1)
                .medKompetanse("                                     SSSSSSSSSSS", barn2)
                .byggKompetanser()

        // Søker har utvidet barnetrygd og barna har ordinær. Det ene barnet har fått for mye fra det andre landet. Det andre har fått for lite.
        val tilkjenteYtelserEtterDifferanseberegningForBarna =
            TilkjentYtelseBuilder(jan(2016), behandling)
                .forPersoner(søker)
                //              |--- 2016---|--- 2017---|--- 2018---|--- 2019---|
                .medUtvidet(" $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$", nasjonalt = { 1054 }, kalkulert = { 1054 })
                .medSmåbarn("                                     $$$$$$$$$$$", nasjonalt = { 660 }, kalkulert = { 660 })
                .forPersoner(barn1)
                .medOrdinær(" $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$", nasjonalt = { 1676 }, differanse = { -446 }, kalkulert = { 0 })
                .forPersoner(barn2)
                .medOrdinær("                                     $$$$$$$$$$$", nasjonalt = { 1676 }, differanse = { 400 }, kalkulert = { 400 })
                .bygg()

        val personResultater =
            VilkårsvurderingBuilder<Måned>(behandling = behandling)
                //             |--- 2017---|--- 2018---|
                .forPerson(barn1, jan(2016))
                .medVilkår("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee", Vilkår.BOR_MED_SØKER)
                .forPerson(barn2, des(2017))
                .medVilkår("eeeeeeeeeeeee", Vilkår.BOR_MED_SØKER)
                .byggVilkårsvurdering()
                .personResultater

        val tilkjenteYtelserEtterDiffernanseberegningForBarnaOgSøker =
            tilkjenteYtelserEtterDifferanseberegningForBarna.andelerTilkjentYtelse.differanseberegnSøkersYtelser(barna, kompetanser, personResultater)

        val forventet =
            TilkjentYtelseBuilder(jan(2016), behandling)
                .forPersoner(søker)
                //              |--- 2016---|--- 2017---|--- 2018---|--- 2019---|
                .medUtvidet(" $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$", nasjonalt = { 1054 }, differanse = { 608 }, kalkulert = { 608 })
                .medSmåbarn("                                     $$$$$$$$$$$", nasjonalt = { 660 })
                .forPersoner(barn1)
                .medOrdinær(" $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$", nasjonalt = { 1676 }, differanse = { -446 }, kalkulert = { 0 })
                .forPersoner(barn2)
                .medOrdinær("                                     $$$$$$$$$$$", nasjonalt = { 1676 }, differanse = { 400 }, kalkulert = { 400 })
                .bygg()

        assertEquals(forventet.andelerTilkjentYtelse.sortert(), tilkjenteYtelserEtterDiffernanseberegningForBarnaOgSøker.sortert())
    }
}

private fun Collection<AndelTilkjentYtelse>.sortert() = this.sortedWith(compareBy({ it.aktør.aktørId }, { it.type }, { it.stønadFom }))
