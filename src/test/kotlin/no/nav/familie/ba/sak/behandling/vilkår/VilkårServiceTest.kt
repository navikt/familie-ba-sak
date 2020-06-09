package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.steg.Vilkårsvurdering
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.e2e.DatabaseCleanupService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles


@SpringBootTest
@ActiveProfiles("dev")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VilkårServiceTest(
        @Autowired
        private val behandlingService: BehandlingService,

        @Autowired
        private val behandlingResultatService: BehandlingResultatService,

        @Autowired
        private val fagsakService: FagsakService,

        @Autowired
        private val persongrunnlagService: PersongrunnlagService,

        @Autowired
        private val vilkårService: VilkårService,

        @Autowired
        private val stegService: StegService,

        @Autowired
        private val databaseCleanupService: DatabaseCleanupService
) {


    @BeforeAll
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `vilkårsvurdering med kun JA blir innvilget`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val behandlingResultat = vilkårService.initierVilkårvurderingForBehandling(behandlingId = behandling.id,
                                                                                   bekreftEndringerViaFrontend = true)
        Assertions.assertEquals(2, behandlingResultat.personResultater.size)

        val behandlingSteg: Vilkårsvurdering = stegService.hentBehandlingSteg(StegType.VILKÅRSVURDERING) as Vilkårsvurdering
        Assertions.assertNotNull(behandlingSteg)

        Assertions.assertThrows(Feil::class.java) { behandlingSteg.postValiderSteg(behandling) }

        val barn: Person = personopplysningGrunnlag.barna.find { it.personIdent.ident == barnFnr }!!

        vurderBehandlingResultatTilInnvilget(behandlingResultat, barn)

        behandlingResultatService.oppdater(behandlingResultat)

        Assertions.assertDoesNotThrow { behandlingSteg.postValiderSteg(behandling) }
    }

    @Test
    fun `Skal automatisk lagre ny vilkårsvurdering over den gamle`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()
        val barnFnr2 = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val behandlingResultat = vilkårService.initierVilkårvurderingForBehandling(behandlingId = behandling.id,
                                                                                   bekreftEndringerViaFrontend = true)
        Assertions.assertEquals(2, behandlingResultat.personResultater.size)

        val personopplysningGrunnlagMedEkstraBarn =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr, barnFnr2))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlagMedEkstraBarn)

        val behandlingResultatMedEkstraBarn = vilkårService.initierVilkårvurderingForBehandling(behandlingId = behandling.id,
                                                                                                bekreftEndringerViaFrontend = true)
        Assertions.assertEquals(3, behandlingResultatMedEkstraBarn.personResultater.size)
    }

    @Test
    fun `vurder ugyldig vilkårsvurdering`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)
    }

    @Test
    fun `Valider gyldige vilkårspermutasjoner for barn og søker`() {
        Assertions.assertEquals(setOf(
                Vilkår.UNDER_18_ÅR,
                Vilkår.BOR_MED_SØKER,
                Vilkår.GIFT_PARTNERSKAP,
                Vilkår.BOSATT_I_RIKET,
                Vilkår.LOVLIG_OPPHOLD
        ), Vilkår.hentVilkårForPart(PersonType.BARN))

        Assertions.assertEquals(setOf(
                Vilkår.BOSATT_I_RIKET,
                Vilkår.LOVLIG_OPPHOLD
        ), Vilkår.hentVilkårForPart(PersonType.SØKER))
    }
}

