package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.behandling.restDomene.RestVilkårResultat
import no.nav.familie.ba.sak.behandling.restDomene.tilRestPersonResultat
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.steg.VilkårsvurderingSteg
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.vurderVilkårsvurderingTilInnvilget
import no.nav.familie.ba.sak.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.nare.Resultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalDateTime


@SpringBootTest
@ActiveProfiles("dev", "mock-pdl", "mock-arbeidsfordeling")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VilkårServiceTest(
        @Autowired
        private val behandlingService: BehandlingService,

        @Autowired
        private val vilkårsvurderingService: VilkårsvurderingService,

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
    fun `vilkårsvurdering med kun JA automatisk behandlet blir innvilget`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val forrigeBehandlingSomErIverksatt =
                behandlingService.hentSisteBehandlingSomErIverksatt(fagsakId = behandling.fagsak.id)

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vilkårsvurdering = vilkårService.initierVilkårsvurderingForBehandling(behandling = behandling,
                                                                                  bekreftEndringerViaFrontend = true,
                                                                                  forrigeBehandling = forrigeBehandlingSomErIverksatt)
        Assertions.assertEquals(2, vilkårsvurdering.personResultater.size)


        vilkårsvurdering.personResultater.map { personResultat ->
            personResultat.tilRestPersonResultat().vilkårResultater.map {
                vilkårService.endreVilkår(behandlingId = behandling.id, vilkårId = it.id,
                                          restPersonResultat =
                                          RestPersonResultat(personIdent = personResultat.personIdent,
                                                             vilkårResultater = listOf(it.copy(
                                                                     resultat = Resultat.OPPFYLT,
                                                                     periodeFom = LocalDate.of(2019, 5, 8)
                                                             ))))
            }
        }

        val behandlingSteg: VilkårsvurderingSteg =
                stegService.hentBehandlingSteg(StegType.VILKÅRSVURDERING) as VilkårsvurderingSteg
        Assertions.assertNotNull(behandlingSteg)

        behandlingSteg.utførStegOgAngiNeste(behandling, "")
        Assertions.assertDoesNotThrow { behandlingSteg.postValiderSteg(behandling) }
    }

    @Test
    fun `Manuell vilkårsvurdering skal få erAutomatiskVurdert på enkelte vilkår`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val forrigeBehandlingSomErIverksatt =
                behandlingService.hentSisteBehandlingSomErIverksatt(fagsakId = behandling.fagsak.id)

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vilkårsvurdering = vilkårService.initierVilkårsvurderingForBehandling(behandling = behandling,
                                                                                  bekreftEndringerViaFrontend = true,
                                                                                  forrigeBehandling = forrigeBehandlingSomErIverksatt)
        vilkårsvurdering.personResultater.forEach { personResultat ->
            personResultat.vilkårResultater.forEach { vilkårResultat ->
                when (vilkårResultat.vilkårType) {
                    Vilkår.UNDER_18_ÅR, Vilkår.GIFT_PARTNERSKAP -> assertTrue(vilkårResultat.erAutomatiskVurdert)
                    else -> assertFalse(vilkårResultat.erAutomatiskVurdert)
                }
            }
        }
    }

    @Test
    fun `Endring på automatisk vurderte vilkår(manuell vilkårsvurdering) skal settes til manuell ved endring`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val forrigeBehandlingSomErIverksatt =
                behandlingService.hentSisteBehandlingSomErIverksatt(fagsakId = behandling.fagsak.id)

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vilkårsvurdering = vilkårService.initierVilkårsvurderingForBehandling(behandling = behandling,
                                                                                  bekreftEndringerViaFrontend = true,
                                                                                  forrigeBehandling = forrigeBehandlingSomErIverksatt)
        val under18ÅrVilkårForBarn =
                vilkårsvurdering.personResultater.find { it.personIdent === barnFnr }
                        ?.tilRestPersonResultat()?.vilkårResultater?.find { it.vilkårType == Vilkår.UNDER_18_ÅR }

        val endretVilkårsvurdering: List<RestPersonResultat> =
                vilkårService.endreVilkår(behandlingId = behandling.id, vilkårId = under18ÅrVilkårForBarn!!.id,
                                          restPersonResultat =
                                          RestPersonResultat(personIdent = barnFnr,
                                                             vilkårResultater = listOf(under18ÅrVilkårForBarn.copy(
                                                                     resultat = Resultat.OPPFYLT,
                                                                     periodeFom = LocalDate.of(2019, 5, 8)
                                                             ))))

        val endretUnder18ÅrVilkårForBarn =
                endretVilkårsvurdering.find { it.personIdent === barnFnr }
                        ?.vilkårResultater?.find { it.vilkårType == Vilkår.UNDER_18_ÅR }
        assertFalse(endretUnder18ÅrVilkårForBarn!!.erAutomatiskVurdert)
    }

    @Test
    fun `Skal automatisk lagre ny vilkårsvurdering over den gamle`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()
        val barnFnr2 = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val forrigeBehandlingSomErIverksatt =
                behandlingService.hentSisteBehandlingSomErIverksatt(fagsakId = behandling.fagsak.id)

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vilkårsvurdering = vilkårService.initierVilkårsvurderingForBehandling(behandling = behandling,
                                                                                  bekreftEndringerViaFrontend = true,
                                                                                  forrigeBehandling = forrigeBehandlingSomErIverksatt)
        Assertions.assertEquals(2, vilkårsvurdering.personResultater.size)

        val personopplysningGrunnlagMedEkstraBarn =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr, barnFnr2))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlagMedEkstraBarn)

        val behandlingResultatMedEkstraBarn = vilkårService.initierVilkårsvurderingForBehandling(behandling = behandling,
                                                                                                 bekreftEndringerViaFrontend = true,
                                                                                                 forrigeBehandling = forrigeBehandlingSomErIverksatt)
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
    fun `Vilkårsvurdering kopieres riktig`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val forrigeBehandlingSomErIverksatt =
                behandlingService.hentSisteBehandlingSomErIverksatt(fagsakId = behandling.fagsak.id)

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vilkårsvurdering = vilkårService.initierVilkårsvurderingForBehandling(behandling = behandling,
                                                                                  bekreftEndringerViaFrontend = true,
                                                                                  forrigeBehandling = forrigeBehandlingSomErIverksatt)

        val kopiertBehandlingResultat = vilkårsvurdering.kopier()

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = kopiertBehandlingResultat)
        val behandlingsResultater = vilkårsvurderingService
                .hentBehandlingResultatForBehandling(behandlingId = behandling.id)

        Assertions.assertEquals(2, behandlingsResultater.size)
        Assertions.assertNotEquals(vilkårsvurdering.id, kopiertBehandlingResultat.id)
    }


    @Test
    fun `Vilkårsvurdering fra forrige behandling kopieres riktig`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val forrigeBehandlingSomErIverksatt =
                behandlingService.hentSisteBehandlingSomErIverksatt(fagsakId = behandling.fagsak.id)

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vilkårsvurdering = vilkårService.initierVilkårsvurderingForBehandling(behandling = behandling,
                                                                                  bekreftEndringerViaFrontend = true,
                                                                                  forrigeBehandling = forrigeBehandlingSomErIverksatt)
        Assertions.assertEquals(2, vilkårsvurdering.personResultater.size)


        vilkårsvurdering.personResultater.map { personResultat ->
            personResultat.tilRestPersonResultat().vilkårResultater.map {
                vilkårService.endreVilkår(behandlingId = behandling.id, vilkårId = it.id,
                                          restPersonResultat =
                                          RestPersonResultat(personIdent = personResultat.personIdent,
                                                             vilkårResultater = listOf(it.copy(
                                                                     resultat = Resultat.OPPFYLT,
                                                                     periodeFom = LocalDate.of(2019, 5, 8)
                                                             ))))
            }
        }

        behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BEHANDLING_AVSLUTTET))
        behandlingService.lagreEllerOppdater(behandling)

        val barnFnr2 = randomFnr()

        val behandling2 = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))


        val personopplysningGrunnlag2 =
                lagTestPersonopplysningGrunnlag(behandling2.id, fnr, listOf(barnFnr, barnFnr2))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag2)

        val behandlingResultat2 = vilkårService.initierVilkårsvurderingForBehandling(behandling = behandling2,
                                                                                     bekreftEndringerViaFrontend = true,
                                                                                     forrigeBehandling = behandling)

        Assertions.assertEquals(3, behandlingResultat2.personResultater.size)

        behandlingResultat2.personResultater.forEach { personResultat ->
            personResultat.vilkårResultater.forEach { vilkårResultat ->
                if (personResultat.personIdent == barnFnr2) {
                    Assertions.assertEquals(behandling2.id, vilkårResultat.behandlingId)
                } else {
                    Assertions.assertEquals(Resultat.OPPFYLT, vilkårResultat.resultat)
                    Assertions.assertEquals(behandling.id, vilkårResultat.behandlingId)
                }
            }
        }
    }

    @Test
    fun `Peker til behandling oppdateres ved vurdering av revurdering`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val forrigeBehandlingSomErIverksatt =
                behandlingService.hentSisteBehandlingSomErIverksatt(fagsakId = behandling.fagsak.id)

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vilkårsvurdering = vilkårService.initierVilkårsvurderingForBehandling(behandling = behandling,
                                                                                  bekreftEndringerViaFrontend = true,
                                                                                  forrigeBehandling = forrigeBehandlingSomErIverksatt)

        val barn: Person = personopplysningGrunnlag.barna.find { it.personIdent.ident == barnFnr }!!
        vurderVilkårsvurderingTilInnvilget(vilkårsvurdering, barn)

        vilkårsvurderingService.oppdater(vilkårsvurdering)
        behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BEHANDLING_AVSLUTTET))
        behandlingService.lagreEllerOppdater(behandling)

        val barnFnr2 = randomFnr()

        val behandling2 = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))


        val personopplysningGrunnlag2 =
                lagTestPersonopplysningGrunnlag(behandling2.id, fnr, listOf(barnFnr, barnFnr2))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag2)

        val behandlingResultat2 = vilkårService.initierVilkårsvurderingForBehandling(behandling = behandling2,
                                                                                     bekreftEndringerViaFrontend = true,
                                                                                     forrigeBehandling = behandling)

        Assertions.assertEquals(3, behandlingResultat2.personResultater.size)

        val personResultat = behandlingResultat2.personResultater.find { it.personIdent == barnFnr }!!
        val borMedSøkerVilkår = personResultat.vilkårResultater.find { it.vilkårType == Vilkår.BOR_MED_SØKER }!!
        Assertions.assertEquals(behandling.id, borMedSøkerVilkår.behandlingId)

        VilkårsvurderingUtils.muterPersonResultatPut(personResultat,
                                                     RestVilkårResultat(borMedSøkerVilkår.id,
                                                                        Vilkår.BOR_MED_SØKER,
                                                                        Resultat.OPPFYLT,
                                                                        LocalDate.of(2010, 6, 2),
                                                                        LocalDate.of(2011, 9, 1),
                                                                        "",
                                                                        "",
                                                                        LocalDateTime.now(),
                                                                        behandling.id))

        val behandlingResultatEtterEndring = vilkårsvurderingService.oppdater(behandlingResultat2)
        val personResultatEtterEndring = behandlingResultatEtterEndring.personResultater.find { it.personIdent == barnFnr }!!
        val borMedSøkerVilkårEtterEndring =
                personResultatEtterEndring.vilkårResultater.find { it.vilkårType == Vilkår.BOR_MED_SØKER }!!
        Assertions.assertEquals(behandling2.id, borMedSøkerVilkårEtterEndring.behandlingId)
    }

    @Test
    fun `Valider gyldige vilkårspermutasjoner for barn og søker`() {
        Assertions.assertEquals(setOf(
                Vilkår.UNDER_18_ÅR,
                Vilkår.BOR_MED_SØKER,
                Vilkår.GIFT_PARTNERSKAP,
                Vilkår.BOSATT_I_RIKET,
                Vilkår.LOVLIG_OPPHOLD
        ), Vilkår.hentVilkårFor(PersonType.BARN))

        Assertions.assertEquals(setOf(
                Vilkår.BOSATT_I_RIKET,
                Vilkår.LOVLIG_OPPHOLD
        ), Vilkår.hentVilkårFor(PersonType.SØKER))
    }
}

