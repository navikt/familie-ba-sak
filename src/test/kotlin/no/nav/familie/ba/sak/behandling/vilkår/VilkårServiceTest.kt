package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingOpprinnelse
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.restDomene.RestVilkårResultat
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.steg.Vilkårsvurdering
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.e2e.DatabaseCleanupService
import no.nav.nare.core.evaluations.Resultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate


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
        private val vilkårsvurderingMetrics: VilkårsvurderingMetrics,

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
                behandlingService.hentForrigeBehandlingSomErIverksatt(fagsakId = behandling.fagsak.id)

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val behandlingResultat = vilkårService.initierVilkårvurderingForBehandling(behandling = behandling,
                                                                                   bekreftEndringerViaFrontend = true,
                                                                                   forrigeBehandling = forrigeBehandlingSomErIverksatt)
        Assertions.assertEquals(2, behandlingResultat.personResultater.size)

        val behandlingSteg: Vilkårsvurdering = stegService.hentBehandlingSteg(StegType.VILKÅRSVURDERING) as Vilkårsvurdering
        Assertions.assertNotNull(behandlingSteg)

        Assertions.assertDoesNotThrow { behandlingSteg.postValiderSteg(behandling) }
    }

    @Test
    fun `Skal automatisk lagre ny vilkårsvurdering over den gamle`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()
        val barnFnr2 = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val forrigeBehandlingSomErIverksatt =
                behandlingService.hentForrigeBehandlingSomErIverksatt(fagsakId = behandling.fagsak.id)

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val behandlingResultat = vilkårService.initierVilkårvurderingForBehandling(behandling = behandling,
                                                                                   bekreftEndringerViaFrontend = true,
                                                                                   forrigeBehandling = forrigeBehandlingSomErIverksatt)
        Assertions.assertEquals(2, behandlingResultat.personResultater.size)

        val personopplysningGrunnlagMedEkstraBarn =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr, barnFnr2))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlagMedEkstraBarn)

        val behandlingResultatMedEkstraBarn = vilkårService.initierVilkårvurderingForBehandling(behandling = behandling,
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
    fun `Behandlingsresultat kopieres riktig`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val forrigeBehandlingSomErIverksatt =
                behandlingService.hentForrigeBehandlingSomErIverksatt(fagsakId = behandling.fagsak.id)

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val behandlingResultat = vilkårService.initierVilkårvurderingForBehandling(behandling = behandling,
                                                                                   bekreftEndringerViaFrontend = true,
                                                                                   forrigeBehandling = forrigeBehandlingSomErIverksatt)

        val kopiertBehandlingResultat = behandlingResultat.kopier()

        behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat = kopiertBehandlingResultat, loggHendelse = false)
        val behandlingsResultater = behandlingResultatService
                .hentBehandlingResultatForBehandling(behandlingId = behandling.id)

        Assertions.assertEquals(2, behandlingsResultater.size)
        Assertions.assertNotEquals(behandlingResultat.id, kopiertBehandlingResultat.id)
    }


    @Test
    fun `Behandlingsresultat fra forrige behandling kopieres riktig`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val forrigeBehandlingSomErIverksatt =
                behandlingService.hentForrigeBehandlingSomErIverksatt(fagsakId = behandling.fagsak.id)

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val behandlingResultat = vilkårService.initierVilkårvurderingForBehandling(behandling = behandling,
                                                                                   bekreftEndringerViaFrontend = true,
                                                                                   forrigeBehandling = forrigeBehandlingSomErIverksatt)

        val barn: Person = personopplysningGrunnlag.barna.find { it.personIdent.ident == barnFnr }!!
        vurderBehandlingResultatTilInnvilget(behandlingResultat, barn)

        behandlingResultatService.oppdater(behandlingResultat)
        behandling.steg = StegType.BEHANDLING_AVSLUTTET
        behandlingService.lagre(behandling)

        val barnFnr2 = randomFnr()

        val behandling2 = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))


        val personopplysningGrunnlag2 =
                lagTestPersonopplysningGrunnlag(behandling2.id, fnr, listOf(barnFnr, barnFnr2))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag2)

        val behandlingResultat2 = vilkårService.initierVilkårvurderingForBehandling(behandling = behandling2,
                                                                                    bekreftEndringerViaFrontend = true,
                                                                                    forrigeBehandling = behandling)

        Assertions.assertEquals(3, behandlingResultat2.personResultater.size)

        behandlingResultat2.personResultater.forEach { personResultat ->
            personResultat.vilkårResultater.forEach { vilkårResultat ->
                if (vilkårResultat.vilkårType == Vilkår.UNDER_18_ÅR) {
                    Assertions.assertEquals(Resultat.JA, vilkårResultat.resultat)
                } else {
                    Assertions.assertEquals(Resultat.JA, vilkårResultat.resultat)
                    if (personResultat.personIdent == barnFnr2) {
                        Assertions.assertEquals(behandling2.id, vilkårResultat.behandlingId)
                    } else {
                        Assertions.assertEquals(behandling.id, vilkårResultat.behandlingId)
                    }
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
                behandlingService.hentForrigeBehandlingSomErIverksatt(fagsakId = behandling.fagsak.id)

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val behandlingResultat = vilkårService.initierVilkårvurderingForBehandling(behandling = behandling,
                                                                                   bekreftEndringerViaFrontend = true,
                                                                                   forrigeBehandling = forrigeBehandlingSomErIverksatt)

        val barn: Person = personopplysningGrunnlag.barna.find { it.personIdent.ident == barnFnr }!!
        vurderBehandlingResultatTilInnvilget(behandlingResultat, barn)

        behandlingResultatService.oppdater(behandlingResultat)
        behandling.steg = StegType.BEHANDLING_AVSLUTTET
        behandlingService.lagre(behandling)

        val barnFnr2 = randomFnr()

        val behandling2 = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))


        val personopplysningGrunnlag2 =
                lagTestPersonopplysningGrunnlag(behandling2.id, fnr, listOf(barnFnr, barnFnr2))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag2)

        val behandlingResultat2 = vilkårService.initierVilkårvurderingForBehandling(behandling = behandling2,
                                                                                    bekreftEndringerViaFrontend = true,
                                                                                    forrigeBehandling = behandling)

        Assertions.assertEquals(3, behandlingResultat2.personResultater.size)

        val personResultat = behandlingResultat2.personResultater.find { it.personIdent == barnFnr }!!
        val borMedSøkerVilkår = personResultat.vilkårResultater.find { it.vilkårType == Vilkår.BOR_MED_SØKER }!!
        Assertions.assertEquals(behandling.id, borMedSøkerVilkår.behandlingId)

        VilkårsvurderingUtils.muterPersonResultatPut(personResultat,
                                                     RestVilkårResultat(borMedSøkerVilkår.id,
                                                                        Vilkår.BOR_MED_SØKER,
                                                                        Resultat.JA,
                                                                        LocalDate.of(2010, 6, 2),
                                                                        LocalDate.of(2011, 9, 1),
                                                                        ""))

        val behandlingResultatEtterEndring = behandlingResultatService.oppdater(behandlingResultat2)
        val personResultatEtterEndring = behandlingResultatEtterEndring.personResultater.find { it.personIdent == barnFnr }!!
        val borMedSøkerVilkårEtterEndring =
                personResultatEtterEndring.vilkårResultater.find { it.vilkårType == Vilkår.BOR_MED_SØKER }!!
        Assertions.assertEquals(behandling2.id, borMedSøkerVilkårEtterEndring.behandlingId)
    }

    @Test
    fun `Innvilget vilkårsvurdering skal generere metrikker`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, fnr, listOf(barnFnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val behandlingResultat = vilkårService.initierVilkårvurderingForBehandling(behandling = behandling,
                                                                                   bekreftEndringerViaFrontend = true)
        Assertions.assertEquals(2, behandlingResultat.personResultater.size)

        val bosattIRiketMetrikkSøkerJa = vilkårsvurderingMetrics.hentCounter(vilkår = Vilkår.BOSATT_I_RIKET.name,
                                                                             resultat = Resultat.JA,
                                                                             personType = PersonType.SØKER,
                                                                             behandlingOpprinnelse = BehandlingOpprinnelse.MANUELL)

        Assertions.assertTrue(1 >= bosattIRiketMetrikkSøkerJa?.count()?.toInt()!!)

        val bosattIRiketMetrikkBarnJa = vilkårsvurderingMetrics.hentCounter(vilkår = Vilkår.BOSATT_I_RIKET.name,
                                                                            resultat = Resultat.JA,
                                                                            personType = PersonType.BARN,
                                                                            behandlingOpprinnelse = BehandlingOpprinnelse.MANUELL)
        Assertions.assertTrue(1 >= bosattIRiketMetrikkBarnJa?.count()?.toInt()!!)

        val bosattIRiketMetrikkSøkerNei = vilkårsvurderingMetrics.hentCounter(vilkår = Vilkår.BOSATT_I_RIKET.name,
                                                                              resultat = Resultat.NEI,
                                                                              personType = PersonType.SØKER,
                                                                              behandlingOpprinnelse = BehandlingOpprinnelse.MANUELL)

        Assertions.assertEquals(0, bosattIRiketMetrikkSøkerNei?.count()?.toInt())

        val bosattIRiketMetrikkBarnNei = vilkårsvurderingMetrics.hentCounter(vilkår = Vilkår.BOSATT_I_RIKET.name,
                                                                             resultat = Resultat.NEI,
                                                                             personType = PersonType.BARN,
                                                                             behandlingOpprinnelse = BehandlingOpprinnelse.MANUELL)
        Assertions.assertEquals(0, bosattIRiketMetrikkBarnNei?.count()?.toInt())
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

