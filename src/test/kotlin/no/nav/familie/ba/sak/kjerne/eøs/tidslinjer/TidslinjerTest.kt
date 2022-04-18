package no.nav.familie.ba.sak.kjerne.eøs.tidslinjer

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.util.VilkårsvurderingBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilRegelverkTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilVilkårResultatTidslinje
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class TidslinjerTest {

    @Test
    fun `lag en søker med to barn og mye kompleksitet i vilkårsvurderingen`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN)
        val barn2 = tilfeldigPerson(personType = PersonType.BARN)

        val behandling = lagBehandling()

        val vilkårsvurderingBygger = VilkårsvurderingBuilder<Måned>(behandling)
            .forPerson(søker, jan(2020))
            .medVilkår("EEEEEEEEEEEEEEEEEEEEE", Vilkår.BOSATT_I_RIKET)
            .medVilkår("EEEEEEEEEEEEEEEEEEEEE", Vilkår.LOVLIG_OPPHOLD)
            .byggPerson()
        val søkerResult = " ++++++++++++++++++++".tilVilkårResultatTidslinje(jan(2020))

        vilkårsvurderingBygger.forPerson(barn1, jan(2020))
            .medVilkår("++++++++++++++++     ", Vilkår.UNDER_18_ÅR)
            .medVilkår("   EEE NNNN  EEEE+++ ", Vilkår.BOSATT_I_RIKET)
            .medVilkår("     EEENNEEEEEEEEE  ", Vilkår.LOVLIG_OPPHOLD)
            .medVilkår("NNNNNNNNNNEEEEEEEEEEE", Vilkår.BOR_MED_SØKER)
            .medVilkår("+++++++++++++++++++++", Vilkår.GIFT_PARTNERSKAP)
            .byggPerson()
        val barn1Result = "         N    E      ".tilRegelverkTidslinje(jan(2020))

        vilkårsvurderingBygger.forPerson(barn2, jan(2020))
            .medVilkår("+++++++++>", Vilkår.UNDER_18_ÅR)
            .medVilkår(" EEEE++EE>", Vilkår.BOSATT_I_RIKET)
            .medVilkår("EEEEEEEEE>", Vilkår.LOVLIG_OPPHOLD)
            .medVilkår("EEEENNEEE>", Vilkår.BOR_MED_SØKER)
            .medVilkår("+++++++++>", Vilkår.GIFT_PARTNERSKAP)
            .byggPerson()
        val barn2Result = "  EE    EEEEEEEEEEEEE >".tilRegelverkTidslinje(jan(2020))

        val tidslinjer = Tidslinjer(
            vilkårsvurdering = vilkårsvurderingBygger.byggVilkårsvurdering(),
            personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søker, barn1, barn2)
        )

        assertEquals(søkerResult, tidslinjer.søkersTidslinjer().oppfyllerVilkårTidslinje)
        assertEquals(barn1Result, tidslinjer.forBarn(barn1).regelverkTidslinje)
        assertEquals(barn2Result, tidslinjer.forBarn(barn2).regelverkTidslinje)
    }
}
