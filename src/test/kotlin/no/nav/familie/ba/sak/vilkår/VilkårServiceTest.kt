package no.nav.familie.ba.sak.vilkår

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.domene.vilkår.UtfallType
import no.nav.familie.ba.sak.behandling.domene.vilkår.VilkårService
import no.nav.familie.ba.sak.behandling.domene.vilkår.VilkårType
import no.nav.familie.ba.sak.behandling.restDomene.RestVilkårResultat
import no.nav.familie.ba.sak.lagRandomSaksnummer
import no.nav.familie.ba.sak.lagTestPersonopplysningGrunnlag
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
        private val behandlingService: BehandlingService,

        @Autowired
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,

        @Autowired
        private val vilkårService: VilkårService
) {

    @Test
    fun `vurder gyldig vilkårsvurdering`() {
        val fagsak = behandlingService.hentEllerOpprettFagsakForPersonIdent("1")
        val behandling = behandlingService.opprettNyBehandlingPåFagsak(fagsak,
                                                                       "sdf",
                                                                       BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                       lagRandomSaksnummer())

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id!!, "1", "12345678910")
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        val samletVilkårResultat =
                vilkårService.vurderVilkår(personopplysningGrunnlag, vilkårsvurderingKomplettForBarnOgSøker("1", listOf("12345678910")))
        Assertions.assertEquals(samletVilkårResultat.samletVilkårResultat.size,
                                vilkårsvurderingKomplettForBarnOgSøker("1", listOf("12345678910")).size)
    }

    @Test
    fun `vurder ugyldig vilkårsvurdering`() {
        val fagsak = behandlingService.hentEllerOpprettFagsakForPersonIdent("1")
        val behandling = behandlingService.opprettNyBehandlingPåFagsak(fagsak,
                                                                       "sdf",
                                                                       BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                       lagRandomSaksnummer())

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id!!, "1", "12345678910")
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        assertThrows<IllegalStateException> {
            vilkårService.vurderVilkår(personopplysningGrunnlag,
                                       vilkårsvurderingUtenKomplettBarnVurdering)
        }

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

fun vilkårsvurderingKomplettForBarnOgSøker(søkerPersonIdent: String, barnPersonIdenter: List<String>): List<RestVilkårResultat> {
    val barnasVilkår = barnPersonIdenter.map {
        listOf(RestVilkårResultat(personIdent = it,
                                  vilkårType = VilkårType.UNDER_18_ÅR_OG_BOR_MED_SØKER,
                                  utfallType = UtfallType.OPPFYLT),
               RestVilkårResultat(personIdent = it,
                                  vilkårType = VilkårType.BOSATT_I_RIKET,
                                  utfallType = UtfallType.OPPFYLT),
               RestVilkårResultat(personIdent = it,
                                  vilkårType = VilkårType.STØNADSPERIODE,
                                  utfallType = UtfallType.OPPFYLT))
    }.flatten()

    val vilkår = arrayListOf(
            RestVilkårResultat(personIdent = søkerPersonIdent,
                               vilkårType = VilkårType.BOSATT_I_RIKET,
                               utfallType = UtfallType.OPPFYLT),
            RestVilkårResultat(personIdent = søkerPersonIdent,
                               vilkårType = VilkårType.STØNADSPERIODE,
                               utfallType = UtfallType.OPPFYLT))

    vilkår.addAll(barnasVilkår)
    return vilkår
}

val vilkårsvurderingUtenKomplettBarnVurdering = listOf(RestVilkårResultat(personIdent = "1",
                                                                          vilkårType = VilkårType.BOSATT_I_RIKET,
                                                                          utfallType = UtfallType.OPPFYLT),
                                                       RestVilkårResultat(personIdent = "1",
                                                                          vilkårType = VilkårType.STØNADSPERIODE,
                                                                          utfallType = UtfallType.OPPFYLT),
                                                       RestVilkårResultat(personIdent = "12345678910",
                                                                          vilkårType = VilkårType.UNDER_18_ÅR_OG_BOR_MED_SØKER,
                                                                          utfallType = UtfallType.OPPFYLT))

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
