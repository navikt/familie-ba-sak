package no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.rangeTo
import no.nav.familie.ba.sak.common.til18ÅrsVilkårsdato
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.oppfyltVilkår
import no.nav.familie.ba.sak.datagenerator.tilPersonEnkelSøkerOgBarn
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement.tilpassKompetanserTilRegelverk
import no.nav.familie.ba.sak.kjerne.eøs.util.uendelig
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.util.VilkårsvurderingBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.apr
import no.nav.familie.ba.sak.kjerne.tidslinje.util.byggVilkårsvurderingTidslinjer
import no.nav.familie.ba.sak.kjerne.tidslinje.util.des
import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.konkatenerTidslinjer
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mai
import no.nav.familie.ba.sak.kjerne.tidslinje.util.nov
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilRegelverkResultatTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilTidslinje
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk.EØS_FORORDNINGEN
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk.NASJONALE_REGLER
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOR_MED_SØKER
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.GIFT_PARTNERSKAP
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.LOVLIG_OPPHOLD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.UNDER_18_ÅR
import no.nav.familie.tidslinje.utvidelser.filtrerIkkeNull
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class TidslinjerTest {
    @Test
    fun `lag en søker med to barn og mye kompleksitet i vilkårsvurderingen`() {
        val barnsFødselsdato = 13.jan(2020)
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = barnsFødselsdato)
        val barn2 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = barnsFødselsdato)

        val behandling = lagBehandling()
        val startMåned = barnsFødselsdato.toYearMonth()
        val startMånedRegelverk = startMåned.nesteMåned()

        val vilkårsvurderingBygger =
            VilkårsvurderingBuilder(behandling)
                .forPerson(søker, startMåned)
                .medVilkår("EEEEEEEENNEEEEEEEEEEE", BOSATT_I_RIKET)
                .medVilkår("EEEEEEEENNEEEEEEEEEEE", LOVLIG_OPPHOLD)
                .byggPerson()
        val søkerResult = "EEEEEEENNEEEEEEEEEEE".tilRegelverkResultatTidslinje(startMånedRegelverk)

        vilkårsvurderingBygger
            .forPerson(barn1, startMåned)
            .medVilkår("++++++++++++++++     ", UNDER_18_ÅR)
            .medVilkår("   EEE NNNN  EEEE+++ ", BOSATT_I_RIKET)
            .medVilkår("     EEENNEEEEEEEEE  ", LOVLIG_OPPHOLD)
            .medVilkår("NNNNNNNNNNEEEEEEEEEEE", BOR_MED_SØKER)
            .medVilkår("+++++++++++++++++++++", GIFT_PARTNERSKAP)
            .byggPerson()
        val barn1Result = "???????NN!???EE?????".tilRegelverkResultatTidslinje(startMånedRegelverk)

        vilkårsvurderingBygger
            .forPerson(barn2, startMåned)
            .medVilkår("+++++++++>", UNDER_18_ÅR)
            .medVilkår(" EEEE++EE>", BOSATT_I_RIKET)
            .medVilkår("EEEEEEEEE>", LOVLIG_OPPHOLD)
            .medVilkår("EEEENNEEE>", BOR_MED_SØKER)
            .medVilkår("+++++++++>", GIFT_PARTNERSKAP)
            .byggPerson()
        val barn2Result = "?EE!!!E!!EEEEEEEEEEE".tilRegelverkResultatTidslinje(startMånedRegelverk)

        val vilkårsvurderingTidslinjer =
            VilkårsvurderingTidslinjer(
                vilkårsvurdering = vilkårsvurderingBygger.byggVilkårsvurdering(),
                søkerOgBarn = lagTestPersonopplysningGrunnlag(behandling.id, søker, barn1, barn2).tilPersonEnkelSøkerOgBarn(),
            )

        assertEquals(søkerResult, vilkårsvurderingTidslinjer.søkersTidslinje().regelverkResultatTidslinje)
        assertEquals(barn1Result, vilkårsvurderingTidslinjer.forBarn(barn1).regelverkResultatTidslinje)
        assertEquals(barn2Result, vilkårsvurderingTidslinjer.forBarn(barn2).regelverkResultatTidslinje)
    }

    @Test
    fun `lag en søker med ett barn og søker går fra EØS-regelverk til nasjonalt`() {
        val barnsFødselsdato = 13.jan(2020)
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = barnsFødselsdato)

        val behandling = lagBehandling()
        val startMåned = barnsFødselsdato.toYearMonth()
        val startMånedRegelverk = startMåned.nesteMåned()

        val vilkårsvurderingBygger =
            VilkårsvurderingBuilder(behandling)
                .forPerson(søker, startMåned)
                .medVilkår("EEEEEEEEEEEEENNNNNNNN", BOSATT_I_RIKET)
                .medVilkår("EEEEEEEEEEEEENNNNNNNN", LOVLIG_OPPHOLD)
                .byggPerson()
        val søkerResult = "EEEEEEEEEEEENNNNNNNN".tilRegelverkResultatTidslinje(startMånedRegelverk)

        vilkårsvurderingBygger
            .forPerson(barn1, startMåned)
            .medVilkår("++++++++++++++++     ", UNDER_18_ÅR)
            .medVilkår("   EEEENNNNEEEEEEEE ", BOSATT_I_RIKET)
            .medVilkår("     EEENNEEEEEEEEE  ", LOVLIG_OPPHOLD)
            .medVilkår("NNNNNNNNNNEEEEEEEEEEE", BOR_MED_SØKER)
            .medVilkår("+++++++++++++++++++++", GIFT_PARTNERSKAP)
            .byggPerson()
        val barn1Result = "?????!!!!!EE!!!?????".tilRegelverkResultatTidslinje(startMånedRegelverk)

        val vilkårsvurderingTidslinjer =
            VilkårsvurderingTidslinjer(
                vilkårsvurdering = vilkårsvurderingBygger.byggVilkårsvurdering(),
                søkerOgBarn = lagTestPersonopplysningGrunnlag(behandling.id, søker, barn1).tilPersonEnkelSøkerOgBarn(),
            )

        assertEquals(søkerResult, vilkårsvurderingTidslinjer.søkersTidslinje().regelverkResultatTidslinje)
        assertEquals(barn1Result, vilkårsvurderingTidslinjer.forBarn(barn1).regelverkResultatTidslinje)
    }

    @Test
    fun `Virkningstidspunkt for vilkårsvurdering varer frem til måneden før barnet fyller 18 år`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = 14.des(2019))

        val behandling = lagBehandling()

        val vilkårsvurderingBygger =
            VilkårsvurderingBuilder(behandling)
                .forPerson(søker, jan(2020))
                .medVilkår("+>", BOSATT_I_RIKET)
                .medVilkår("+>", LOVLIG_OPPHOLD)
                .forPerson(barn1, jan(2020))
                .medVilkår("+>", UNDER_18_ÅR)
                .medVilkår("+>", BOSATT_I_RIKET)
                .medVilkår("+>", LOVLIG_OPPHOLD)
                .medVilkår("+>", BOR_MED_SØKER)
                .medVilkår("+>", GIFT_PARTNERSKAP)
                .byggPerson()

        val vilkårsvurderingTidslinjer =
            VilkårsvurderingTidslinjer(
                vilkårsvurdering = vilkårsvurderingBygger.byggVilkårsvurdering(),
                søkerOgBarn =
                    lagTestPersonopplysningGrunnlag(behandling.id, søker, barn1)
                        .tilPersonEnkelSøkerOgBarn(),
            )

        assertEquals(
            barn1.fødselsdato
                .til18ÅrsVilkårsdato()
                .minusMonths(1)
                .toYearMonth(),
            vilkårsvurderingTidslinjer
                .forBarn(barn1)
                .egetRegelverkResultatTidslinje
                .filtrerIkkeNull()
                .tilPerioder()
                .maxOf { it.tom!!.toYearMonth() },
        )
    }

    @Test
    fun `Sjekk overgang fra oppfylt nasjonalt til oppfylt EØS i månedsskiftet`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = 14.des(2019))

        val giftPartnerskap = (30.apr(2020)..1.mai(2020)).tilTidslinje { oppfyltVilkår(GIFT_PARTNERSKAP) }
        val under18 = (30.apr(2020)..1.mai(2020)).tilTidslinje { oppfyltVilkår(UNDER_18_ÅR) }
        val bosattBarnOgSøker =
            konkatenerTidslinjer(
                (30.apr(2020)..30.apr(2020)).tilTidslinje { oppfyltVilkår(BOSATT_I_RIKET, NASJONALE_REGLER) },
                (1.mai(2020)..1.mai(2020)).tilTidslinje { oppfyltVilkår(BOSATT_I_RIKET, EØS_FORORDNINGEN) },
            )
        val lovligOppholdBarnOgSøker =
            konkatenerTidslinjer(
                (30.apr(2020)..30.apr(2020)).tilTidslinje { oppfyltVilkår(LOVLIG_OPPHOLD, NASJONALE_REGLER) },
                (1.mai(2020)..1.mai(2020)).tilTidslinje { oppfyltVilkår(LOVLIG_OPPHOLD, EØS_FORORDNINGEN) },
            )
        val borMedSøker =
            konkatenerTidslinjer(
                (30.apr(2020)..30.apr(2020)).tilTidslinje { oppfyltVilkår(BOR_MED_SØKER, NASJONALE_REGLER) },
                (1.mai(2020)..1.mai(2020)).tilTidslinje { oppfyltVilkår(BOR_MED_SØKER, EØS_FORORDNINGEN) },
            )

        val vilkårsvurderingTidslinjer =
            VilkårsvurderingBuilder()
                .forPerson(søker, 30.apr(2020))
                .medVilkår(bosattBarnOgSøker)
                .medVilkår(lovligOppholdBarnOgSøker)
                .forPerson(barn1, 30.apr(2020))
                .medVilkår(bosattBarnOgSøker)
                .medVilkår(lovligOppholdBarnOgSøker)
                .medVilkår(giftPartnerskap)
                .medVilkår(under18)
                .medVilkår(borMedSøker)
                .byggVilkårsvurderingTidslinjer()

        val barn1Result = "E".tilRegelverkResultatTidslinje(YearMonth.of(2020, 5))

        assertEquals(barn1Result, vilkårsvurderingTidslinjer.forBarn(barn1).regelverkResultatTidslinje)
    }

    @Test
    fun `Sjekk overgang fra oppfylt EØS til oppfylt nasjonalt i månedsskiftet`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = 14.des(2019))

        val giftPartnerskap = (30.apr(2020)..1.mai(2020)).tilTidslinje { oppfyltVilkår(GIFT_PARTNERSKAP) }
        val under18 = (30.apr(2020)..1.mai(2020)).tilTidslinje { oppfyltVilkår(UNDER_18_ÅR) }
        val bosattBarnOgSøker =
            konkatenerTidslinjer(
                (30.apr(2020)..30.apr(2020)).tilTidslinje { oppfyltVilkår(BOSATT_I_RIKET, EØS_FORORDNINGEN) },
                (1.mai(2020)..1.mai(2020)).tilTidslinje { oppfyltVilkår(BOSATT_I_RIKET, NASJONALE_REGLER) },
            )
        val lovligOppholdBarnOgSøker =
            konkatenerTidslinjer(
                (30.apr(2020)..30.apr(2020)).tilTidslinje { oppfyltVilkår(LOVLIG_OPPHOLD, EØS_FORORDNINGEN) },
                (1.mai(2020)..1.mai(2020)).tilTidslinje { oppfyltVilkår(LOVLIG_OPPHOLD, NASJONALE_REGLER) },
            )
        val borMedSøker =
            konkatenerTidslinjer(
                (30.apr(2020)..30.apr(2020)).tilTidslinje { oppfyltVilkår(BOR_MED_SØKER, EØS_FORORDNINGEN) },
                (1.mai(2020)..1.mai(2020)).tilTidslinje { oppfyltVilkår(BOR_MED_SØKER, NASJONALE_REGLER) },
            )

        val vilkårsvurderingTidslinjer =
            VilkårsvurderingBuilder()
                .forPerson(søker, 30.apr(2020))
                .medVilkår(bosattBarnOgSøker)
                .medVilkår(lovligOppholdBarnOgSøker)
                .forPerson(barn1, 30.apr(2020))
                .medVilkår(bosattBarnOgSøker)
                .medVilkår(lovligOppholdBarnOgSøker)
                .medVilkår(under18)
                .medVilkår(giftPartnerskap)
                .medVilkår(borMedSøker)
                .byggVilkårsvurderingTidslinjer()

        val barn1Result = "N".tilRegelverkResultatTidslinje(YearMonth.of(2020, 5))

        assertEquals(barn1Result, vilkårsvurderingTidslinjer.forBarn(barn1).regelverkResultatTidslinje)
    }

    @Test
    fun `Sjekk overgang fra oppfylt EØS til oppfylt blandet i månedsskiftet`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = 14.des(2019))

        val giftPartnerskap = (30.apr(2020)..1.mai(2020)).tilTidslinje { oppfyltVilkår(GIFT_PARTNERSKAP) }
        val under18 = (30.apr(2020)..1.mai(2020)).tilTidslinje { oppfyltVilkår(UNDER_18_ÅR) }
        val bosattBarnOgSøker = (30.apr(2020)..1.mai(2020)).tilTidslinje { oppfyltVilkår(BOSATT_I_RIKET, EØS_FORORDNINGEN) }
        val lovligOppholdBarnOgSøker = (30.apr(2020)..1.mai(2020)).tilTidslinje { oppfyltVilkår(LOVLIG_OPPHOLD, EØS_FORORDNINGEN) }
        val borMedSøker =
            konkatenerTidslinjer(
                (30.apr(2020)..30.apr(2020)).tilTidslinje { oppfyltVilkår(BOR_MED_SØKER, EØS_FORORDNINGEN) },
                (1.mai(2020)..1.mai(2020)).tilTidslinje { oppfyltVilkår(BOR_MED_SØKER, NASJONALE_REGLER) },
            )

        val vilkårsvurderingTidslinjer =
            VilkårsvurderingBuilder()
                .forPerson(søker, 30.apr(2020))
                .medVilkår(bosattBarnOgSøker)
                .medVilkår(lovligOppholdBarnOgSøker)
                .forPerson(barn1, 30.apr(2020))
                .medVilkår(bosattBarnOgSøker)
                .medVilkår(lovligOppholdBarnOgSøker)
                .medVilkår(giftPartnerskap)
                .medVilkår(under18)
                .medVilkår(borMedSøker)
                .byggVilkårsvurderingTidslinjer()

        val barn1Result = "!".tilRegelverkResultatTidslinje(YearMonth.of(2020, 5))

        assertEquals(barn1Result, vilkårsvurderingTidslinjer.forBarn(barn1).regelverkResultatTidslinje)
    }

    @Test
    fun `Sjekk overgang fra oppfylt nasjonalt til oppfylt EØS dagen før siste dag i måneden`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = 14.des(2019))

        val giftPartnerskap =
            (26.jan(2020)..30.nov(2021)).tilTidslinje { oppfyltVilkår(GIFT_PARTNERSKAP) }
        val under18 =
            (26.jan(2020)..30.nov(2021)).tilTidslinje { oppfyltVilkår(UNDER_18_ÅR) }
        val bosattBarnOgSøker =
            konkatenerTidslinjer(
                (26.jan(2020)..29.apr(2021)).tilTidslinje { oppfyltVilkår(BOSATT_I_RIKET, NASJONALE_REGLER) },
                (30.apr(2021)..30.nov(2021)).tilTidslinje { oppfyltVilkår(BOSATT_I_RIKET, EØS_FORORDNINGEN) },
            )
        val lovligOppholdBarnOgSøker =
            konkatenerTidslinjer(
                (26.jan(2020)..29.apr(2021)).tilTidslinje { oppfyltVilkår(LOVLIG_OPPHOLD, NASJONALE_REGLER) },
                (30.apr(2021)..30.nov(2021)).tilTidslinje { oppfyltVilkår(LOVLIG_OPPHOLD, EØS_FORORDNINGEN) },
            )
        val borMedSøker =
            konkatenerTidslinjer(
                (26.jan(2020)..29.apr(2021)).tilTidslinje { oppfyltVilkår(BOR_MED_SØKER, NASJONALE_REGLER) },
                (30.apr(2021)..30.nov(2021)).tilTidslinje { oppfyltVilkår(BOR_MED_SØKER, EØS_FORORDNINGEN) },
            )

        val barnaRegelverkTidslinjer =
            VilkårsvurderingBuilder()
                .forPerson(søker, 26.jan(2020))
                .medVilkår(bosattBarnOgSøker)
                .medVilkår(lovligOppholdBarnOgSøker)
                .forPerson(barn, 26.jan(2020))
                .medVilkår(bosattBarnOgSøker)
                .medVilkår(lovligOppholdBarnOgSøker)
                .medVilkår(giftPartnerskap)
                .medVilkår(under18)
                .medVilkår(borMedSøker)
                .byggVilkårsvurderingTidslinjer()
                .barnasRegelverkResultatTidslinjer()

        val kompetanser =
            tilpassKompetanserTilRegelverk(
                emptyList(),
                barnaRegelverkTidslinjer,
                emptyMap(),
                inneværendeMåned = YearMonth.now(),
            )

        assertEquals(1, kompetanser.size)
        assertEquals(YearMonth.of(2021, 5), kompetanser.first().fom)
        assertEquals(YearMonth.of(2021, 11), kompetanser.first().tom)
    }

    @Test
    fun `Sjekk overgang fra oppfylt nasjonalt til oppfylt EØS dagen andre dag i måneden`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = 14.des(2019))

        val giftPartnerskap =
            (26.jan(2020)..30.nov(2021)).tilTidslinje { oppfyltVilkår(GIFT_PARTNERSKAP) }
        val under18 =
            (26.jan(2020)..30.nov(2021)).tilTidslinje { oppfyltVilkår(UNDER_18_ÅR) }
        val bosattBarnOgSøker =
            konkatenerTidslinjer(
                (26.jan(2020)..1.mai(2021)).tilTidslinje { oppfyltVilkår(BOSATT_I_RIKET, NASJONALE_REGLER) },
                (2.mai(2021)..30.nov(2021)).tilTidslinje { oppfyltVilkår(BOSATT_I_RIKET, EØS_FORORDNINGEN) },
            )
        val lovligOppholdBarnOgSøker =
            konkatenerTidslinjer(
                (26.jan(2020)..1.mai(2021)).tilTidslinje { oppfyltVilkår(LOVLIG_OPPHOLD, NASJONALE_REGLER) },
                (2.mai(2021)..30.nov(2021)).tilTidslinje { oppfyltVilkår(LOVLIG_OPPHOLD, EØS_FORORDNINGEN) },
            )
        val borMedSøker =
            konkatenerTidslinjer(
                (26.jan(2020)..1.mai(2021)).tilTidslinje { oppfyltVilkår(BOR_MED_SØKER, NASJONALE_REGLER) },
                (2.mai(2021)..30.nov(2021)).tilTidslinje { oppfyltVilkår(BOR_MED_SØKER, EØS_FORORDNINGEN) },
            )

        val barnaRegelverkTidslinjer =
            VilkårsvurderingBuilder()
                .forPerson(søker, 26.jan(2020))
                .medVilkår(bosattBarnOgSøker)
                .medVilkår(lovligOppholdBarnOgSøker)
                .forPerson(barn, 26.jan(2020))
                .medVilkår(bosattBarnOgSøker)
                .medVilkår(lovligOppholdBarnOgSøker)
                .medVilkår(giftPartnerskap)
                .medVilkår(under18)
                .medVilkår(borMedSøker)
                .byggVilkårsvurderingTidslinjer()
                .barnasRegelverkResultatTidslinjer()

        val kompetanser =
            tilpassKompetanserTilRegelverk(
                emptyList(),
                barnaRegelverkTidslinjer,
                emptyMap(),
                inneværendeMåned = YearMonth.now(),
            )

        val forventetRegelverkResultat =
            "NNNNNNNNNNNNNNNNEEEEEE".tilRegelverkResultatTidslinje(feb(2020))

        assertEquals(forventetRegelverkResultat, barnaRegelverkTidslinjer[barn.aktør])
        assertEquals(1, kompetanser.size)
        assertEquals(YearMonth.of(2021, 6), kompetanser.first().fom)
        assertEquals(YearMonth.of(2021, 11), kompetanser.first().tom)
    }

    @Test
    fun `Sjekk overgang fra oppfylt nasjonalt til oppfylt EØS dagen før siste dag i måneden, der siste periode er uendelig`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = 14.des(2019))

        val giftPartnerskap =
            (26.jan(2020)..uendelig).tilTidslinje { oppfyltVilkår(GIFT_PARTNERSKAP) }
        val under18 =
            (26.jan(2020)..uendelig).tilTidslinje { oppfyltVilkår(UNDER_18_ÅR) }
        val bosattBarnOgSøker =
            konkatenerTidslinjer(
                (26.jan(2020)..29.apr(2021)).tilTidslinje { oppfyltVilkår(BOSATT_I_RIKET, NASJONALE_REGLER) },
                (30.apr(2021)..uendelig).tilTidslinje { oppfyltVilkår(BOSATT_I_RIKET, EØS_FORORDNINGEN) },
            )
        val lovligOppholdBarnOgSøker =
            konkatenerTidslinjer(
                (26.jan(2020)..29.apr(2021)).tilTidslinje { oppfyltVilkår(LOVLIG_OPPHOLD, NASJONALE_REGLER) },
                (30.apr(2021)..uendelig).tilTidslinje { oppfyltVilkår(LOVLIG_OPPHOLD, EØS_FORORDNINGEN) },
            )
        val borMedSøker =
            konkatenerTidslinjer(
                (26.jan(2020)..29.apr(2021)).tilTidslinje { oppfyltVilkår(BOR_MED_SØKER, NASJONALE_REGLER) },
                (30.apr(2021)..uendelig).tilTidslinje { oppfyltVilkår(BOR_MED_SØKER, EØS_FORORDNINGEN) },
            )

        val barnaRegelverkTidslinjer =
            VilkårsvurderingBuilder()
                .forPerson(søker, 26.jan(2020))
                .medVilkår(bosattBarnOgSøker)
                .medVilkår(lovligOppholdBarnOgSøker)
                .forPerson(barn, 26.jan(2020))
                .medVilkår(bosattBarnOgSøker)
                .medVilkår(lovligOppholdBarnOgSøker)
                .medVilkår(giftPartnerskap)
                .medVilkår(under18)
                .medVilkår(borMedSøker)
                .byggVilkårsvurderingTidslinjer()
                .barnasRegelverkResultatTidslinjer()

        val kompetanser =
            tilpassKompetanserTilRegelverk(
                emptyList(),
                barnaRegelverkTidslinjer,
                emptyMap(),
                inneværendeMåned = YearMonth.now(),
            )

        assertEquals(1, kompetanser.size)
        assertEquals(YearMonth.of(2021, 5), kompetanser.first().fom)
        assertNull(kompetanser.first().tom)
    }
}

fun VilkårsvurderingTidslinjer.barnasRegelverkResultatTidslinjer() =
    this
        .barnasTidslinjer()
        .mapValues { (_, barnetsTidslinjer) -> barnetsTidslinjer.regelverkResultatTidslinje }
