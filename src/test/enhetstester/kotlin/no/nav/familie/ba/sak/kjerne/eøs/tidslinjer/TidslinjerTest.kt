package no.nav.familie.ba.sak.kjerne.eøs.tidslinjer

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.til18ÅrsVilkårsdato
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.forskyv
import no.nav.familie.ba.sak.kjerne.tidslinje.util.VilkårsvurderingBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.des
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilRegelverkResultatTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilVilkårResultatTidslinje
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class TidslinjerTest {

    @Test
    fun `lag en søker med to barn og mye kompleksitet i vilkårsvurderingen`() {
        val barnsFødselsdato = 13.jan(2020)
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = barnsFødselsdato.tilLocalDate())
        val barn2 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = barnsFødselsdato.tilLocalDate())

        val behandling = lagBehandling()
        val startMåned = barnsFødselsdato.tilInneværendeMåned()

        val vilkårsvurderingBygger = VilkårsvurderingBuilder<Måned>(behandling)
            .forPerson(søker, startMåned)
            .medVilkår("EEEEEEEENNEEEEEEEEEEE", Vilkår.BOSATT_I_RIKET)
            .medVilkår("EEEEEEEENNEEEEEEEEEEE", Vilkår.LOVLIG_OPPHOLD)
            .byggPerson()
        val søkerResult = "+++++++++++++++++++++".tilVilkårResultatTidslinje(startMåned).forskyv(1)

        vilkårsvurderingBygger.forPerson(barn1, startMåned)
            .medVilkår("++++++++++++++++     ", Vilkår.UNDER_18_ÅR)
            .medVilkår("   EEE NNNN  EEEE+++ ", Vilkår.BOSATT_I_RIKET)
            .medVilkår("     EEENNEEEEEEEEE  ", Vilkår.LOVLIG_OPPHOLD)
            .medVilkår("NNNNNNNNNNEEEEEEEEEEE", Vilkår.BOR_MED_SØKER)
            .medVilkår("+++++++++++++++++++++", Vilkår.GIFT_PARTNERSKAP)
            .byggPerson()
        val barn1Result = "-----?-?NN?--EEE-----".tilRegelverkResultatTidslinje(startMåned).forskyv(1)

        vilkårsvurderingBygger.forPerson(barn2, startMåned)
            .medVilkår("+++++++++>", Vilkår.UNDER_18_ÅR)
            .medVilkår(" EEEE++EE>", Vilkår.BOSATT_I_RIKET)
            .medVilkår("EEEEEEEEE>", Vilkår.LOVLIG_OPPHOLD)
            .medVilkår("EEEENNEEE>", Vilkår.BOR_MED_SØKER)
            .medVilkår("+++++++++>", Vilkår.GIFT_PARTNERSKAP)
            .byggPerson()
        val barn2Result = "-EEE???E??EEEEEEEEEEE".tilRegelverkResultatTidslinje(startMåned).forskyv(1)

        val tidslinjer = Tidslinjer(
            vilkårsvurdering = vilkårsvurderingBygger.byggVilkårsvurdering(),
            personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søker, barn1, barn2)
        )

        assertEquals(søkerResult, tidslinjer.søkersTidslinjer().oppfyllerVilkårTidslinje)
        assertEquals(barn1Result, tidslinjer.forBarn(barn1).regelverkResultatTidslinje)
        assertEquals(barn2Result, tidslinjer.forBarn(barn2).regelverkResultatTidslinje)
    }

    @Test
    fun `lag en søker med ett barn og søker går fra EØS-regelverk til nasjonalt`() {
        val barnsFødselsdato = 13.jan(2020)
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = barnsFødselsdato.tilLocalDate())

        val behandling = lagBehandling()
        val startMåned = barnsFødselsdato.tilInneværendeMåned()

        val vilkårsvurderingBygger = VilkårsvurderingBuilder<Måned>(behandling)
            .forPerson(søker, startMåned)
            .medVilkår("EEEEEEEEEEEEENNNNNNNN", Vilkår.BOSATT_I_RIKET)
            .medVilkår("EEEEEEEEEEEEENNNNNNNN", Vilkår.LOVLIG_OPPHOLD)
            .byggPerson()
        val søkerResult = "+++++++++++++++++++++".tilVilkårResultatTidslinje(startMåned).forskyv(1)

        vilkårsvurderingBygger.forPerson(barn1, startMåned)
            .medVilkår("++++++++++++++++     ", Vilkår.UNDER_18_ÅR)
            .medVilkår("   EEEENNNNEEEEEEEE ", Vilkår.BOSATT_I_RIKET)
            .medVilkår("     EEENNEEEEEEEEE  ", Vilkår.LOVLIG_OPPHOLD)
            .medVilkår("NNNNNNNNNNEEEEEEEEEEE", Vilkår.BOR_MED_SØKER)
            .medVilkår("+++++++++++++++++++++", Vilkår.GIFT_PARTNERSKAP)
            .byggPerson()
        val barn1Result = "-----??????EE???-----".tilRegelverkResultatTidslinje(startMåned).forskyv(1)

        val tidslinjer = Tidslinjer(
            vilkårsvurdering = vilkårsvurderingBygger.byggVilkårsvurdering(),
            personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søker, barn1)
        )

        assertEquals(søkerResult, tidslinjer.søkersTidslinjer().oppfyllerVilkårTidslinje)
        assertEquals(barn1Result, tidslinjer.forBarn(barn1).regelverkResultatTidslinje)
    }

    @Test
    fun `Virkningstidspunkt for vilkårsvurdering varer frem til måneden før barnet fyller 18 år`() {

        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = 14.des(2019).tilLocalDate())

        val behandling = lagBehandling()

        val vilkårsvurderingBygger = VilkårsvurderingBuilder<Måned>(behandling)
            .forPerson(søker, jan(2020))
            .medVilkår("+>", Vilkår.BOSATT_I_RIKET)
            .medVilkår("+>", Vilkår.LOVLIG_OPPHOLD)
            .forPerson(barn1, jan(2020))
            .medVilkår("+>", Vilkår.UNDER_18_ÅR)
            .medVilkår("+>", Vilkår.BOSATT_I_RIKET)
            .medVilkår("+>", Vilkår.LOVLIG_OPPHOLD)
            .medVilkår("+>", Vilkår.BOR_MED_SØKER)
            .medVilkår("+>", Vilkår.GIFT_PARTNERSKAP)
            .byggPerson()

        val tidslinjer = Tidslinjer(
            vilkårsvurdering = vilkårsvurderingBygger.byggVilkårsvurdering(),
            personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søker, barn1)
        )

        assertEquals(
            barn1.fødselsdato.til18ÅrsVilkårsdato().minusMonths(1).toYearMonth(),
            tidslinjer.forBarn(barn1).oppfyllerVilkårTidslinje.filtrerIkkeNull()
                .perioder().maxOf { it.tilOgMed.tilYearMonth() }
        )
    }
}
