package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.eksempler.Tidslinjer
import no.nav.familie.ba.sak.kjerne.tidslinje.util.KompetanseBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.VilkårsvurderingBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.print
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class TidslinjeTest {

    @Test
    fun test() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN)
        val barn2 = tilfeldigPerson(personType = PersonType.BARN)
        val barn3 = tilfeldigPerson(personType = PersonType.BARN)

        val januar2020 = YearMonth.of(2020, 1)
        val behandling = lagBehandling()

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandling.id,
            søker, barn1, barn2, barn3
        )

        val vilkårsvurdering = VilkårsvurderingBuilder(behandling = behandling)
            .forPerson(søker, januar2020)
            .medVilkår("---------------------", Vilkår.BOSATT_I_RIKET)
            .medVilkår("---------------------", Vilkår.LOVLIG_OPPHOLD)
            .forPerson(barn1, januar2020)
            .medVilkår("----------------     ", Vilkår.UNDER_18_ÅR)
            .medVilkår("   EEE NNNN  EEEE--- ", Vilkår.BOSATT_I_RIKET)
            .medVilkår("     EEENNEEEEEEEEE  ", Vilkår.LOVLIG_OPPHOLD)
            .medVilkår("NNNNNNNNNNEEEEEEEEEEE", Vilkår.BOR_MED_SØKER)
            .medVilkår("---------------------", Vilkår.GIFT_PARTNERSKAP)
            .forPerson(barn2, januar2020)
            .medVilkår("--------->", Vilkår.UNDER_18_ÅR)
            .medVilkår(" EEEE--EE>", Vilkår.BOSATT_I_RIKET)
            .medVilkår("EEEEEEEEE>", Vilkår.LOVLIG_OPPHOLD)
            .medVilkår("EEEENNEEE>", Vilkår.BOR_MED_SØKER)
            .medVilkår("--------->", Vilkår.GIFT_PARTNERSKAP)
            .byggVilkårsvurdering()

        val kompetanser = KompetanseBuilder(behandling = behandling, januar2020)
            .medKompetanse("---SSSPP--SSPPSS", barn1, barn2, barn3)
            .medKompetanse("                SSSSS", barn1)
            .medKompetanse("                PPPPP", barn2)
            .medKompetanse("                -----", barn3)
            .byggKompetanser()

        val tidslinjer = Tidslinjer(
            vilkårsvurdering,
            personopplysningGrunnlag,
            kompetanser
        )

        tidslinjer.forBarn(barn2).barnetsVilkårsresultatTidslinjer.print()
        // tidslinjer.forBarn(barn2).erEøsTidslinje.print()
        // tidslinjer.forBarn(barn2).kompetanseTidslinje.print()

        println("Søker")
        tidslinjer.søkerOppfyllerVilkårTidslinje.print()
        println("Barn: ${barn1.aktør.aktivFødselsnummer()}")
        tidslinjer.forBarn(barn1).erEøsTidslinje.print()
        println("Barn: ${barn2.aktør.aktivFødselsnummer()}")

        tidslinjer.forBarn(barn2).erEøsTidslinje.print()
        tidslinjer.forBarn(barn2).kompetanseValideringTidslinje.perioder().size
        tidslinjer.forBarn(barn2).erSekundærlandTidslinje.perioder().size
    }
}
