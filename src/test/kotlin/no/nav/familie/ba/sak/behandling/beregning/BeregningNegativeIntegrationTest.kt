package no.nav.familie.ba.sak.behandling.beregning

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vedtak.Ytelsetype
import no.nav.familie.ba.sak.behandling.vilkår.vilkårsvurderingKomplettForBarnOgSøker
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonTjeneste
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.util.DbContainerInitializer
import no.nav.familie.ba.sak.util.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.util.randomFnr
import no.nav.familie.kontrakter.felles.Ressurs
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate


@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres", "mock-dokgen-negative")
@Tag("integration")
class BeregningNegativeIntegrationTest {

    @Autowired
    private lateinit var beregningController: BeregningController

    @MockBean
    lateinit var integrasjonTjeneste: IntegrasjonTjeneste

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var fagsakService: FagsakService

    @Autowired
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Autowired
    private lateinit var vedtakService: VedtakService

    @BeforeEach
    fun setup() {
        Mockito.`when`(integrasjonTjeneste.hentAktørId(anyString())).thenReturn(AktørId("1"))
    }

    @Test
    @Tag("integration")
    fun `Oppdater avslag vedtak med beregning`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.opprettNyBehandlingPåFagsak(fagsak,
                                                                       null,
                                                                       BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                       BehandlingKategori.NASJONAL,
                                                                       BehandlingUnderkategori.ORDINÆR)
        behandlingService.settVilkårsvurdering(behandling, BehandlingResultat.AVSLÅTT, "")
        Assertions.assertNotNull(behandling.fagsak.id)

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, fnr, barnFnr)
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag,
                restSamletVilkårResultat = vilkårsvurderingKomplettForBarnOgSøker(
                        fnr,
                        listOf(barnFnr)),
                ansvarligSaksbehandler = "ansvarligSaksbehandler"
        )
        val vedtak = vedtakService.hentAktivForBehandling(behandling.id)
        Assertions.assertNotNull(vedtak)

        val fagsakRes = beregningController.oppdaterVedtakMedBeregning(fagsak.id,
                                                                       NyBeregning(
                                                                               listOf(
                                                                                       BarnBeregning(
                                                                                               ident = barnFnr,
                                                                                               beløp = 1054,
                                                                                               stønadFom = LocalDate.of(2020,
                                                                                                                        1,
                                                                                                                        1),
                                                                                               ytelsetype = Ytelsetype.ORDINÆR_BARNETRYGD
                                                                                       ))
                                                                       ))

        Assertions.assertEquals(Ressurs.Status.FEILET, fagsakRes.body?.status)
        Assertions.assertEquals("Kan ikke lage beregning på et vedtak som ikke er innvilget", fagsakRes.body?.melding)
    }
}
