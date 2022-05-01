package no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.rest

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.Tidslinjer
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.util.VilkårsvurderingBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.des
import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class RestTidslinjerTest {

    @Test
    fun `når barnet har løpende vilkår, skal likevel rest-tidslinjene for regelverk og oppfylt vilkår være avsluttet ved 18 år`() {
        val barnsFødselsdato = 13.jan(2020)
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = barnsFødselsdato.tilLocalDate())

        val behandling = lagBehandling()
        val startMåned = barnsFødselsdato.tilInneværendeMåned()

        val vilkårsvurderingBygger = VilkårsvurderingBuilder<Måned>(behandling)
            .forPerson(søker, startMåned)
            .medVilkår("EEEEEEEEEEEEEEEEEEEEEEEEE", Vilkår.BOSATT_I_RIKET)
            .medVilkår("EEEEEEEEEEEEEEEEEEEEEEEEE", Vilkår.LOVLIG_OPPHOLD)
            .byggPerson().forPerson(barn1, startMåned)
            .medVilkår("+++++++++>", Vilkår.UNDER_18_ÅR)
            .medVilkår(" EEEE++EE>", Vilkår.BOSATT_I_RIKET)
            .medVilkår("EEEEEEEEE>", Vilkår.LOVLIG_OPPHOLD)
            .medVilkår("EEEENNEEE>", Vilkår.BOR_MED_SØKER)
            .medVilkår("+++++++++>", Vilkår.GIFT_PARTNERSKAP)
            .byggPerson()

        val tidslinjer = Tidslinjer(
            vilkårsvurdering = vilkårsvurderingBygger.byggVilkårsvurdering(),
            personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søker, barn1)
        )

        val restTidslinjer = tidslinjer.tilRestTidslinjer()
        val barnetsTidslinjer = restTidslinjer.barnasTidslinjer[barn1.aktør.aktivFødselsnummer()]!!

        // Stopper ved søkers siste til-og-med-dato fordi Regelverk er <null> etter det, som filtreres bort
        assertEquals(
            28.feb(2022).tilLocalDate(),
            barnetsTidslinjer.regelverkTidslinje.last().tilOgMed
        )
        // Stopper først ved barnets 18-års-grense fordi vilkårsvurderinen er IKKE_VURDERT løpende (mot uendelig)
        // Hadde den vært <null> ville den bli stoppet før
        assertEquals(
            31.des(2037).tilLocalDate(),
            barnetsTidslinjer.oppfyllerEgneVilkårIKombinasjonMedSøkerTidslinje.last().tilOgMed
        )

        // Alle vilkårene til barnet er løpende, dvs til-og-med-dato i siste perioder er <null>
        // MERK at det også gjelder UNDER_18_ÅR fordi test-dataene settes opp sånn. Det skal ikke skje i virkeligheten
        barnetsTidslinjer.vilkårTidslinjer.forEach {
            assertNull(it.last().tilOgMed)
        }
    }
}
