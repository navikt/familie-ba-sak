package no.nav.familie.ba.sak.kjerne.eøs.tidslinjer

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.util.VilkårsvurderingBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilRegelverkTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilVilkårResultatTidslinje
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class TidslinjerTest {

    @Test
    fun `lag en søker med to barn og mye kompleksitet i vilkårsvurderingen`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN)
        val barn2 = tilfeldigPerson(personType = PersonType.BARN)

        val behandling = lagBehandling()
        val jan2020 = YearMonth.of(2020, 1)

        val vilkårsvurderingBygger = VilkårsvurderingBuilder(behandling = behandling)
            .forPerson(søker, jan2020)
            .medVilkår("---------------------", Vilkår.BOSATT_I_RIKET)
            .medVilkår("---------------------", Vilkår.LOVLIG_OPPHOLD)
            .byggPerson()
        val søkerResult = " ++++++++++++++++++++".tilVilkårResultatTidslinje(jan(2020))

        vilkårsvurderingBygger.forPerson(barn1, jan2020)
            .medVilkår("----------------     ", Vilkår.UNDER_18_ÅR)
            .medVilkår("   EEE NNNN  EEEE--- ", Vilkår.BOSATT_I_RIKET)
            .medVilkår("     EEENNEEEEEEEEE  ", Vilkår.LOVLIG_OPPHOLD)
            .medVilkår("NNNNNNNNNNEEEEEEEEEEE", Vilkår.BOR_MED_SØKER)
            .medVilkår("---------------------", Vilkår.GIFT_PARTNERSKAP)
            .byggPerson()
        val barn1Result = "         N    E      ".tilRegelverkTidslinje(jan(2020))

        vilkårsvurderingBygger.forPerson(barn2, jan2020)
            .medVilkår("--------->", Vilkår.UNDER_18_ÅR)
            .medVilkår(" EEEE--EE>", Vilkår.BOSATT_I_RIKET)
            .medVilkår("EEEEEEEEE>", Vilkår.LOVLIG_OPPHOLD)
            .medVilkår("EEEENNEEE>", Vilkår.BOR_MED_SØKER)
            .medVilkår("--------->", Vilkår.GIFT_PARTNERSKAP)
            .byggPerson()
        val barn2Result = "  EE    EEEEEEEEEEEEE >".tilRegelverkTidslinje(jan(2020))

        val vilkårsvurdering = vilkårsvurderingBygger.byggVilkårsvurdering()
        val tidslinjer = Tidslinjer(
            vilkårsvurdering = vilkårsvurdering,
            søkersFødselsdato = søker.fødselsdato,
            yngsteBarnFødselsdato = maxOf(barn1.fødselsdato, barn2.fødselsdato),
            barnOgFødselsdatoer = mapOf(barn1.aktør to barn1.fødselsdato, barn2.aktør to barn2.fødselsdato)
        )

        assertEquals(søkerResult, tidslinjer.søkersTidslinjer().oppfyllerVilkårTidslinje)
        assertEquals(barn1Result, tidslinjer.forBarn(barn1).regelverkTidslinje)
        assertEquals(barn2Result, tidslinjer.forBarn(barn2).regelverkTidslinje)
    }
}
