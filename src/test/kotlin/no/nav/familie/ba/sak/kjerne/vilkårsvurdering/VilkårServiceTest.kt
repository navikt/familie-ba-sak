package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.common.nyOrdinærBehandling
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.vurderVilkårsvurderingTilInnvilget
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.RestVilkårResultat
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPersonResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime

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
    private val databaseCleanupService: DatabaseCleanupService,

    @Autowired
    private val personidentService: PersonidentService,

) : AbstractSpringIntegrationTest() {

    @BeforeAll
    fun init() {
        databaseCleanupService.truncate()
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
            lagTestPersonopplysningGrunnlag(
                behandling.id, fnr, listOf(barnFnr),
                søkerAktør = personidentService.hentOgLagreAktør(fnr),
                barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr))
            )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vilkårsvurdering = vilkårService.initierVilkårsvurderingForBehandling(
            behandling = behandling,
            bekreftEndringerViaFrontend = true,
            forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErIverksatt
        )
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

        fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(fnr))
        val forrigeBehandlingSomErIverksatt =
            behandlingService.hentSisteBehandlingSomErIverksatt(fagsakId = behandling.fagsak.id)

        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id, fnr, listOf(barnFnr),
                søkerAktør = personidentService.hentOgLagreAktør(fnr),
                barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr))
            )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vilkårsvurdering = vilkårService.initierVilkårsvurderingForBehandling(
            behandling = behandling,
            bekreftEndringerViaFrontend = true,
            forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErIverksatt
        )
        val under18ÅrVilkårForBarn =
            vilkårsvurdering.personResultater.find { it.aktør.aktivIdent() == barnFnr }
                ?.tilRestPersonResultat()?.vilkårResultater?.find { it.vilkårType == Vilkår.UNDER_18_ÅR }

        val endretVilkårsvurdering: List<RestPersonResultat> =
            vilkårService.endreVilkår(
                behandlingId = behandling.id,
                vilkårId = under18ÅrVilkårForBarn!!.id,
                restPersonResultat =
                RestPersonResultat(
                    personIdent = barnFnr,
                    vilkårResultater = listOf(
                        under18ÅrVilkårForBarn.copy(
                            resultat = Resultat.OPPFYLT,
                            periodeFom = LocalDate.of(2019, 5, 8)
                        )
                    )
                )
            )

        val endretUnder18ÅrVilkårForBarn =
            endretVilkårsvurdering.find { it.personIdent == barnFnr }
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
            lagTestPersonopplysningGrunnlag(
                behandling.id, fnr, listOf(barnFnr),
                søkerAktør = personidentService.hentOgLagreAktør(fnr),
                barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr))
            )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vilkårsvurdering = vilkårService.initierVilkårsvurderingForBehandling(
            behandling = behandling,
            bekreftEndringerViaFrontend = true,
            forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErIverksatt
        )
        Assertions.assertEquals(2, vilkårsvurdering.personResultater.size)

        val personopplysningGrunnlagMedEkstraBarn =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                fnr,
                listOf(barnFnr, barnFnr2),
                søkerAktør = personidentService.hentOgLagreAktør(fnr),
                barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr, barnFnr2))
            )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlagMedEkstraBarn)

        val behandlingResultatMedEkstraBarn = vilkårService.initierVilkårsvurderingForBehandling(
            behandling = behandling,
            bekreftEndringerViaFrontend = true,
            forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErIverksatt
        )
        Assertions.assertEquals(3, behandlingResultatMedEkstraBarn.personResultater.size)
    }

    @Test
    fun `vurder ugyldig vilkårsvurdering`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id, fnr, listOf(barnFnr),
                søkerAktør = personidentService.hentOgLagreAktør(fnr),
                barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr))
            )
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
            lagTestPersonopplysningGrunnlag(
                behandling.id, fnr, listOf(barnFnr),
                søkerAktør = personidentService.hentOgLagreAktør(fnr),
                barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr))
            )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vilkårsvurdering = vilkårService.initierVilkårsvurderingForBehandling(
            behandling = behandling,
            bekreftEndringerViaFrontend = true,
            forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErIverksatt
        )
            .also {
                it.personResultater
                    .forEach {
                        it.leggTilBlankAnnenVurdering(annenVurderingType = AnnenVurderingType.OPPLYSNINGSPLIKT)
                    }
            }

        val kopiertBehandlingResultat = vilkårsvurdering.kopier(inkluderAndreVurderinger = true)

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = kopiertBehandlingResultat)
        val behandlingsResultater = vilkårsvurderingService
            .hentBehandlingResultatForBehandling(behandlingId = behandling.id)

        Assertions.assertEquals(2, behandlingsResultater.size)
        Assertions.assertNotEquals(vilkårsvurdering.id, kopiertBehandlingResultat.id)
        Assertions.assertEquals(1, kopiertBehandlingResultat.personResultater.first().andreVurderinger.size)
        Assertions.assertEquals(
            AnnenVurderingType.OPPLYSNINGSPLIKT,
            kopiertBehandlingResultat.personResultater.first().andreVurderinger.first().type
        )
    }

    @Test
    fun `Vilkårsvurdering fra forrige behandling kopieres riktig`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(fnr))
        val forrigeBehandlingSomErIverksatt =
            behandlingService.hentSisteBehandlingSomErIverksatt(fagsakId = behandling.fagsak.id)

        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id, fnr, listOf(barnFnr),
                søkerAktør = personidentService.hentOgLagreAktør(fnr),
                barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr))
            )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vilkårsvurdering = vilkårService.initierVilkårsvurderingForBehandling(
            behandling = behandling,
            bekreftEndringerViaFrontend = true,
            forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErIverksatt
        )
        Assertions.assertEquals(2, vilkårsvurdering.personResultater.size)

        vilkårsvurdering.personResultater.map { personResultat ->
            personResultat.tilRestPersonResultat().vilkårResultater.map {
                vilkårService.endreVilkår(
                    behandlingId = behandling.id, vilkårId = it.id,
                    restPersonResultat =
                    RestPersonResultat(
                        personIdent = personResultat.aktør.aktivIdent(),
                        vilkårResultater = listOf(
                            it.copy(
                                resultat = Resultat.OPPFYLT,
                                periodeFom = LocalDate.of(2019, 5, 8)
                            )
                        )
                    )
                )
            }
        }

        behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BEHANDLING_AVSLUTTET))
        behandlingService.lagreEllerOppdater(behandling)

        val barnFnr2 = randomFnr()

        val behandling2 = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag2 =
            lagTestPersonopplysningGrunnlag(
                behandling2.id, fnr, listOf(barnFnr, barnFnr2),
                søkerAktør = personidentService.hentOgLagreAktør(fnr),
                barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr, barnFnr2))
            )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag2)

        val vilkårsvurdering2 = vilkårService.initierVilkårsvurderingForBehandling(
            behandling = behandling2,
            bekreftEndringerViaFrontend = true,
            forrigeBehandlingSomErVedtatt = behandling
        )

        Assertions.assertEquals(3, vilkårsvurdering2.personResultater.size)

        vilkårsvurdering2.personResultater.forEach { personResultat ->
            personResultat.vilkårResultater.forEach { vilkårResultat ->
                if (personResultat.aktør.aktivIdent() == barnFnr2) {
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
            lagTestPersonopplysningGrunnlag(
                behandling.id, fnr, listOf(barnFnr),
                søkerAktør = personidentService.hentOgLagreAktør(fnr),
                barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr))
            )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vilkårsvurdering = vilkårService.initierVilkårsvurderingForBehandling(
            behandling = behandling,
            bekreftEndringerViaFrontend = true,
            forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErIverksatt
        )

        val barn: Person = personopplysningGrunnlag.barna.find { it.aktør.aktivIdent() == barnFnr }!!
        vurderVilkårsvurderingTilInnvilget(vilkårsvurdering, barn)

        vilkårsvurderingService.oppdater(vilkårsvurdering)
        behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, StegType.BEHANDLING_AVSLUTTET))
        behandlingService.lagreEllerOppdater(behandling)

        val barnFnr2 = randomFnr()

        val behandling2 = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag2 =
            lagTestPersonopplysningGrunnlag(
                behandling2.id, fnr, listOf(barnFnr, barnFnr2),
                søkerAktør = personidentService.hentOgLagreAktør(fnr),
                barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr, barnFnr2))
            )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag2)

        val behandlingResultat2 = vilkårService.initierVilkårsvurderingForBehandling(
            behandling = behandling2,
            bekreftEndringerViaFrontend = true,
            forrigeBehandlingSomErVedtatt = behandling
        )

        Assertions.assertEquals(3, behandlingResultat2.personResultater.size)

        val personResultat = behandlingResultat2.personResultater.find { it.aktør.aktivIdent() == barnFnr }!!
        val borMedSøkerVilkår = personResultat.vilkårResultater.find { it.vilkårType == Vilkår.BOR_MED_SØKER }!!
        Assertions.assertEquals(behandling.id, borMedSøkerVilkår.behandlingId)

        VilkårsvurderingUtils.muterPersonVilkårResultaterPut(
            personResultat,
            RestVilkårResultat(
                borMedSøkerVilkår.id,
                Vilkår.BOR_MED_SØKER,
                Resultat.OPPFYLT,
                LocalDate.of(2010, 6, 2),
                LocalDate.of(2011, 9, 1),
                "",
                "",
                LocalDateTime.now(),
                behandling.id
            )
        )

        val behandlingResultatEtterEndring = vilkårsvurderingService.oppdater(behandlingResultat2)
        val personResultatEtterEndring =
            behandlingResultatEtterEndring.personResultater.find { it.aktør.aktivIdent() == barnFnr }!!
        val borMedSøkerVilkårEtterEndring =
            personResultatEtterEndring.vilkårResultater.find { it.vilkårType == Vilkår.BOR_MED_SØKER }!!
        Assertions.assertEquals(behandling2.id, borMedSøkerVilkårEtterEndring.behandlingId)
    }

    @Test
    fun `Valider gyldige vilkårspermutasjoner for barn og søker`() {
        Assertions.assertEquals(
            setOf(
                Vilkår.UNDER_18_ÅR,
                Vilkår.BOR_MED_SØKER,
                Vilkår.GIFT_PARTNERSKAP,
                Vilkår.BOSATT_I_RIKET,
                Vilkår.LOVLIG_OPPHOLD
            ),
            Vilkår.hentVilkårFor(PersonType.BARN)
        )

        Assertions.assertEquals(
            setOf(
                Vilkår.BOSATT_I_RIKET,
                Vilkår.LOVLIG_OPPHOLD
            ),
            Vilkår.hentVilkårFor(PersonType.SØKER)
        )
    }

    @Test
    fun `Valider gyldige vilkårspermutasjoner for barn og søker ved utvidet barnetrygd`() {
        Assertions.assertEquals(
            setOf(
                Vilkår.UNDER_18_ÅR,
                Vilkår.BOR_MED_SØKER,
                Vilkår.GIFT_PARTNERSKAP,
                Vilkår.BOSATT_I_RIKET,
                Vilkår.LOVLIG_OPPHOLD
            ),
            Vilkår.hentVilkårFor(personType = PersonType.BARN, ytelseType = YtelseType.UTVIDET_BARNETRYGD)
        )

        Assertions.assertEquals(
            setOf(
                Vilkår.BOSATT_I_RIKET,
                Vilkår.LOVLIG_OPPHOLD,
                Vilkår.UTVIDET_BARNETRYGD
            ),
            Vilkår.hentVilkårFor(personType = PersonType.SØKER, ytelseType = YtelseType.UTVIDET_BARNETRYGD)
        )
    }

    @Test
    fun `Skal ikke få duplikate utdypede vilkårsvurderinger`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(fnr))
        val forrigeBehandlingSomErIverksatt =
            behandlingService.hentSisteBehandlingSomErIverksatt(fagsakId = behandling.fagsak.id)

        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id, fnr, listOf(barnFnr),
                søkerAktør = personidentService.hentOgLagreAktør(fnr),
                barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr))
            )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vilkårsvurdering = vilkårService.initierVilkårsvurderingForBehandling(
            behandling = behandling,
            bekreftEndringerViaFrontend = true,
            forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErIverksatt
        )
        val under18ÅrVilkårForBarn =
            vilkårsvurdering.personResultater.find { it.aktør.aktivIdent() == barnFnr }
                ?.tilRestPersonResultat()?.vilkårResultater?.find { it.vilkårType == Vilkår.UNDER_18_ÅR }

        val endretVilkårsvurdering: List<RestPersonResultat> =
            vilkårService.endreVilkår(
                behandlingId = behandling.id,
                vilkårId = under18ÅrVilkårForBarn!!.id,
                restPersonResultat =
                RestPersonResultat(
                    personIdent = barnFnr,
                    vilkårResultater = listOf(
                        under18ÅrVilkårForBarn.copy(
                            resultat = Resultat.OPPFYLT,
                            periodeFom = LocalDate.of(2019, 5, 8),
                            erDeltBosted = true,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED)
                        )
                    )
                )
            )

        val endretUnder18ÅrVilkårForBarn =
            endretVilkårsvurdering.find { it.personIdent == barnFnr }
                ?.vilkårResultater?.find { it.vilkårType == Vilkår.UNDER_18_ÅR }

        Assertions.assertEquals(
            1,
            endretUnder18ÅrVilkårForBarn!!.utdypendeVilkårsvurderinger.size
        )
    }

    @Test
    fun `Skal legge til både erSkjønnsmessigVurdert og erMedlemskapVurdert i utdypendeVilkårsvurderinger liste`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(fnr))
        val forrigeBehandlingSomErIverksatt =
            behandlingService.hentSisteBehandlingSomErIverksatt(fagsakId = behandling.fagsak.id)

        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id, fnr, listOf(barnFnr),
                søkerAktør = personidentService.hentOgLagreAktør(fnr),
                barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr))
            )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vilkårsvurdering = vilkårService.initierVilkårsvurderingForBehandling(
            behandling = behandling,
            bekreftEndringerViaFrontend = true,
            forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErIverksatt
        )
        val under18ÅrVilkårForBarn =
            vilkårsvurdering.personResultater.find { it.aktør.aktivIdent() == barnFnr }
                ?.tilRestPersonResultat()?.vilkårResultater?.find { it.vilkårType == Vilkår.UNDER_18_ÅR }

        val endretVilkårsvurdering: List<RestPersonResultat> =
            vilkårService.endreVilkår(
                behandlingId = behandling.id,
                vilkårId = under18ÅrVilkårForBarn!!.id,
                restPersonResultat =
                RestPersonResultat(
                    personIdent = barnFnr,
                    vilkårResultater = listOf(
                        under18ÅrVilkårForBarn.copy(
                            resultat = Resultat.OPPFYLT,
                            periodeFom = LocalDate.of(2019, 5, 8),
                            erSkjønnsmessigVurdert = true,
                            erMedlemskapVurdert = true,
                        )
                    )
                )
            )

        val endretUnder18ÅrVilkårForBarn =
            endretVilkårsvurdering.find { it.personIdent == barnFnr }
                ?.vilkårResultater?.find { it.vilkårType == Vilkår.UNDER_18_ÅR }

        Assertions.assertEquals(
            2,
            endretUnder18ÅrVilkårForBarn!!.utdypendeVilkårsvurderinger.size
        )
    }

    @Test
    fun `skal lage vilkårsvurderingsperiode for migrering ved flyttesak`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()
        val søkerFødselsdato = LocalDate.of(1984, 1, 14)
        val barnetsFødselsdato = LocalDate.now().minusMonths(6)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val forrigeBehandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = BehandlingÅrsak.MIGRERING,
            )
        )
        val forrigePersonopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            forrigeBehandling.id,
            fnr, listOf(barnFnr),
            barnetsFødselsdato,
            personidentService.hentOgLagreAktør(fnr),
            personidentService.hentOgLagreAktørIder(listOf(barnFnr))
        )
        persongrunnlagService.lagreOgDeaktiverGammel(forrigePersonopplysningGrunnlag)

        var forrigeVilkårsvurdering = Vilkårsvurdering(behandling = forrigeBehandling)
        val søkerPersonResultat = PersonResultat(
            vilkårsvurdering = forrigeVilkårsvurdering,
            personIdent = fnr,
            aktør = personidentService.hentOgLagreAktør(fnr)
        )
        søkerPersonResultat.setSortedVilkårResultater(
            lagSøkerVilkårResultat(
                søkerPersonResultat = søkerPersonResultat,
                periodeFom = søkerFødselsdato,
                behandlingId = forrigeBehandling.id
            )
        )
        val barnPersonResultat = PersonResultat(
            vilkårsvurdering = forrigeVilkårsvurdering, personIdent = barnFnr,
            aktør = personidentService.hentOgLagreAktør(barnFnr)
        )
        barnPersonResultat.setSortedVilkårResultater(
            setOf(
                lagVilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.UNDER_18_ÅR,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnetsFødselsdato,
                    periodeTom = barnetsFødselsdato.plusYears(18).minusMonths(1),
                    behandlingId = forrigeBehandling.id
                ),
                lagVilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.GIFT_PARTNERSKAP,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnetsFødselsdato,
                    periodeTom = null,
                    behandlingId = forrigeBehandling.id
                ),
                lagVilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.now().minusMonths(1),
                    periodeTom = null,
                    behandlingId = forrigeBehandling.id
                ),
                lagVilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnetsFødselsdato,
                    periodeTom = null,
                    behandlingId = forrigeBehandling.id
                ),
                lagVilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.LOVLIG_OPPHOLD,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnetsFødselsdato,
                    periodeTom = null,
                    behandlingId = forrigeBehandling.id
                )
            )
        )
        forrigeVilkårsvurdering = forrigeVilkårsvurdering.apply {
            personResultater = setOf(
                søkerPersonResultat,
                barnPersonResultat
            )
        }
        vilkårsvurderingService.lagreNyOgDeaktiverGammel(forrigeVilkårsvurdering)

        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO,
            )
        )
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandling.id,
            fnr, listOf(barnFnr),
            barnetsFødselsdato,
            personidentService.hentOgLagreAktør(fnr),
            personidentService.hentOgLagreAktørIder(listOf(barnFnr))
        )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val nyMigreringsdato = LocalDate.now().minusMonths(5)
        val vilkårsvurdering = vilkårService.genererVilkårsvurderingForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
            behandling = behandling,
            forrigeBehandlingSomErVedtatt = forrigeBehandling,
            nyMigreringsdato = nyMigreringsdato
        )
        assertTrue { vilkårsvurdering.personResultater.isNotEmpty() }
        val søkerVilkårResultat =
            vilkårsvurdering.personResultater.first { it.aktør.aktivIdent() == fnr }.vilkårResultater
        assertTrue { søkerVilkårResultat.size == 2 }
        assertTrue {
            søkerVilkårResultat.all {
                it.periodeFom == søkerFødselsdato &&
                    it.periodeTom == null
            }
        }

        val barnVilkårResultat =
            vilkårsvurdering.personResultater.first { it.aktør.aktivIdent() == barnFnr }.vilkårResultater
        assertTrue { barnVilkårResultat.size == 5 }
        assertTrue {
            barnVilkårResultat.filter { it.vilkårType == Vilkår.BOR_MED_SØKER }.all {
                it.periodeFom == nyMigreringsdato &&
                    it.periodeTom == null
            }
        }
        assertTrue {
            barnVilkårResultat.filter { it.vilkårType == Vilkår.UNDER_18_ÅR }.all {
                it.periodeFom == barnetsFødselsdato &&
                    it.periodeTom == barnetsFødselsdato.plusYears(18).minusDays(1)
            }
        }
        assertTrue {
            barnVilkårResultat.filter { it.vilkårType !in listOf(Vilkår.BOR_MED_SØKER, Vilkår.UNDER_18_ÅR) }.all {
                it.periodeFom == barnetsFødselsdato &&
                    it.periodeTom == null
            }
        }
    }

    @Test
    fun `skal lage vilkårsvurderingsperiode for vanlig migrering tilbake i tid`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()
        val forrigeVilkårsdato = LocalDate.of(2021, 8, 1)
        val barnetsFødselsdato = LocalDate.now().minusYears(1)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val forrigeBehandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = BehandlingÅrsak.MIGRERING,
            )
        )
        val forrigePersonopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            forrigeBehandling.id,
            fnr, listOf(barnFnr),
            barnetsFødselsdato,
            personidentService.hentOgLagreAktør(fnr),
            personidentService.hentOgLagreAktørIder(listOf(barnFnr))
        )
        persongrunnlagService.lagreOgDeaktiverGammel(forrigePersonopplysningGrunnlag)

        var forrigeVilkårsvurdering = Vilkårsvurdering(behandling = forrigeBehandling)
        val søkerPersonResultat = PersonResultat(
            vilkårsvurdering = forrigeVilkårsvurdering,
            personIdent = fnr,
            aktør = personidentService.hentOgLagreAktør(fnr)
        )
        søkerPersonResultat.setSortedVilkårResultater(
            lagSøkerVilkårResultat(
                søkerPersonResultat = søkerPersonResultat,
                periodeFom = forrigeVilkårsdato,
                behandlingId = forrigeBehandling.id
            )
        )
        val barnPersonResultat = PersonResultat(
            vilkårsvurdering = forrigeVilkårsvurdering,
            personIdent = barnFnr,
            aktør = personidentService.hentOgLagreAktør(barnFnr)
        )
        barnPersonResultat.setSortedVilkårResultater(
            setOf(
                lagVilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.UNDER_18_ÅR,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnetsFødselsdato,
                    periodeTom = barnetsFødselsdato.plusYears(18).minusMonths(1),
                    behandlingId = forrigeBehandling.id
                ),
                lagVilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.GIFT_PARTNERSKAP,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnetsFødselsdato,
                    periodeTom = null,
                    behandlingId = forrigeBehandling.id
                ),
                lagVilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = forrigeVilkårsdato,
                    periodeTom = null,
                    behandlingId = forrigeBehandling.id
                ),
                lagVilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = forrigeVilkårsdato,
                    periodeTom = null,
                    behandlingId = forrigeBehandling.id
                ),
                lagVilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.LOVLIG_OPPHOLD,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = forrigeVilkårsdato,
                    periodeTom = null,
                    behandlingId = forrigeBehandling.id
                )
            )
        )
        forrigeVilkårsvurdering = forrigeVilkårsvurdering.apply {
            personResultater = setOf(
                søkerPersonResultat,
                barnPersonResultat
            )
        }
        vilkårsvurderingService.lagreNyOgDeaktiverGammel(forrigeVilkårsvurdering)

        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO,
            )
        )
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandling.id,
            fnr, listOf(barnFnr),
            barnetsFødselsdato,
            personidentService.hentOgLagreAktør(fnr),
            personidentService.hentOgLagreAktørIder(listOf(barnFnr))
        )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val nyMigreringsdato = LocalDate.of(2021, 1, 1)
        val vilkårsvurdering = vilkårService.genererVilkårsvurderingForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
            behandling = behandling,
            forrigeBehandlingSomErVedtatt = forrigeBehandling,
            nyMigreringsdato = nyMigreringsdato
        )
        assertTrue { vilkårsvurdering.personResultater.isNotEmpty() }
        val søkerVilkårResultat =
            vilkårsvurdering.personResultater.first { it.aktør.aktivIdent() == fnr }.vilkårResultater
        assertTrue { søkerVilkårResultat.size == 2 }
        assertTrue {
            søkerVilkårResultat.all {
                it.periodeFom == nyMigreringsdato &&
                    it.periodeTom == null
            }
        }

        val barnVilkårResultat =
            vilkårsvurdering.personResultater.first { it.aktør.aktivIdent() == barnFnr }.vilkårResultater
        assertTrue { barnVilkårResultat.size == 5 }
        assertTrue {
            barnVilkårResultat.filter { it.vilkårType.påvirketVilkårForEndreMigreringsdato() }.all {
                it.periodeFom == nyMigreringsdato &&
                    it.periodeTom == null
            }
        }
        assertTrue {
            barnVilkårResultat.filter { it.vilkårType == Vilkår.UNDER_18_ÅR }.all {
                it.periodeFom == barnetsFødselsdato &&
                    it.periodeTom == barnetsFødselsdato.plusYears(18).minusDays(1)
            }
        }
        assertTrue {
            barnVilkårResultat.filter { it.vilkårType == Vilkår.GIFT_PARTNERSKAP }.all {
                it.periodeFom == barnetsFødselsdato &&
                    it.periodeTom == null
            }
        }
    }

    @Test
    fun `skal lage vilkårsvurderingsperiode for migrering ved flere perioder`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()
        val søkerFødselsdato = LocalDate.of(1984, 8, 1)
        val barnetsFødselsdato = LocalDate.now().minusYears(10)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val forrigeBehandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = BehandlingÅrsak.MIGRERING,
            )
        )
        val forrigePersonopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            forrigeBehandling.id,
            fnr, listOf(barnFnr),
            barnetsFødselsdato,
            personidentService.hentOgLagreAktør(fnr),
            personidentService.hentOgLagreAktørIder(listOf(barnFnr))
        )
        persongrunnlagService.lagreOgDeaktiverGammel(forrigePersonopplysningGrunnlag)

        var forrigeVilkårsvurdering = Vilkårsvurdering(behandling = forrigeBehandling)
        val søkerPersonResultat = PersonResultat(
            vilkårsvurdering = forrigeVilkårsvurdering, personIdent = fnr,
            aktør = personidentService.hentOgLagreAktør(fnr)
        )
        søkerPersonResultat.setSortedVilkårResultater(
            lagSøkerVilkårResultat(
                søkerPersonResultat = søkerPersonResultat,
                periodeFom = søkerFødselsdato,
                behandlingId = forrigeBehandling.id
            )
        )
        val barnPersonResultat = PersonResultat(
            vilkårsvurdering = forrigeVilkårsvurdering,
            personIdent = barnFnr,
            aktør = personidentService.hentOgLagreAktør(barnFnr)
        )
        barnPersonResultat.setSortedVilkårResultater(
            setOf(
                lagVilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.UNDER_18_ÅR,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnetsFødselsdato,
                    periodeTom = barnetsFødselsdato.plusYears(18).minusMonths(1),
                    behandlingId = forrigeBehandling.id
                ),
                lagVilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.GIFT_PARTNERSKAP,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnetsFødselsdato,
                    periodeTom = null,
                    behandlingId = forrigeBehandling.id
                ),
                lagVilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2021, 4, 14),
                    periodeTom = LocalDate.of(2021, 8, 16),
                    behandlingId = forrigeBehandling.id
                ),
                lagVilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2021, 10, 5),
                    periodeTom = null,
                    behandlingId = forrigeBehandling.id
                ),
                lagVilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnetsFødselsdato,
                    periodeTom = null,
                    behandlingId = forrigeBehandling.id
                ),
                lagVilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.LOVLIG_OPPHOLD,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnetsFødselsdato,
                    periodeTom = null,
                    behandlingId = forrigeBehandling.id
                )
            )
        )
        forrigeVilkårsvurdering = forrigeVilkårsvurdering.apply {
            personResultater = setOf(
                søkerPersonResultat,
                barnPersonResultat
            )
        }
        vilkårsvurderingService.lagreNyOgDeaktiverGammel(forrigeVilkårsvurdering)

        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO,
            )
        )
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandling.id,
            fnr, listOf(barnFnr),
            barnetsFødselsdato,
            personidentService.hentOgLagreAktør(fnr),
            personidentService.hentOgLagreAktørIder(listOf(barnFnr))
        )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val nyMigreringsdato = LocalDate.of(2021, 1, 1)
        val vilkårsvurdering = vilkårService.genererVilkårsvurderingForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
            behandling = behandling,
            forrigeBehandlingSomErVedtatt = forrigeBehandling,
            nyMigreringsdato = nyMigreringsdato
        )
        assertTrue { vilkårsvurdering.personResultater.isNotEmpty() }
        val søkerVilkårResultat =
            vilkårsvurdering.personResultater.first { it.aktør.aktivIdent() == fnr }.vilkårResultater
        assertTrue { søkerVilkårResultat.size == 2 }
        assertTrue {
            søkerVilkårResultat.all {
                it.periodeFom == søkerFødselsdato &&
                    it.periodeTom == null
            }
        }

        val barnVilkårResultat =
            vilkårsvurdering.personResultater.first { it.aktør.aktivIdent() == barnFnr }.vilkårResultater
        assertTrue { barnVilkårResultat.size == 6 }
        assertTrue {
            barnVilkårResultat.filter { it.vilkårType == Vilkår.BOR_MED_SØKER }.any {
                it.periodeFom == nyMigreringsdato &&
                    it.periodeTom == LocalDate.of(2021, 8, 16)
            }
        }
        assertTrue {
            barnVilkårResultat.filter { it.vilkårType == Vilkår.BOR_MED_SØKER }.any {
                it.periodeFom == LocalDate.of(2021, 10, 5) &&
                    it.periodeTom == null
            }
        }
        assertTrue {
            barnVilkårResultat.filter { it.vilkårType == Vilkår.UNDER_18_ÅR }.all {
                it.periodeFom == barnetsFødselsdato &&
                    it.periodeTom == barnetsFødselsdato.plusYears(18).minusDays(1)
            }
        }
        assertTrue {
            barnVilkårResultat.filter { it.vilkårType !in listOf(Vilkår.BOR_MED_SØKER, Vilkår.UNDER_18_ÅR) }.all {
                it.periodeFom == barnetsFødselsdato &&
                    it.periodeTom == null
            }
        }
    }

    private fun lagSøkerVilkårResultat(
        søkerPersonResultat: PersonResultat,
        periodeFom: LocalDate,
        periodeTom: LocalDate? = null,
        behandlingId: Long
    ): Set<VilkårResultat> {
        return setOf(
            lagVilkårResultat(
                personResultat = søkerPersonResultat,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.OPPFYLT,
                periodeFom = periodeFom,
                periodeTom = periodeTom,
                behandlingId = behandlingId
            ),
            lagVilkårResultat(
                personResultat = søkerPersonResultat,
                vilkårType = Vilkår.LOVLIG_OPPHOLD,
                resultat = Resultat.OPPFYLT,
                periodeFom = periodeFom,
                periodeTom = periodeTom,
                behandlingId = behandlingId
            )
        )
    }

    fun Vilkår.påvirketVilkårForEndreMigreringsdato() = this in listOf(
        Vilkår.BOSATT_I_RIKET,
        Vilkår.LOVLIG_OPPHOLD,
        Vilkår.BOR_MED_SØKER
    )
}
