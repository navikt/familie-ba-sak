package no.nav.familie.ba.sak.kjerne.automatiskVurdering

import no.nav.familie.ba.sak.kjerne.automatiskvurdering.AutomatiskVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.OppfyllerVilkår
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.barnBorMedSøker
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.barnErBosattIRiket
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.barnErUgift
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.barnUnder18
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.morBorIriket
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AutomatiskVilkårsvurderingUtilsTest {


    @Test
    fun `Godkjenner kun når mor har bostedsadresse som er gyldig nå`() {
        val morsManglendeBosted = null

        Assertions.assertEquals(true, morBorIriket(mockNåværendeBosted))
        Assertions.assertEquals(true, morBorIriket(mockTidløstBosted))
        Assertions.assertEquals(false, morBorIriket(mockTidligereBosted))
        Assertions.assertEquals(false, morBorIriket(morsManglendeBosted))

    }

    @Test
    fun `Godkjenner kun barn under 18 år`() {
        val barnUnder18 = LocalDate.now().minusYears(6)
        val barnOver18 = LocalDate.now().minusYears(20)

        Assertions.assertEquals(true, barnUnder18(listOf(barnUnder18)))
        Assertions.assertEquals(false, barnUnder18(listOf(barnOver18)))
        Assertions.assertEquals(false, barnUnder18(listOf(barnUnder18, barnOver18)))
    }

    @Test
    fun `Godkjenner kun når barn bor med søker`() {
        Assertions.assertEquals(true, barnBorMedSøker(listOf(mockNåværendeBosted), mockNåværendeBosted))
        Assertions.assertEquals(false, barnBorMedSøker(listOf(mockTidligereBosted), mockNåværendeBosted))
        Assertions.assertEquals(false, barnBorMedSøker(listOf(mockAnnetNåværendeBosted), mockNåværendeBosted))
        Assertions.assertEquals(false,
                                barnBorMedSøker(listOf(mockNåværendeBosted, mockAnnetNåværendeBosted), mockNåværendeBosted))
    }

    @Test
    fun `Godkjenner kun når barn er ugift`() {
        Assertions.assertEquals(true,
                                barnErUgift(listOf(GrSivilstand(type = SIVILSTAND.UGIFT,
                                                                person = personopplysningGrunnlagForGodkjentSak.barna.last()))))
        Assertions.assertEquals(true,
                                barnErUgift(listOf(GrSivilstand(type = SIVILSTAND.UOPPGITT,
                                                                person = personopplysningGrunnlagForGodkjentSak.barna.last()))))
        Assertions.assertEquals(false,
                                barnErUgift(listOf(GrSivilstand(type = SIVILSTAND.GIFT,
                                                                person = personopplysningGrunnlagForGodkjentSak.barna.last()))))
        Assertions.assertEquals(false,
                                barnErUgift(listOf(GrSivilstand(type = SIVILSTAND.SKILT,
                                                                person = personopplysningGrunnlagForGodkjentSak.barna.last()))))
    }

    @Test
    fun `Godkjenner kun når barn har bostedsadresse som er gyldig nå`() {
        val barnasManglendeBosted = mutableListOf<GrBostedsadresse?>(null)
        Assertions.assertEquals(true, barnErBosattIRiket(listOf(mockNåværendeBosted)))
        Assertions.assertEquals(true, barnErBosattIRiket(listOf(mockTidløstBosted)))
        Assertions.assertEquals(false, barnErBosattIRiket(listOf(mockTidligereBosted)))
        Assertions.assertEquals(false, barnErBosattIRiket(listOf(mockNåværendeBosted, mockTidligereBosted)))
        Assertions.assertEquals(false, barnErBosattIRiket(barnasManglendeBosted))
    }

    @Test
    fun `En Godkjent sak`() {
        val vurdering = AutomatiskVilkårsvurdering(morBosattIRiket = OppfyllerVilkår.JA,
                                                   barnErUnder18 = OppfyllerVilkår.JA,
                                                   barnBorMedSøker = OppfyllerVilkår.JA,
                                                   barnErUgift = OppfyllerVilkår.JA,
                                                   barnErBosattIRiket = OppfyllerVilkår.JA)
        Assertions.assertEquals(true, vurdering.alleVilkårOppfylt())
        Assertions.assertEquals(vurdering, vilkårsvurdering(personopplysningGrunnlagForGodkjentSak))
    }

    @Test
    fun `En Ikke Godkjent sak, pga mor ikke bor i riket`() {
        val vurdering = AutomatiskVilkårsvurdering(morBosattIRiket = OppfyllerVilkår.NEI,
                                                   barnErUnder18 = OppfyllerVilkår.JA,
                                                   barnBorMedSøker = OppfyllerVilkår.JA,
                                                   barnErUgift = OppfyllerVilkår.JA,
                                                   barnErBosattIRiket = OppfyllerVilkår.JA)
        Assertions.assertEquals(false, vurdering.alleVilkårOppfylt())
        Assertions.assertEquals(vurdering, vilkårsvurdering(personopplysningGrunnlagMedUtdatertAdresse))
    }

    @Test
    fun `En Ikke Godkjent sak, pga barn ikke bor med søker`() {
        val vurdering = AutomatiskVilkårsvurdering(morBosattIRiket = OppfyllerVilkår.JA,
                                                   barnErUnder18 = OppfyllerVilkår.JA,
                                                   barnBorMedSøker = OppfyllerVilkår.NEI,
                                                   barnErUgift = OppfyllerVilkår.JA,
                                                   barnErBosattIRiket = OppfyllerVilkår.JA)
        Assertions.assertEquals(false, vurdering.alleVilkårOppfylt())
        Assertions.assertEquals(vurdering, vilkårsvurdering(personopplysningGrunnlagMedUlikeAdresser))
    }
}