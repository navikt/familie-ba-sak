package no.nav.familie.ba.sak.behandling.vedtak

import io.mockk.MockKAnnotations
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakPersonRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.restDomene.RestPutUtbetalingBegrunnelse
import no.nav.familie.ba.sak.behandling.vilkår.*
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.nare.core.evaluations.Resultat
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate

@SpringBootTest
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("mock-pdl", "postgres")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VedtakBegrunnelseTest(@Autowired
                            val behandlingRepository: BehandlingRepository,

                            @Autowired
                            val behandlingResultatService: BehandlingResultatService,

                            @Autowired
                            val vedtakService: VedtakService,

                            @Autowired
                            val persongrunnlagService: PersongrunnlagService,

                            @Autowired
                            val beregningService: BeregningService,

                            @Autowired
                            val fagsakService: FagsakService,

                            @Autowired
                            val fagsakPersonRepository: FagsakPersonRepository,

                            @Autowired
                            val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,

                            @Autowired
                            val loggService: LoggService) {

    lateinit var behandlingService: BehandlingService

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        behandlingService = BehandlingService(
                behandlingRepository,
                fagsakPersonRepository,
                persongrunnlagService,
                beregningService,
                fagsakService,
                loggService)
    }

    @Test
    fun `endring av begrunnelse skal koble seg til korrekt vilkår`() {
        val søkerFnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr, listOf(barn1Fnr, barn2Fnr))
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val behandlingResultat = BehandlingResultat(
                behandling = behandling
        )

        val søkerPersonResultat = PersonResultat(behandlingResultat = behandlingResultat, personIdent = søkerFnr)
        søkerPersonResultat.setVilkårResultater(setOf(
                VilkårResultat(
                        personResultat = søkerPersonResultat,
                        vilkårType = Vilkår.LOVLIG_OPPHOLD,
                        resultat = Resultat.JA,
                        periodeFom = LocalDate.of(2009, 12, 24),
                        periodeTom = LocalDate.of(2010, 6, 1),
                        begrunnelse = "",
                        behandlingId = behandlingResultat.behandling.id,
                        regelInput = null,
                        regelOutput = null),
                VilkårResultat(
                        personResultat = søkerPersonResultat,
                        vilkårType = Vilkår.BOSATT_I_RIKET,
                        resultat = Resultat.JA,
                        periodeFom = LocalDate.of(2009, 12, 24),
                        periodeTom = LocalDate.of(2010, 6, 1),
                        begrunnelse = "",
                        behandlingId = behandlingResultat.behandling.id,
                        regelInput = null,
                        regelOutput = null)))

        val barn1PersonResultat = PersonResultat(behandlingResultat = behandlingResultat, personIdent = barn1Fnr)

        barn1PersonResultat.setVilkårResultater(setOf(
                VilkårResultat(personResultat = barn1PersonResultat,
                               vilkårType = Vilkår.LOVLIG_OPPHOLD,
                               resultat = Resultat.JA,
                               periodeFom = LocalDate.of(2009, 12, 24),
                               periodeTom = LocalDate.of(2010, 6, 1),
                               begrunnelse = "",
                               behandlingId = behandlingResultat.behandling.id,
                               regelInput = null,
                               regelOutput = null),
                VilkårResultat(personResultat = barn1PersonResultat,
                               vilkårType = Vilkår.GIFT_PARTNERSKAP,
                               resultat = Resultat.JA,
                               periodeFom = LocalDate.of(2009, 11, 24),
                               periodeTom = LocalDate.of(2010, 6, 1),
                               begrunnelse = "",
                               behandlingId = behandlingResultat.behandling.id,
                               regelInput = null,
                               regelOutput = null)))

        val barn2PersonResultat = PersonResultat(behandlingResultat = behandlingResultat, personIdent = barn1Fnr)

        barn2PersonResultat.setVilkårResultater(setOf(
                VilkårResultat(personResultat = barn1PersonResultat,
                               vilkårType = Vilkår.LOVLIG_OPPHOLD,
                               resultat = Resultat.JA,
                               periodeFom = LocalDate.of(2010, 2, 24),
                               periodeTom = LocalDate.of(2010, 6, 1),
                               begrunnelse = "",
                               behandlingId = behandlingResultat.behandling.id,
                               regelInput = null,
                               regelOutput = null),
                VilkårResultat(personResultat = barn1PersonResultat,
                               vilkårType = Vilkår.GIFT_PARTNERSKAP,
                               resultat = Resultat.JA,
                               periodeFom = LocalDate.of(2009, 11, 24),
                               periodeTom = LocalDate.of(2010, 6, 1),
                               begrunnelse = "",
                               behandlingId = behandlingResultat.behandling.id,
                               regelInput = null,
                               regelOutput = null)))

        behandlingResultat.personResultater = setOf(søkerPersonResultat, barn1PersonResultat, barn2PersonResultat)

        behandlingResultatService.lagreNyOgDeaktiverGammel(behandlingResultat, false)

        vedtakService.lagreOgDeaktiverGammel(lagVedtak(behandling))

        val initertRestUtbetalingBegrunnelseLovligOpphold =
                vedtakService.leggTilUtbetalingBegrunnelse(periode = Periode(fom = LocalDate.of(2010, 1, 1),
                                                                             tom = LocalDate.of(2010, 6, 1)),
                                                           fagsakId = fagsak.id)

        val begrunnelserLovligOpphold =
                vedtakService.endreUtbetalingBegrunnelse(
                        RestPutUtbetalingBegrunnelse(resultat = BehandlingResultatType.INNVILGET,
                                                     vedtakBegrunnelse = VedtakBegrunnelse.INNVILGET_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE),
                        fagsakId = fagsak.id,
                        utbetalingBegrunnelseId = initertRestUtbetalingBegrunnelseLovligOpphold[0].id!!)

        assert(begrunnelserLovligOpphold.size == 1)
        Assertions.assertEquals(
                "Du får barnetrygd fordi du og barn født 01.01.19 har oppholdstillatelse fra desember 2009.",
                begrunnelserLovligOpphold.firstOrNull { it.vedtakBegrunnelse == VedtakBegrunnelse.INNVILGET_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE }!!.brevBegrunnelse)

        val initertRestUtbetalingBegrunnelseBosattIRiket =
                vedtakService.leggTilUtbetalingBegrunnelse(periode = Periode(fom = LocalDate.of(2010, 1, 1),
                                                                             tom = LocalDate.of(2010, 6, 1)),
                                                           fagsakId = fagsak.id)

        val begrunnelserLovligOppholdOgBosattIRiket =
                vedtakService.endreUtbetalingBegrunnelse(
                        RestPutUtbetalingBegrunnelse(resultat = BehandlingResultatType.INNVILGET,
                                                     vedtakBegrunnelse = VedtakBegrunnelse.INNVILGET_BOSATT_I_RIKTET),
                        fagsakId = fagsak.id,
                        utbetalingBegrunnelseId = initertRestUtbetalingBegrunnelseBosattIRiket[1].id!!)

        assert(begrunnelserLovligOppholdOgBosattIRiket.size == 2)
        Assertions.assertEquals(
                "Du får barnetrygd fordi du er bosatt i Norge fra 24.12.09.",
                begrunnelserLovligOppholdOgBosattIRiket.firstOrNull { it.vedtakBegrunnelse == VedtakBegrunnelse.INNVILGET_BOSATT_I_RIKTET }!!.brevBegrunnelse)

        assertThrows<Feil> {
            vedtakService.endreUtbetalingBegrunnelse(
                    RestPutUtbetalingBegrunnelse(resultat = BehandlingResultatType.INNVILGET,
                                                 vedtakBegrunnelse = VedtakBegrunnelse.INNVILGET_BOR_HOS_SØKER),
                    fagsakId = fagsak.id,
                    utbetalingBegrunnelseId = initertRestUtbetalingBegrunnelseBosattIRiket[1].id!!)
        }
    }
}