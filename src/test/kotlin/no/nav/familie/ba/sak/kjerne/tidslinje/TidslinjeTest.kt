package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer.Tidslinjer
import no.nav.familie.ba.sak.kjerne.tidslinje.util.VilkårsvurderingBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.print
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class TidslinjeTest {

    @Test
    fun skrivUtEksempler() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN)
        val barn2 = tilfeldigPerson(personType = PersonType.BARN)
        val personerMedFødselsdato =
            mapOf(søker.aktør to søker.fødselsdato, barn1.aktør to barn1.fødselsdato, barn2.aktør to barn2.fødselsdato)

        val januar2020 = YearMonth.of(2020, 1)
        val behandling = lagBehandling()

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

        val tidslinjer = Tidslinjer(
            vilkårsvurdering = vilkårsvurdering,
            søkersFødselsdato = søker.fødselsdato,
            yngsteBarnFødselsdato = maxOf(barn1.fødselsdato, barn2.fødselsdato),
            barnOgFødselsdatoer = mapOf(barn1.aktør to barn1.fødselsdato, barn2.aktør to barn2.fødselsdato)
        )

        tidslinjer.forBarn(barn2).vilkårsresultatTidslinjer.print()

        println("Søker")
        tidslinjer.søkersTidslinjer().oppfyllerVilkårTidslinje.print()
        println("Barn: ${barn1.aktør.aktivFødselsnummer()}")
        tidslinjer.forBarn(barn1).regelverkTidslinje.print()
        println("Barn: ${barn2.aktør.aktivFødselsnummer()}")

        tidslinjer.forBarn(barn2).regelverkTidslinje.print()
    }
}
