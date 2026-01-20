package no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.rest

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.tilPersonEnkelSøkerOgBarn
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjer
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.util.VilkårsvurderingBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.des
import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.nov
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class TidslinjerDtoTest {
    @Test
    fun `når barnet har løpende vilkår, skal likevel rest-tidslinjene for regelverk og oppfylt vilkår være avsluttet ved 18 år`() {
        val barnsFødselsdato = 13.jan(2020)
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = barnsFødselsdato)

        val behandling = lagBehandling()
        val startMåned = barnsFødselsdato.toYearMonth()

        val vilkårsvurderingBygger =
            VilkårsvurderingBuilder(behandling)
                .forPerson(søker, startMåned)
                .medVilkår("EEEEEEEEEEEEEEEEEEEEEEEEE", Vilkår.BOSATT_I_RIKET)
                .medVilkår("EEEEEEEEEEEEEEEEEEEEEEEEE", Vilkår.LOVLIG_OPPHOLD)
                .forPerson(barn1, startMåned)
                .medVilkår("+++++++++>", Vilkår.UNDER_18_ÅR)
                .medVilkår(" EEEE++EE>", Vilkår.BOSATT_I_RIKET)
                .medVilkår("EEEEEEEEE>", Vilkår.LOVLIG_OPPHOLD)
                .medVilkår("EEEENNEEE>", Vilkår.BOR_MED_SØKER)
                .medVilkår("+++++++++>", Vilkår.GIFT_PARTNERSKAP)

        val vilkårsvurderingTidslinjer =
            VilkårsvurderingTidslinjer(
                vilkårsvurdering = vilkårsvurderingBygger.byggVilkårsvurdering(),
                søkerOgBarn =
                    lagTestPersonopplysningGrunnlag(behandling.id, søker, barn1)
                        .tilPersonEnkelSøkerOgBarn(),
            )

        val restTidslinjer = vilkårsvurderingTidslinjer.tilTidslinjerDto()
        val barnetsTidslinjer = restTidslinjer.barnasTidslinjer[barn1.aktør.aktivFødselsnummer()]!!

        // Stopper ved søkers siste til-og-med-dato fordi Regelverk er <null> etter det, som filtreres bort
        assertEquals(
            31.jan(2022),
            barnetsTidslinjer.regelverkTidslinje.last().tilOgMed,
        )
        assertEquals(
            31.jan(2022),
            barnetsTidslinjer.oppfyllerEgneVilkårIKombinasjonMedSøkerTidslinje.last().tilOgMed,
        )

        // Alle vilkårene til barnet kuttes ved siste dag i måneden før barnet fyller 18 år
        barnetsTidslinjer.vilkårTidslinjer.forEach {
            assertEquals(
                31.des(2037),
                it.last().tilOgMed,
            )
        }
    }

    @Test
    fun `søkers rest-tidslinjene for oppfylt vilkår skal begrenses av barnas 18-års-perioder`() {
        val søkersFødselsdato = 3.feb(1995)
        val barn1Fødselsdato = 13.jan(2020)
        val barn2Fødselsdato = 27.des(2021)
        val søker = tilfeldigPerson(personType = PersonType.SØKER, fødselsdato = søkersFødselsdato)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = barn1Fødselsdato)
        val barn2 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = barn2Fødselsdato)

        val behandling = lagBehandling()

        val vilkårsvurderingBygger =
            VilkårsvurderingBuilder(behandling)
                .forPerson(søker, søkersFødselsdato.toYearMonth())
                .medVilkår("E>", Vilkår.BOSATT_I_RIKET)
                .medVilkår("E>", Vilkår.LOVLIG_OPPHOLD)
                .forPerson(barn1, barn1Fødselsdato.toYearMonth())
                .medVilkår("+>", Vilkår.UNDER_18_ÅR)
                .medVilkår("E>", Vilkår.BOSATT_I_RIKET)
                .medVilkår("E>", Vilkår.LOVLIG_OPPHOLD)
                .medVilkår("E>", Vilkår.BOR_MED_SØKER)
                .medVilkår("+>", Vilkår.GIFT_PARTNERSKAP)
                .forPerson(barn2, barn2Fødselsdato.toYearMonth())
                .medVilkår("+>", Vilkår.UNDER_18_ÅR)
                .medVilkår("E>", Vilkår.BOSATT_I_RIKET)
                .medVilkår("E>", Vilkår.LOVLIG_OPPHOLD)
                .medVilkår("E>", Vilkår.BOR_MED_SØKER)
                .medVilkår("+>", Vilkår.GIFT_PARTNERSKAP)

        val vilkårsvurderingTidslinjer =
            VilkårsvurderingTidslinjer(
                vilkårsvurdering = vilkårsvurderingBygger.byggVilkårsvurdering(),
                søkerOgBarn =
                    lagTestPersonopplysningGrunnlag(behandling.id, søker, barn1, barn2)
                        .tilPersonEnkelSøkerOgBarn(),
            )

        val restTidslinjer = vilkårsvurderingTidslinjer.tilTidslinjerDto()
        val søkersTidslinjer = restTidslinjer.søkersTidslinjer

        // Stopper ved siste dag i måneden før yngste barn fyller 18 år
        søkersTidslinjer.vilkårTidslinjer.forEach {
            assertEquals(
                30.nov(2039),
                it.last().tilOgMed,
            )
        }

        assertEquals(
            30.nov(2039),
            søkersTidslinjer.oppfyllerEgneVilkårTidslinje.last().tilOgMed,
        )
    }
}
