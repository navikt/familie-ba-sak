package no.nav.familie.ba.sak

import junit.framework.Assert.assertEquals
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.*
import no.nav.familie.ba.sak.behandling.domene.vilkår.*
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.nare.core.evaluations.Resultat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("dev")
class NareTest {

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var fagsakService: FagsakService

    @Autowired
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Test
    fun `Hent relevante vilkår for persontype med alt i en klasse`() {
        val relevanteVilkår = Vilkår.hentVilkårFor(PersonType.BARN, "TESTSAKSTYPE")
        val vilkårForBarn = setOf(Vilkår.UNDER_18_ÅR_OG_BOR_MED_SØKER,
                                  Vilkår.STØNADSPERIODE,
                                  Vilkår.BOSATT_I_RIKET_BARN)
        assertEquals(vilkårForBarn, relevanteVilkår)
    }


    @Test
    fun `Hent og evaluer vilkår for persontype`() {

        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        val samletVilkårResultat = SamletVilkårResultat(behandlingId = behandling.id, samletVilkårResultat = mutableSetOf())
        personopplysningGrunnlag.personer.map { person ->
            val relevanteVilkårForBarn = Vilkår.hentVilkårFor(person.type, "TESTSAKSTYPE")
            val samletSpesifikasjon = relevanteVilkårForBarn
                    .map { vilkår -> vilkår.spesifikasjon }
                    .reduce { samledeVilkår, vilkår -> samledeVilkår og vilkår }
            val evaluering = samletSpesifikasjon.evaluer(Fakta(personopplysningGrunnlag))
            evaluering.children.map { child ->
                samletVilkårResultat.samletVilkårResultat.add(VilkårResultat(person = person,
                                                                             resultat = child.resultat,
                                                                             vilkårType = Vilkår.valueOf(child.identifikator)))
            }
        }

        assertEquals(samletVilkårResultat.hentSamletVilkårResultat(), Resultat.JA)


    }

}



