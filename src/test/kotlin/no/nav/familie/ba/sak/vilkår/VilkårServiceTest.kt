package no.nav.familie.ba.sak.vilkår

import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.domene.vilkår.UtfallType
import no.nav.familie.ba.sak.behandling.domene.vilkår.VilkårService
import no.nav.familie.ba.sak.behandling.domene.vilkår.VilkårType
import no.nav.familie.ba.sak.behandling.restDomene.RestVilkårResultat
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate


@SpringBootTest
@ExtendWith(SpringExtension::class)
@ActiveProfiles("dev")
class VilkårServiceTest(
        @Autowired
        private val vilkårService: VilkårService
) {
    @Test
    fun `vurder gyldig vilkårsvurdering`() {
        val personopplysningGrunnlag = PersonopplysningGrunnlag(1L)
        val søker = Person(personIdent = PersonIdent("1"),
                           type = PersonType.SØKER,
                           personopplysningGrunnlag = personopplysningGrunnlag,
                           fødselsdato = LocalDate.now())
        val barn = Person(personIdent = PersonIdent("12345678910"),
                          type = PersonType.BARN,
                          personopplysningGrunnlag = personopplysningGrunnlag,
                          fødselsdato = LocalDate.now())

        personopplysningGrunnlag.leggTilPerson(søker)
        personopplysningGrunnlag.leggTilPerson(barn)

        val vilkårsvurdering = listOf(RestVilkårResultat(personIdent = "1",
                                                         vilkårType = VilkårType.BOSATT_I_RIKET,
                                                         utfallType = UtfallType.OPPFYLT),
                                      RestVilkårResultat(personIdent = "1",
                                                         vilkårType = VilkårType.STØNADSPERIODE,
                                                         utfallType = UtfallType.OPPFYLT),
                                      RestVilkårResultat(personIdent = "12345678910",
                                                         vilkårType = VilkårType.UNDER_18_ÅR_OG_BOR_MED_SØKER,
                                                         utfallType = UtfallType.OPPFYLT),
                                      RestVilkårResultat(personIdent = "12345678910",
                                                         vilkårType = VilkårType.BOSATT_I_RIKET,
                                                         utfallType = UtfallType.OPPFYLT),
                                      RestVilkårResultat(personIdent = "12345678910",
                                                         vilkårType = VilkårType.STØNADSPERIODE,
                                                         utfallType = UtfallType.OPPFYLT))

        val samletVilkårResultat = vilkårService.vurderVilkår(personopplysningGrunnlag, vilkårsvurdering)
        Assertions.assertEquals(samletVilkårResultat.samletVilkårResultat.size, vilkårsvurdering.size)
    }

    @Test
    fun `vurder ugyldig vilkårsvurdering`() {
        val personopplysningGrunnlag = PersonopplysningGrunnlag(1L)
        val søker = Person(personIdent = PersonIdent("1"),
                           type = PersonType.SØKER,
                           personopplysningGrunnlag = personopplysningGrunnlag,
                           fødselsdato = LocalDate.now())
        val barn = Person(personIdent = PersonIdent("12345678910"),
                          type = PersonType.BARN,
                          personopplysningGrunnlag = personopplysningGrunnlag,
                          fødselsdato = LocalDate.now())

        personopplysningGrunnlag.leggTilPerson(søker)
        personopplysningGrunnlag.leggTilPerson(barn)

        val vilkårsvurderingUtenKomplettBarnVurdering = listOf(RestVilkårResultat(personIdent = "1",
                                                                                  vilkårType = VilkårType.BOSATT_I_RIKET,
                                                                                  utfallType = UtfallType.OPPFYLT),
                                                               RestVilkårResultat(personIdent = "1",
                                                                                  vilkårType = VilkårType.STØNADSPERIODE,
                                                                                  utfallType = UtfallType.OPPFYLT),
                                                               RestVilkårResultat(personIdent = "12345678910",
                                                                                  vilkårType = VilkårType.UNDER_18_ÅR_OG_BOR_MED_SØKER,
                                                                                  utfallType = UtfallType.OPPFYLT))

        assertThrows<IllegalStateException> {
            vilkårService.vurderVilkår(personopplysningGrunnlag,
                                       vilkårsvurderingUtenKomplettBarnVurdering)
        }

        val vilkårsvurderingUtenKomplettSøkerVurdering = listOf(RestVilkårResultat(personIdent = "1",
                                                                                   vilkårType = VilkårType.BOSATT_I_RIKET,
                                                                                   utfallType = UtfallType.OPPFYLT),
                                                                RestVilkårResultat(personIdent = "12345678910",
                                                                                   vilkårType = VilkårType.UNDER_18_ÅR_OG_BOR_MED_SØKER,
                                                                                   utfallType = UtfallType.OPPFYLT),
                                                                RestVilkårResultat(personIdent = "12345678910",
                                                                                   vilkårType = VilkårType.BOSATT_I_RIKET,
                                                                                   utfallType = UtfallType.OPPFYLT),
                                                                RestVilkårResultat(personIdent = "12345678910",
                                                                                   vilkårType = VilkårType.STØNADSPERIODE,
                                                                                   utfallType = UtfallType.OPPFYLT))

        assertThrows<IllegalStateException> {
            vilkårService.vurderVilkår(personopplysningGrunnlag,
                                       vilkårsvurderingUtenKomplettSøkerVurdering)
        }
    }

    @Test
    fun `Valider gyldige vilkårspermutasjoner for barn og søker`() {
        Assertions.assertEquals(listOf(
                VilkårType.UNDER_18_ÅR_OG_BOR_MED_SØKER,
                VilkårType.BOSATT_I_RIKET,
                VilkårType.STØNADSPERIODE
        ), VilkårType.hentVilkårForPart(PersonType.BARN))

        Assertions.assertEquals(listOf(
                VilkårType.BOSATT_I_RIKET,
                VilkårType.STØNADSPERIODE
        ), VilkårType.hentVilkårForPart(PersonType.SØKER))
    }
}