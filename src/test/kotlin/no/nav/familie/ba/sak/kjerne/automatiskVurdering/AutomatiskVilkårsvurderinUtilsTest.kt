package no.nav.familie.ba.sak.kjerne.automatiskVurdering

import no.nav.familie.ba.sak.kjerne.automatiskvurdering.PersonResultat
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.Rolle
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.VilkårType
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.VilkårsVurdering
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.Vilkårsresultat
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.erBarnBosattIRiket
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.erBarnetBosattMedSøker
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.erBarnetUgift
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.erBarnetUnder18
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.erMorBosattIRiket
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.erVilkårOppfylt
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.initierVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AutomatiskVilkårsvurderingUtilsTest {


    @Test
    fun `Godkjenner kun når mor har bostedsadresse som er gyldig nå`() {
        val morsManglendeBosted = null

        Assertions.assertEquals(true, erMorBosattIRiket(mockNåværendeBosted))
        Assertions.assertEquals(true, erMorBosattIRiket(mockTidløstBosted))
        Assertions.assertEquals(false, erMorBosattIRiket(mockTidligereBosted))
        Assertions.assertEquals(false, erMorBosattIRiket(morsManglendeBosted))
    }

    @Test
    fun `Godkjenner kun barn under 18 år`() {
        val barnUnder18 = LocalDate.now().minusYears(6)
        val barnOver18 = LocalDate.now().minusYears(20)

        Assertions.assertEquals(true, erBarnetUnder18(barnUnder18))
        Assertions.assertEquals(false, erBarnetUnder18(barnOver18))
    }

    @Test
    fun `Godkjenner kun når barn bor med søker`() {
        Assertions.assertEquals(true, erBarnetBosattMedSøker(mockNåværendeBosted, mockNåværendeBosted))
        Assertions.assertEquals(false, erBarnetBosattMedSøker(mockTidligereBosted, mockNåværendeBosted))
        Assertions.assertEquals(false, erBarnetBosattMedSøker(mockAnnetNåværendeBosted, mockNåværendeBosted))
    }

    @Test
    fun `Godkjenner kun når barn er ugift`() {
        Assertions.assertEquals(true,
                                erBarnetUgift(GrSivilstand(type = SIVILSTAND.UGIFT,
                                                           person = personopplysningGrunnlagForGodkjentSak.barna.last())))
        Assertions.assertEquals(true,
                                erBarnetUgift(GrSivilstand(type = SIVILSTAND.UOPPGITT,
                                                           person = personopplysningGrunnlagForGodkjentSak.barna.last())))
        Assertions.assertEquals(false,
                                erBarnetUgift(GrSivilstand(type = SIVILSTAND.GIFT,
                                                           person = personopplysningGrunnlagForGodkjentSak.barna.last())))
        Assertions.assertEquals(false,
                                erBarnetUgift(GrSivilstand(type = SIVILSTAND.SKILT,
                                                           person = personopplysningGrunnlagForGodkjentSak.barna.last())))
    }

    @Test
    fun `Godkjenner kun når barn har bostedsadresse som er gyldig nå`() {
        val barnasManglendeBosted = null
        Assertions.assertEquals(true, erBarnBosattIRiket(mockNåværendeBosted))
        Assertions.assertEquals(true, erBarnBosattIRiket(mockTidløstBosted))
        Assertions.assertEquals(false, erBarnBosattIRiket(mockTidligereBosted))
        Assertions.assertEquals(false, erBarnBosattIRiket(barnasManglendeBosted))
    }
    

    @Test
    fun `En Godkjent sak`() {
        val barnasIdenter = personopplysningGrunnlagForGodkjentSak.barna.map { it.personIdent.ident }
        val forventetResultat = listOf(PersonResultat(rolle = Rolle.MOR,
                                                      vilkår = listOf(Vilkårsresultat(type = VilkårType.MOR_ER_BOSTATT_I_RIKET,
                                                                                      resultat = VilkårsVurdering.OPPFYLT))),
                                       PersonResultat(rolle = Rolle.BARN,
                                                      vilkår = listOf(Vilkårsresultat(type = VilkårType.BARN_ER_UNDER_18,
                                                                                      resultat = VilkårsVurdering.OPPFYLT),
                                                                      Vilkårsresultat(type = VilkårType.BARN_BOR_MED_SØKER,
                                                                                      resultat = VilkårsVurdering.OPPFYLT),
                                                                      Vilkårsresultat(type = VilkårType.BARN_ER_UGIFT,
                                                                                      resultat = VilkårsVurdering.OPPFYLT),
                                                                      Vilkårsresultat(type = VilkårType.BARN_ER_BOSATT_I_RIKET,
                                                                                      resultat = VilkårsVurdering.OPPFYLT))))

        val faktiskresultat = initierVilkårsvurdering(personopplysningGrunnlagForGodkjentSak, barnasIdenter)
        println(faktiskresultat)
        Assertions.assertEquals(forventetResultat, faktiskresultat)
        Assertions.assertEquals(true, erVilkårOppfylt(faktiskresultat))
    }

    @Test
    fun `En Ikke Godkjent sak, pga mor ikke bor i riket`() {
        val barnasIdenter = personopplysningGrunnlagMedUtdatertAdresse.barna.map { it.personIdent.ident }
        val forventetResultat = listOf(PersonResultat(rolle = Rolle.MOR,
                                                      vilkår = listOf(Vilkårsresultat(type = VilkårType.MOR_ER_BOSTATT_I_RIKET,
                                                                                      resultat = VilkårsVurdering.IKKE_OPPFYLT))),
                                       PersonResultat(rolle = Rolle.BARN,
                                                      vilkår = listOf(Vilkårsresultat(type = VilkårType.BARN_ER_UNDER_18,
                                                                                      resultat = VilkårsVurdering.OPPFYLT),
                                                                      Vilkårsresultat(type = VilkårType.BARN_BOR_MED_SØKER,
                                                                                      resultat = VilkårsVurdering.OPPFYLT),
                                                                      Vilkårsresultat(type = VilkårType.BARN_ER_UGIFT,
                                                                                      resultat = VilkårsVurdering.OPPFYLT),
                                                                      Vilkårsresultat(type = VilkårType.BARN_ER_BOSATT_I_RIKET,
                                                                                      resultat = VilkårsVurdering.OPPFYLT))))

        val faktiskresultat = initierVilkårsvurdering(personopplysningGrunnlagMedUtdatertAdresse, barnasIdenter)
        println(faktiskresultat)
        Assertions.assertEquals(forventetResultat, faktiskresultat)
        Assertions.assertEquals(false, erVilkårOppfylt(faktiskresultat))
    }

    @Test
    fun `En Ikke Godkjent sak, pga barn ikke bor med søker`() {
        val barnasIdenter = personopplysningGrunnlagMedUlikeAdresser.barna.map { it.personIdent.ident }
        val forventetResultat = listOf(PersonResultat(rolle = Rolle.MOR,
                                                      vilkår = listOf(Vilkårsresultat(type = VilkårType.MOR_ER_BOSTATT_I_RIKET,
                                                                                      resultat = VilkårsVurdering.OPPFYLT))),
                                       PersonResultat(rolle = Rolle.BARN,
                                                      vilkår = listOf(Vilkårsresultat(type = VilkårType.BARN_ER_UNDER_18,
                                                                                      resultat = VilkårsVurdering.OPPFYLT),
                                                                      Vilkårsresultat(type = VilkårType.BARN_BOR_MED_SØKER,
                                                                                      resultat = VilkårsVurdering.IKKE_OPPFYLT),
                                                                      Vilkårsresultat(type = VilkårType.BARN_ER_UGIFT,
                                                                                      resultat = VilkårsVurdering.OPPFYLT),
                                                                      Vilkårsresultat(type = VilkårType.BARN_ER_BOSATT_I_RIKET,
                                                                                      resultat = VilkårsVurdering.OPPFYLT))))

        val faktiskresultat = initierVilkårsvurdering(personopplysningGrunnlagMedUlikeAdresser, barnasIdenter)
        println(faktiskresultat)
        Assertions.assertEquals(forventetResultat, faktiskresultat)
        Assertions.assertEquals(false, erVilkårOppfylt(faktiskresultat))
    }

    @Test
    fun `En Ikke Godkjent sak, pga ett barn ikke bor med søker`() {
        val barnasIdenter = personopplysningGrunnlagMedUlikeAdresserForEtAvFlereBarn.barna.map { it.personIdent.ident }
        val forventetResultat = listOf(PersonResultat(rolle = Rolle.MOR,
                                                      vilkår = listOf(Vilkårsresultat(type = VilkårType.MOR_ER_BOSTATT_I_RIKET,
                                                                                      resultat = VilkårsVurdering.OPPFYLT))),
                                       PersonResultat(rolle = Rolle.BARN,
                                                      vilkår = listOf(Vilkårsresultat(type = VilkårType.BARN_ER_UNDER_18,
                                                                                      resultat = VilkårsVurdering.OPPFYLT),
                                                                      Vilkårsresultat(type = VilkårType.BARN_BOR_MED_SØKER,
                                                                                      resultat = VilkårsVurdering.IKKE_OPPFYLT),
                                                                      Vilkårsresultat(type = VilkårType.BARN_ER_UGIFT,
                                                                                      resultat = VilkårsVurdering.OPPFYLT),
                                                                      Vilkårsresultat(type = VilkårType.BARN_ER_BOSATT_I_RIKET,
                                                                                      resultat = VilkårsVurdering.OPPFYLT))),
                                       PersonResultat(rolle = Rolle.BARN,
                                                      vilkår = listOf(Vilkårsresultat(type = VilkårType.BARN_ER_UNDER_18,
                                                                                      resultat = VilkårsVurdering.OPPFYLT),
                                                                      Vilkårsresultat(type = VilkårType.BARN_BOR_MED_SØKER,
                                                                                      resultat = VilkårsVurdering.OPPFYLT),
                                                                      Vilkårsresultat(type = VilkårType.BARN_ER_UGIFT,
                                                                                      resultat = VilkårsVurdering.OPPFYLT),
                                                                      Vilkårsresultat(type = VilkårType.BARN_ER_BOSATT_I_RIKET,
                                                                                      resultat = VilkårsVurdering.OPPFYLT))))

        val faktiskresultat = initierVilkårsvurdering(personopplysningGrunnlagMedUlikeAdresserForEtAvFlereBarn, barnasIdenter)
        println(faktiskresultat)
        Assertions.assertEquals(forventetResultat, faktiskresultat)
        Assertions.assertEquals(false, erVilkårOppfylt(faktiskresultat))
    }
}