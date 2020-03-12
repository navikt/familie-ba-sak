package no.nav.familie.ba.sak.behandling.vilkår

import junit.framework.Assert.assertEquals
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.*
import no.nav.familie.ba.sak.behandling.domene.vilkår.*
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.nare.core.evaluations.Resultat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("dev")
class VilkårVurderingTest {

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var fagsakService: FagsakService

    @Autowired
    private lateinit var vilkårService: VilkårService

    @Autowired
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Test
    fun `Hent relevante vilkår for persontype BARN`() {
        val relevanteVilkår = Vilkår.hentVilkårForPart(PersonType.BARN)
        val vilkårForBarn = setOf(Vilkår.UNDER_18_ÅR_OG_BOR_MED_SØKER,
                                  Vilkår.STØNADSPERIODE,
                                  Vilkår.BOSATT_I_RIKET)
        assertEquals(vilkårForBarn, relevanteVilkår)
    }

    @Test
    fun `Hent relevante vilkår for persontype SØKER`() {
        val relevanteVilkår = Vilkår.hentVilkårForPart(PersonType.SØKER)
        val vilkårForSøker = setOf(Vilkår.STØNADSPERIODE,
                                  Vilkår.BOSATT_I_RIKET)
        assertEquals(vilkårForSøker, relevanteVilkår)
    }

    @Test
    fun `Hent relevante vilkår for saktype`() { //Banal test, legg til saktyper
        val relevanteVilkårSaktypeFinnes = Vilkår.hentVilkårForSakstype(SakType.VILKÅRGJELDERFOR)
        val relevanteVilkårSaktypeFinnesIkke = Vilkår.hentVilkårForSakstype(SakType.VILKÅRGJELDERIKKEFOR)
        val vilkårForSaktypeFinnes  = setOf(Vilkår.UNDER_18_ÅR_OG_BOR_MED_SØKER,
                                            Vilkår.STØNADSPERIODE,
                                            Vilkår.BOSATT_I_RIKET)
        val vilkårForSaktypFinnesIkke : Set<Vilkår> = emptySet()
        assertEquals(vilkårForSaktypeFinnes, relevanteVilkårSaktypeFinnes)
        assertEquals(vilkårForSaktypFinnesIkke, relevanteVilkårSaktypeFinnesIkke)
    }

    @Test
    fun `Hent relevante vilkår for persontype og saktype`() {
        val relevanteVilkår = Vilkår.hentVilkårFor(PersonType.BARN, SakType.VILKÅRGJELDERFOR)
        val vilkårForBarn = setOf(Vilkår.UNDER_18_ÅR_OG_BOR_MED_SØKER,
                                  Vilkår.STØNADSPERIODE,
                                  Vilkår.BOSATT_I_RIKET)
        assertEquals(vilkårForBarn, relevanteVilkår)
    }

    @Test
    fun `Hent og evaluer oppfylte vilkår`() {

        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        val samletVilkårResultat = vilkårService.vurderVilkårOgLagResultat(personopplysningGrunnlag = personopplysningGrunnlag,
                                                                           behandlingId = behandling.id)

        assertEquals(samletVilkårResultat.hentSamletVilkårResultat(), Resultat.JA)
    }

    @Test
    fun `Hent og evaluer ikke-oppfylte vilkår`() {

        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))

        //Legger til barn over 18 år
        personopplysningGrunnlag.personer.add(Person(aktørId = randomAktørId(),
                                                     personIdent = PersonIdent("11111111111"),
                                                     type = PersonType.BARN,
                                                     personopplysningGrunnlag = personopplysningGrunnlag,
                                                     fødselsdato = LocalDate.of(1980, 1, 1),
                                                     navn = "",
                                                     kjønn = Kjønn.MANN))

        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        val samletVilkårResultat = vilkårService.vurderVilkårOgLagResultat(personopplysningGrunnlag = personopplysningGrunnlag,
                                                                           behandlingId = behandling.id)

        assertEquals(samletVilkårResultat.hentSamletVilkårResultat(), Resultat.NEI)
    }

}



