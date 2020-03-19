package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.RestPersonVilkårResultat
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
import java.time.LocalDate


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

        val behandlingResultat =
                vilkårService.kontrollerVurderteVilkårOgLagResultat(personopplysningGrunnlag,
                                                                    vilkårsvurderingKomplettForBarnOgSøker(fnr, listOf(barnFnr)),
                                                                    behandling.id
                )
        Assertions.assertEquals(behandlingResultat.behandlingResultat.size,
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
            vilkårService.kontrollerVurderteVilkårOgLagResultat(personopplysningGrunnlag,
                                                                vilkårsvurderingUtenKomplettBarnVurdering,
                                                                behandling.id
            )
        }

        assertThrows<IllegalStateException> {
            vilkårService.kontrollerVurderteVilkårOgLagResultat(personopplysningGrunnlag,
                                                                vilkårsvurderingUtenKomplettSøkerVurdering,
                                                                behandling.id
            )
        }
    }

    @Test
    fun `Valider gyldige vilkårspermutasjoner for barn og søker`() {
        Assertions.assertEquals(setOf(
                Vilkår.UNDER_18_ÅR_OG_BOR_MED_SØKER,
                Vilkår.BOSATT_I_RIKET,
                Vilkår.STØNADSPERIODE
        ), Vilkår.hentVilkårForPart(PersonType.BARN))

        Assertions.assertEquals(setOf(
                Vilkår.BOSATT_I_RIKET,
                Vilkår.STØNADSPERIODE
        ), Vilkår.hentVilkårForPart(PersonType.SØKER))
    }
}

fun vilkårsvurderingKomplettForBarnOgSøker(søkerPersonIdent: String,
                                           barnPersonIdenter: List<String>): List<RestPersonVilkårResultat> {
    val søkerVilkår = RestPersonVilkårResultat(
            personIdent = søkerPersonIdent,
            vurderteVilkår = listOf(
                    RestVilkårResultat(vilkårType = Vilkår.BOSATT_I_RIKET,
                                       resultat = Resultat.JA,
                                       fom = LocalDate.now(),
                                       tom = LocalDate.now().plusYears(10),
                                       begrunnelse = "OK"),
                    RestVilkårResultat(vilkårType = Vilkår.STØNADSPERIODE,
                                       resultat = Resultat.JA,
                                       fom = LocalDate.now(),
                                       tom = LocalDate.now().plusYears(10),
                                       begrunnelse = "OK")))

    val barnasVilkår = barnPersonIdenter.map {
        RestPersonVilkårResultat(
                personIdent = it,
                vurderteVilkår = listOf(
                        RestVilkårResultat(vilkårType = Vilkår.UNDER_18_ÅR_OG_BOR_MED_SØKER,
                                           resultat = Resultat.JA,
                                           fom = LocalDate.now(),
                                           tom = LocalDate.now().plusYears(10),
                                           begrunnelse = "OK"),
                        RestVilkårResultat(vilkårType = Vilkår.BOSATT_I_RIKET,
                                           resultat = Resultat.JA,
                                           fom = LocalDate.now(),
                                           tom = LocalDate.now().plusYears(10),
                                           begrunnelse = "OK"),
                        RestVilkårResultat(vilkårType = Vilkår.STØNADSPERIODE,
                                           resultat = Resultat.JA,
                                           fom = LocalDate.now(),
                                           tom = LocalDate.now().plusYears(10),
                                           begrunnelse = "OK")))
    }.toTypedArray()

    return listOf(søkerVilkår, *barnasVilkår)
}


val vilkårsvurderingUtenKomplettBarnVurdering = listOf(
        RestPersonVilkårResultat(
                personIdent = "1",
                vurderteVilkår = listOf(
                        RestVilkårResultat(vilkårType = Vilkår.UNDER_18_ÅR_OG_BOR_MED_SØKER,
                                           resultat = Resultat.JA,
                                           fom = LocalDate.now(),
                                           tom = LocalDate.now().plusYears(10),
                                           begrunnelse = "OK"))),
        RestPersonVilkårResultat(
                personIdent = "12345678910",
                vurderteVilkår = listOf(
                        RestVilkårResultat(vilkårType = Vilkår.BOSATT_I_RIKET,
                                           resultat = Resultat.JA,
                                           fom = LocalDate.now(),
                                           tom = LocalDate.now().plusYears(10),
                                           begrunnelse = "OK"),
                        RestVilkårResultat(vilkårType = Vilkår.STØNADSPERIODE,
                                           resultat = Resultat.JA,
                                           fom = LocalDate.now(),
                                           tom = LocalDate.now().plusYears(10),
                                           begrunnelse = "OK"))))

val vilkårsvurderingUtenKomplettSøkerVurdering = listOf(
        RestPersonVilkårResultat(
                personIdent = "1",
                vurderteVilkår = listOf(
                        RestVilkårResultat(vilkårType = Vilkår.BOSATT_I_RIKET,
                                           resultat = Resultat.JA,
                                           fom = LocalDate.now(),
                                           tom = LocalDate.now().plusYears(10),
                                           begrunnelse = "OK"))),
        RestPersonVilkårResultat(
                personIdent = "12345678910",
                vurderteVilkår = listOf(
                        RestVilkårResultat(vilkårType = Vilkår.UNDER_18_ÅR_OG_BOR_MED_SØKER,
                                           resultat = Resultat.JA,
                                           fom = LocalDate.now(),
                                           tom = LocalDate.now().plusYears(10),
                                           begrunnelse = "OK"),
                        RestVilkårResultat(vilkårType = Vilkår.BOSATT_I_RIKET,
                                           resultat = Resultat.JA,
                                           fom = LocalDate.now(),
                                           tom = LocalDate.now().plusYears(10),
                                           begrunnelse = "OK"),
                        RestVilkårResultat(vilkårType = Vilkår.STØNADSPERIODE,
                                           resultat = Resultat.JA,
                                           fom = LocalDate.now(),
                                           tom = LocalDate.now().plusYears(10),
                                           begrunnelse = "OK"))))
