package no.nav.familie.ba.sak.kjerne.automatiskVurdering

import no.nav.familie.ba.sak.kjerne.automatiskvurdering.AutomatiskVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.OppfyllerVilkår
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.erBarnBosattIRiket
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.erBarnetBosattMedSøker
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.erBarnetUgift
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.erBarnetUnder18
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.erMorBosattIRiket
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.vilkårsvurdering
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
        val vurdering = AutomatiskVilkårsvurdering(morBosattIRiket = OppfyllerVilkår.JA,
                                                   barnErUnder18 = barnasIdenter.map { Pair(it, OppfyllerVilkår.JA) },
                                                   barnBorMedSøker = barnasIdenter.map { Pair(it, OppfyllerVilkår.JA) },
                                                   barnErUgift = barnasIdenter.map { Pair(it, OppfyllerVilkår.JA) },
                                                   barnErBosattIRiket = barnasIdenter.map { Pair(it, OppfyllerVilkår.JA) })
        Assertions.assertEquals(true, vurdering.alleVilkårOppfylt())
        Assertions.assertEquals(vurdering, vilkårsvurdering(personopplysningGrunnlagForGodkjentSak, barnasIdenter))
    }

    @Test
    fun `En Ikke Godkjent sak, pga mor ikke bor i riket`() {
        val barnasIdenter = personopplysningGrunnlagMedUtdatertAdresse.barna.map { it.personIdent.ident }
        val vurdering = AutomatiskVilkårsvurdering(morBosattIRiket = OppfyllerVilkår.NEI,
                                                   barnErUnder18 = barnasIdenter.map { Pair(it, OppfyllerVilkår.JA) },
                                                   barnBorMedSøker = barnasIdenter.map { Pair(it, OppfyllerVilkår.JA) },
                                                   barnErUgift = barnasIdenter.map { Pair(it, OppfyllerVilkår.JA) },
                                                   barnErBosattIRiket = barnasIdenter.map { Pair(it, OppfyllerVilkår.JA) })
        Assertions.assertEquals(false, vurdering.alleVilkårOppfylt())
        Assertions.assertEquals(vurdering, vilkårsvurdering(personopplysningGrunnlagMedUtdatertAdresse, barnasIdenter))
    }

    @Test
    fun `En Ikke Godkjent sak, pga barn ikke bor med søker`() {
        val barnasIdenter = personopplysningGrunnlagMedUlikeAdresser.barna.map { it.personIdent.ident }
        val vurdering = AutomatiskVilkårsvurdering(morBosattIRiket = OppfyllerVilkår.JA,
                                                   barnErUnder18 = barnasIdenter.map { Pair(it, OppfyllerVilkår.JA) },
                                                   barnBorMedSøker = barnasIdenter.map { Pair(it, OppfyllerVilkår.NEI) },
                                                   barnErUgift = barnasIdenter.map { Pair(it, OppfyllerVilkår.JA) },
                                                   barnErBosattIRiket = barnasIdenter.map { Pair(it, OppfyllerVilkår.JA) })
        Assertions.assertEquals(false, vurdering.alleVilkårOppfylt())
        Assertions.assertEquals(vurdering, vilkårsvurdering(personopplysningGrunnlagMedUlikeAdresser, barnasIdenter))
    }

    @Test
    fun `En Ikke Godkjent sak, pga ett barn ikke bor med søker`() {
        val barnasIdenter = personopplysningGrunnlagMedUlikeAdresserForEtAvFlereBarn.barna.map { it.personIdent.ident }
        val vurdering = AutomatiskVilkårsvurdering(morBosattIRiket = OppfyllerVilkår.JA,
                                                   barnErUnder18 = barnasIdenter.map { Pair(it, OppfyllerVilkår.JA) },
                                                   barnBorMedSøker = listOf(Pair(barnasIdenter.first(), OppfyllerVilkår.NEI),
                                                                            Pair(barnasIdenter.last(), OppfyllerVilkår.JA)),
                                                   barnErUgift = barnasIdenter.map { Pair(it, OppfyllerVilkår.JA) },
                                                   barnErBosattIRiket = barnasIdenter.map { Pair(it, OppfyllerVilkår.JA) })
        Assertions.assertEquals(false, vurdering.alleVilkårOppfylt())
        Assertions.assertEquals(vurdering,
                                vilkårsvurdering(personopplysningGrunnlagMedUlikeAdresserForEtAvFlereBarn, barnasIdenter))
    }
}