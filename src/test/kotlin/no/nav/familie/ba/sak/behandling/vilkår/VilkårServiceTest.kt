package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.domene.vilkår.VilkårService
import no.nav.familie.ba.sak.behandling.domene.vilkår.Vilkår
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.RestVilkårResultat
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.nare.core.evaluations.Resultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension


@SpringBootTest
@ExtendWith(SpringExtension::class)
@ActiveProfiles("dev")
class VilkårServiceTest {

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var fagsakService: FagsakService

    @Autowired
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Autowired
    private lateinit var vilkårService: VilkårService

    @Test
    fun `vurder gyldig vilkårsvurdering`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        val samletVilkårResultat =
                vilkårService.vurderVilkårOgLagResultat(personopplysningGrunnlag,
                                                        vilkårsvurderingKomplettForBarnOgSøker(fnr, listOf(barnFnr)),
                                                        behandling.id
                )
        Assertions.assertEquals(samletVilkårResultat.samletVilkårResultat.size,
                                vilkårsvurderingKomplettForBarnOgSøker(fnr, listOf(barnFnr)).size)
    }

    @Test
    fun `vurder ugyldig vilkårsvurdering`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        assertThrows<IllegalStateException> {
            vilkårService.vurderVilkårOgLagResultat(personopplysningGrunnlag,
                                                    vilkårsvurderingUtenKomplettBarnVurdering,
                                                    behandling.id
            )
        }

        assertThrows<IllegalStateException> {
            vilkårService.vurderVilkårOgLagResultat(personopplysningGrunnlag,
                                                    vilkårsvurderingUtenKomplettSøkerVurdering,
                                                    behandling.id
            )
        }
    }

    @Test
    fun `Valider gyldige vilkårspermutasjoner for barn og søker`() {
        Assertions.assertEquals(setOf(
                Vilkår.UNDER_18_ÅR_OG_BOR_MED_SØKER,
                Vilkår.BOSATT_I_RIKET_BARN,
                Vilkår.STØNADSPERIODE
        ), Vilkår.hentVilkårTyperForPart(PersonType.BARN))

        Assertions.assertEquals(setOf(
                Vilkår.BOSATT_I_RIKET_SØKER,
                Vilkår.STØNADSPERIODE
        ), Vilkår.hentVilkårTyperForPart(PersonType.SØKER))
    }
}

fun vilkårsvurderingKomplettForBarnOgSøker(søkerPersonIdent: String, barnPersonIdenter: List<String>): List<RestVilkårResultat> {
    val barnasVilkår = barnPersonIdenter.map {
        listOf(RestVilkårResultat(personIdent = it,
                                  vilkårType = Vilkår.UNDER_18_ÅR_OG_BOR_MED_SØKER,
                                  resultat = Resultat.JA),
               RestVilkårResultat(personIdent = it,
                                  vilkårType = Vilkår.BOSATT_I_RIKET_BARN,
                                  resultat = Resultat.JA),
               RestVilkårResultat(personIdent = it,
                                  vilkårType = Vilkår.STØNADSPERIODE,
                                  resultat = Resultat.JA))
    }.flatten()

    val vilkår = arrayListOf(
            RestVilkårResultat(personIdent = søkerPersonIdent,
                               vilkårType = Vilkår.BOSATT_I_RIKET_SØKER,
                               resultat = Resultat.JA),
            RestVilkårResultat(personIdent = søkerPersonIdent,
                               vilkårType = Vilkår.STØNADSPERIODE,
                               resultat = Resultat.JA))

    vilkår.addAll(barnasVilkår)
    return vilkår
}

val vilkårsvurderingUtenKomplettBarnVurdering = listOf(RestVilkårResultat(personIdent = "1",
                                                                          vilkårType = Vilkår.BOSATT_I_RIKET_BARN,
                                                                          resultat = Resultat.JA),
                                                       RestVilkårResultat(personIdent = "1",
                                                                          vilkårType = Vilkår.STØNADSPERIODE,
                                                                          resultat = Resultat.JA),
                                                       RestVilkårResultat(personIdent = "12345678910",
                                                                          vilkårType = Vilkår.UNDER_18_ÅR_OG_BOR_MED_SØKER,
                                                                          resultat = Resultat.JA))

val vilkårsvurderingUtenKomplettSøkerVurdering = listOf(RestVilkårResultat(personIdent = "1",
                                                                           vilkårType = Vilkår.BOSATT_I_RIKET_BARN,
                                                                           resultat = Resultat.JA),
                                                        RestVilkårResultat(personIdent = "12345678910",
                                                                           vilkårType = Vilkår.UNDER_18_ÅR_OG_BOR_MED_SØKER,
                                                                           resultat = Resultat.JA),
                                                        RestVilkårResultat(personIdent = "12345678910",
                                                                           vilkårType = Vilkår.BOSATT_I_RIKET_SØKER,
                                                                           resultat = Resultat.JA),
                                                        RestVilkårResultat(personIdent = "12345678910",
                                                                           vilkårType = Vilkår.STØNADSPERIODE,
                                                                           resultat = Resultat.JA))
